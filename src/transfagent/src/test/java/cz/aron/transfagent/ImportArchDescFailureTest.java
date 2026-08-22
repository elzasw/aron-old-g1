package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.transfagent.elza.synchronization.LegacyNotifyPublicationCommand;
import cz.aron.transfagent.elza.synchronization.PublicationOutcomeNotify;
import cz.aron.transfagent.elza.synchronization.PublicationReport;
import cz.aron.transfagent.service.StorageService;
import cz.tacr.elza.client.vo.PublicationReportStatus;

/**
 * What happens to an archdesc that cannot be imported at all, and how the outcome gets back to the
 * system that published it.
 * <p>
 * Each scenario stages the archdesc under its own directory name: the importer takes the fund code
 * from the file name, not from the directory, so the parked directories stay tellable apart in the
 * error directory the whole class shares.
 */
@SpringBootTest
public class ImportArchDescFailureTest extends AbstractCommonTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PublicationOutcomeNotify publicationOutcomeNotify;

    @Autowired
    LegacyNotifyPublicationCommand legacyNotifyPublicationCommand;

    @Autowired
    StorageService storageService;

    /**
     * A published archdesc naming an institution the deployment does not have is parked, not retried,
     * and ELZA is told why.
     * <p>
     * The institution stands in for every unrecoverable reason here: a missing <em>fund</em> is no
     * longer one of them, the portal creates it from the export instead - see
     * {@link ImportMissingFundTest}.
     */
    @Test
    public void testMissingInstitutionIsReportedAndParked() throws IOException {
        var publicationId = 999_043;
        var dir = stageArchDesc("archdesc-missing-institution", publicationId);

        assertTrue(importArchDescService.processDirectory(dir));

        assertTrue(archDescRepository.findAll().isEmpty());
        assertFalse(Files.exists(dir), "the directory must leave the input directory for good");

        var parked = parkedDir("archdesc-missing-institution");
        assertTrue(Files.readString(parked.resolve("protokol.txt"), StandardCharsets.UTF_8)
                .contains("Missing institution"));

        var report = readReport(publicationId);
        assertEquals(PublicationReportStatus.ERROR, report.getStatus());
        assertTrue(report.getErrorMessage().contains("Missing institution"), report.getErrorMessage());
        assertNotNull(report.getOccurredAt());
    }

    /**
     * An archdesc staged by hand carries no context, so there is nobody to report to - and the import
     * must not fail over that.
     */
    @Test
    public void testFailureWithoutContextReportsToNobody() throws IOException {
        var reportsBefore = pendingReports();
        var dir = stageArchDesc("archdesc-no-context", null);

        assertTrue(importArchDescService.processDirectory(dir));

        assertFalse(Files.exists(dir));
        assertEquals(reportsBefore, pendingReports(), "data staged by hand belongs to no publication");
    }

    /**
     * Success travels the same queue as failure.
     */
    @Test
    public void testSuccessIsReportedThroughTheSameQueue() throws IOException {
        var publicationId = 999_044;

        publicationOutcomeNotify.sentToAronCore(Collections.singletonMap("publicationId", publicationId));

        var report = readReport(publicationId);
        assertEquals(PublicationReportStatus.OK, report.getStatus());
        assertNull(report.getErrorMessage());
    }

    /**
     * A marker left in the commands directory by the previous version still reaches ELZA.
     */
    @Test
    public void testLegacyNotifyMarkerIsConverted() throws IOException {
        var publicationId = 999_045;
        Files.createDirectories(commandsDir());
        var marker = commandsDir().resolve("publication_notify-" + publicationId);
        Files.createFile(marker);

        legacyNotifyPublicationCommand.processCommand(marker, null);

        assertFalse(Files.exists(marker));
        assertEquals(PublicationReportStatus.OK, readReport(publicationId).getStatus());
    }

    /**
     * Copy the archdesc fixture into the input directory under its own name, optionally with the
     * {@code notify.json} a publication pulled from ELZA is staged with.
     *
     * @param dirName name of the staged directory
     * @param publicationId publication the data belongs to, {@code null} for data staged by hand
     * @return the staged directory
     */
    private Path stageArchDesc(String dirName, Integer publicationId) throws IOException {
        var dir = Path.of(DIR_TO_ARCH_DESC, dirName);
        // copyDirectory merges, so a directory a previous run left behind would keep its markers
        FileUtils.deleteDirectory(dir.toFile());
        FileUtils.copyDirectory(new File(DIR_FROM_ARCH_DESC, ARCH_DESC_DIR), dir.toFile());
        if (publicationId != null) {
            try (var os = Files.newOutputStream(dir.resolve("notify.json"))) {
                objectMapper.writeValue(os, Collections.singletonMap("publicationId", publicationId));
            }
        }
        return dir;
    }

    /**
     * @return the directory the staged data was parked in, whatever suffix the error directory gave it
     */
    private Path parkedDir(String dirName) throws IOException {
        var errorDateDir = Path.of(DIR_ERROR, getDateDir());
        assertTrue(Files.isDirectory(errorDateDir), "nothing was parked in " + errorDateDir);
        try (var stream = Files.list(errorDateDir)) {
            var parked = stream.filter(p -> p.getFileName().toString().startsWith(dirName))
                    .collect(Collectors.toList());
            assertEquals(1, parked.size(), "expected exactly one parked " + dirName + ", got " + parked);
            return parked.get(0);
        }
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

    private List<Path> pendingReports() throws IOException {
        if (!Files.isDirectory(commandsDir())) {
            return Collections.emptyList();
        }
        try (var stream = Files.list(commandsDir())) {
            return stream.filter(p -> p.getFileName().toString().startsWith("publication_report-"))
                    .collect(Collectors.toList());
        }
    }

}
