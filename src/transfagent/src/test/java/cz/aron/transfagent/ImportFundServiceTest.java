package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;

//@Disabled
@SpringBootTest
public class ImportFundServiceTest extends AbstractCommonTest {

    @Test
    @Transactional
    public void testImportFundService() throws IOException, InterruptedException {
        FileUtils.copyDirectory(new File(DIR_FROM_INSTITUTION), new File(DIR_TO_INSTITUTION));
        importInstitutionService.processDirectory(Path.of(DIR_TO_INSTITUTION, INSTITUTION_DIR));

        FileUtils.copyDirectory(new File(DIR_FROM_FUND), new File(DIR_TO_FUND));
        importFundService.processDirectory(Path.of(DIR_TO_FUND, FUND_DIR));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 2);

        ApuSource apuSource = apuSources.get(1);
        assertTrue(apuSource.getSourceType() == SourceType.FUND);

        List<Institution> institutions = institutionRepository.findAll();
        assertTrue(institutions.size() == 1);

        List<Fund> funds = fundRepository.findAll();
        assertTrue(funds.size() == 1);

        Institution institution = institutions.get(0);
        Fund fund = funds.get(0);
        assertTrue(fund.getCode().equals(FUND_CODE));
        assertTrue(fund.getApuSource().equals(apuSource));
        assertTrue(fund.getInstitution().equals(institution));

        // kontrola reimportu
        importFundService.reimport(apuSource);
    }

    /**
     * A fund naming an institution the deployment does not have yet cannot be imported by any
     * retry, so it is parked with its reason rather than left to fail every cycle - a fund waiting
     * in the input directory holds up every importer of lower priority, the archival descriptions
     * among them.
     */
    @Test
    public void testFundWithoutInstitutionIsParked() throws IOException {
        var dir = Path.of(DIR_TO_FUND, FUND_DIR);
        FileUtils.deleteDirectory(dir.toFile());
        FileUtils.copyDirectory(new File(DIR_FROM_FUND, FUND_DIR), dir.toFile());

        assertTrue(importFundService.processDirectory(dir));

        assertTrue(fundRepository.findAll().isEmpty());
        assertFalse(Files.exists(dir), "the directory must leave the input directory for good");

        var errorDateDir = Path.of(DIR_ERROR, getDateDir());
        assertTrue(Files.isDirectory(errorDateDir), "nothing was parked in " + errorDateDir);
        try (var stream = Files.list(errorDateDir)) {
            var parked = stream.filter(p -> p.getFileName().toString().startsWith(FUND_DIR))
                    .collect(Collectors.toList());
            assertEquals(1, parked.size(), "expected exactly one parked " + FUND_DIR + ", got " + parked);
            assertTrue(Files.readString(parked.get(0).resolve("protokol.txt"), StandardCharsets.UTF_8)
                    .contains("Missing institution"));
        }
    }

}
