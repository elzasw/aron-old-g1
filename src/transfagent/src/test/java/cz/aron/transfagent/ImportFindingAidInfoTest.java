package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.ead3.ImportFindingAidInfo;
import cz.aron.transfagent.transformation.CoreTypes;

//@Disabled
public class ImportFindingAidInfoTest {

    private final static String FINDING_AID_DIR = "src/test/resources/files/findingaids/findingaid-CR2303";

    private final static String FINDING_AID_CODE = "CR2303";

    private final String FINDING_AID_FILE = "findingaid-" + FINDING_AID_CODE + ".xml";

    private final String PROPERTIES_FILE = "findingaid.properties";

    private final String PROPERTY_INST_KEY = "institution.225201010";

    private final String PROPERTY_FUND_KEY = "fund.225201010.CR2303";

    private final String APU_NAME = "Farní úřad Všestary";

    private final static String APUSRC_XML = "apusrc.xml";

    @Test
    public void testImportFundInfo() throws IOException, JAXBException {
        Path inputFile = Path.of(FINDING_AID_DIR, FINDING_AID_FILE);

        var ifai = new ImportFindingAidInfo(FINDING_AID_CODE, null);
        ApuSourceBuilder builder = ifai.importFindingAidInfo(inputFile, FINDING_AID_DIR + "/" + PROPERTIES_FILE);

        Path outputPath = Path.of(FINDING_AID_DIR, APUSRC_XML);
        try(OutputStream fos = Files.newOutputStream(outputPath)) {
            builder.build(fos);
        }

        Apu apu = builder.getApuByName(APU_NAME);
        assertNotNull(apu);

        Part part = builder.getFirstPart(apu, CoreTypes.PT_FINDINGAID_INFO);
        assertNotNull(part);

        String id = builder.getItemByPartAndType(part, CoreTypes.FINDINGAID_ID);
        String rdp = builder.getItemByPartAndType(part, CoreTypes.FINDINGAID_RELEASE_DATE_PLACE);
        String dr = builder.getItemByPartAndType(part, CoreTypes.FINDINGAID_DATE_RANGE);
        String ua = builder.getItemByPartAndType(part, CoreTypes.FINDINGAID_UNITS_AMOUNT);
        String type = builder.getItemByPartAndType(part, CoreTypes.FINDINGAID_TYPE);
        assertTrue(id.equals("1820"));
        assertTrue(rdp.equals("Hradec Králové 2018"));
        assertTrue(dr.equals("1734-1949 (1958)"));
        assertTrue(ua.equals("7,5 bm"));
        assertTrue(type.equals("Inventář"));

        Properties properties = new Properties();
        properties.load(Files.newInputStream(Path.of(FINDING_AID_DIR, PROPERTIES_FILE)));

        String instUuid = builder.getApuRef(apu, "PT_ARCH_DESC_FUND", "FUND_INST_REF").getValue();
        String fundUuid = builder.getApuRef(apu, "PT_ARCH_DESC_FUND", "FUND_REF").getValue();

        assertTrue(instUuid.equals(properties.get(PROPERTY_INST_KEY)));
        assertTrue(fundUuid.equals(properties.get(PROPERTY_FUND_KEY)));

        Files.delete(Path.of(FINDING_AID_DIR, APUSRC_XML));
    }

}
