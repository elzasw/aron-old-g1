package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.elza.ImportFundElza;
import cz.aron.transfagent.elza.synchronization.PublicationReport;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.service.StorageService;
import cz.tacr.elza.client.vo.PublicationReportStatus;

/**
 * Creating the fund an Elza publication names from the publication itself.
 * <p>
 * The fixture export identifies its fund by {@code fi/@c} = {@value #FUND_CODE} and carries no NAD,
 * so the staged fund has to appear under that very code - the one the next import attempt looks it
 * up by.
 * <p>
 * {@link ConfigElzaArchDesc} is the context-wide bean the importer holds, so the option is restored
 * after every test rather than pinned with a property - a second property combination would fork
 * another Spring context.
 */
@SpringBootTest
public class ImportMissingFundTest extends AbstractCommonTest {

    private static final String FUND_CODE = "CR2303";

    /** Root level of the fixture export and the one a second publication replaces it with. */
    private static final String ORIGINAL_ROOT_UUID = "752288f9-6c89-4a04-aa3c-906efc294250";

    private static final String REPLACED_ROOT_UUID = "0f4c1d2e-3a4b-4c5d-8e6f-70a1b2c3d4e5";

    @Autowired
    ImportFundElza importFundElza;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StorageService storageService;

    @Autowired
    ConfigElzaArchDesc configArchDesc;

    @BeforeEach
    void clearStagedFund() throws IOException {
        FileUtils.deleteDirectory(fundInputDir().toFile());
    }

    @AfterEach
    void restoreConfig() {
        configArchDesc.setImportMissingFund(true);
    }

    /**
     * The whole round trip: the description defers, the fund is created from its own export, and the
     * description imports on the next attempt.
     */
    @Test
    public void testFundIsCreatedFromTheArchDesc() throws IOException {
        importInstitution();
        var dir = stageArchDesc("archdesc-creates-fund", 999_050);

        assertTrue(importArchDescService.processDirectory(dir));

        // nothing imported yet, the description waits where it is
        assertTrue(archDescRepository.findAll().isEmpty());
        assertTrue(Files.isDirectory(dir));
        assertTrue(Files.isRegularFile(dir.resolve("fund-staged")));
        assertNoReport(999_050, "a postponed import is not an outcome to report");

        var fundDir = fundInputDir();
        assertTrue(Files.isRegularFile(fundDir.resolve("fund-" + FUND_CODE + ".xml")));
        assertFalse(Files.exists(fundDir.resolve("fund-" + FUND_CODE + ".xml.tmp")));

        // ImportFundService has the higher priority, so it runs before the description is retried
        assertTrue(importFundService.processDirectory(fundDir));
        var fund = fundRepository.findByCode(FUND_CODE);
        assertNotNull(fund, "the fund must exist under the code the archdesc resolves");

        assertTrue(importArchDescService.processDirectory(dir));

        assertEquals(1, archDescRepository.findAll().size());
        assertEquals(1, fundRepository.findAll().size());
        assertFalse(Files.exists(dir), "the imported description moves to the data directory");
        assertNoReport(999_050, "success is reported only after the upload to the core");
    }

    /**
     * Staging happens once. If the staged fund does not make it either, the description is rejected
     * rather than postponed for ever.
     */
    @Test
    public void testSecondAttemptIsRejected() throws IOException {
        importInstitution();
        var publicationId = 999_051;
        var dir = stageArchDesc("archdesc-fund-staged-twice", publicationId);

        assertTrue(importArchDescService.processDirectory(dir));
        assertTrue(Files.isRegularFile(dir.resolve("fund-staged")));

        // the staged fund never became a fund - drop it and let the description try again
        FileUtils.deleteDirectory(fundInputDir().toFile());

        assertTrue(importArchDescService.processDirectory(dir));

        assertFalse(Files.exists(dir));
        assertFalse(Files.exists(fundInputDir()), "the fund must not be staged a second time");
        var report = readReport(publicationId);
        assertEquals(PublicationReportStatus.ERROR, report.getStatus());
        assertTrue(report.getErrorMessage().contains("Missing fund"), report.getErrorMessage());
    }

    /**
     * With the option off the description is rejected straight away, as it was before.
     */
    @Test
    public void testDisabledOptionRejectsImmediately() throws IOException {
        importInstitution();
        configArchDesc.setImportMissingFund(false);
        var publicationId = 999_052;
        var dir = stageArchDesc("archdesc-fund-import-off", publicationId);

        assertTrue(importArchDescService.processDirectory(dir));

        assertFalse(Files.exists(dir));
        assertFalse(Files.exists(fundInputDir()), "no fund may be staged when the option is off");
        assertEquals(PublicationReportStatus.ERROR, readReport(publicationId).getStatus());
    }

    /**
     * A later publication of the same fund - here with another root level and another name - must
     * reach the fund too: its package is regenerated from the stored export, so the export is
     * refreshed and the reimport requested. Without that the fund would keep referencing a root the
     * portal has just replaced.
     */
    @Test
    public void testNewerPublicationRefreshesTheFund() throws IOException {
        importInstitution();
        var first = stageArchDesc("archdesc-fund-refresh-1", 999_053);
        assertTrue(importArchDescService.processDirectory(first));
        assertTrue(importFundService.processDirectory(fundInputDir()));
        assertTrue(importArchDescService.processDirectory(first));

        var fund = fundRepository.findByCode(FUND_CODE);
        var fundSource = apuSourceRepository.findById(fund.getApuSource().getId()).orElseThrow();
        assertEquals("Test datace", fund.getName());

        var second = stageArchDesc("archdesc-fund-refresh-2", 999_054);
        var xml = second.resolve(ARCH_DESC_DIR + ".xml");
        var content = Files.readString(xml)
                .replace(ORIGINAL_ROOT_UUID, REPLACED_ROOT_UUID)
                .replace("n=\"Test datace\"", "n=\"Test datace v2\"");
        Files.writeString(xml, content);

        assertTrue(importArchDescService.processDirectory(second));

        fund = fundRepository.findByCode(FUND_CODE);
        assertEquals("Test datace v2", fund.getName());
        fundSource = apuSourceRepository.findById(fundSource.getId()).orElseThrow();
        assertTrue(fundSource.isReimport(), "the fund package has to be regenerated");
        var fundDataDir = storageService.getApuDataDir(fundSource.getDataDir());
        var fundExport = Files.readString(fundDataDir.resolve("fund-" + FUND_CODE + ".xml"));
        assertTrue(fundExport.contains(REPLACED_ROOT_UUID), "the stored fund export follows the publication");
        assertFalse(Files.exists(fundDataDir.resolve("fund-" + FUND_CODE + ".xml.tmp")));

        // what the reimport then produces is the point: the fund references the new root
        assertEquals(ReimportProcessor.Result.REIMPORTED,
                importFundElza.reimport(fundSource, fund, Path.of(fundSource.getDataDir())));
        var apusrc = Files.readString(fundDataDir.resolve("apusrc.xml"));
        assertTrue(apusrc.contains(">" + REPLACED_ROOT_UUID + "<"), apusrc);
        assertFalse(apusrc.contains(ORIGINAL_ROOT_UUID), "no reference to the replaced root may survive");
        assertTrue(apusrc.contains("Test datace v2"));
    }

    private void importInstitution() throws IOException {
        FileUtils.copyDirectory(new File(DIR_FROM_INSTITUTION), new File(DIR_TO_INSTITUTION));
        importInstitutionService.processDirectory(Path.of(DIR_TO_INSTITUTION, INSTITUTION_DIR));
    }

    private Path stageArchDesc(String dirName, int publicationId) throws IOException {
        var dir = Path.of(DIR_TO_ARCH_DESC, dirName);
        // copyDirectory merges, so a directory a previous run left behind would keep its markers
        FileUtils.deleteDirectory(dir.toFile());
        FileUtils.copyDirectory(new File(DIR_FROM_ARCH_DESC, ARCH_DESC_DIR), dir.toFile());
        try (var os = Files.newOutputStream(dir.resolve("notify.json"))) {
            objectMapper.writeValue(os, Collections.singletonMap("publicationId", publicationId));
        }
        return dir;
    }

    private Path fundInputDir() {
        return storageService.getInputPath().resolve("fund").resolve("fund-" + FUND_CODE);
    }

    private Path commandsDir() {
        return storageService.getInputPath().resolve("commands");
    }

    private PublicationReport readReport(int publicationId) throws IOException {
        var reportFile = commandsDir().resolve("publication_report-" + publicationId);
        assertTrue(Files.isRegularFile(reportFile), "no report enqueued for publication " + publicationId);
        try (var is = Files.newInputStream(reportFile)) {
            return objectMapper.readValue(is, PublicationReport.class);
        }
    }

    private void assertNoReport(int publicationId, String message) {
        assertFalse(Files.exists(commandsDir().resolve("publication_report-" + publicationId)), message);
    }

}
