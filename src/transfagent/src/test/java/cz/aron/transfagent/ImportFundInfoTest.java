package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.transfagent.elza.ImportFundInfo;
import cz.aron.transfagent.elza.datace.LocalDateTimeRange;
import cz.aron.transfagent.transformation.CoreTypes;

//@Disabled
public class ImportFundInfoTest {

    private final static String FUND_DIR = "src/test/resources/files/fund/fund-CR2303";

    private final String FUND_FILE = "fund-CR2303.xml";

    private final String PROPERTIES_FILE = "fund.properties"; 

    private final String APU_NAME = "Gymnázium Chrudim";

    private final static String APUSRC_XML = "apusrc.xml";

    private final int Y_1855 = 1855;
    private final int Y_2013 = 2013;

    @Test
    public void testImportFundInfo() throws IOException, JAXBException {
        Path inputFile = Path.of(FUND_DIR, FUND_FILE);

        ImportFundInfo ifi = new ImportFundInfo(null);
        ApuSourceBuilder builder = ifi.importFundInfo(inputFile, FUND_DIR + "/" + PROPERTIES_FILE);

        Path outputPath = Path.of(FUND_DIR, APUSRC_XML);
        try(OutputStream fos = Files.newOutputStream(outputPath)) {
            builder.build(fos);
        }

        Apu apu = builder.getApuByName(APU_NAME);
        List<ItemDateRange> ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        assertTrue(ranges.size() == 1);

        var dateRange = ranges.get(0);
        LocalDateTimeRange idra = new LocalDateTimeRange(dateRange);
        assertTrue(idra.getFrom().getYear() == Y_1855);
        assertTrue(idra.getTo().getYear() == Y_2013);
        assertFalse(dateRange.isVisible());

        Files.delete(Path.of(FUND_DIR, APUSRC_XML));
    }

}
