package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Test;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.elza.datace.LocalDateTimeRange;
import cz.aron.transfagent.transformation.CoreTypes;

//@Disabled
public class ImportArchDescTest {

    private final static String ARCHDESC_DIR = "src/test/resources/files/archdesc/archdesc-CR2303";

    private final String ARCHDESC_FILE = "archdesc-CR2303.xml";

    private final String PROPERTIES_FILE = "archdesc.properties"; 

    private final static String APUSRC_XML = "apusrc.xml";

    private final int Y_1810 = 1810;
    private final int Y_1820 = 1820;
    private final int Y_1830 = 1830;
    private final int Y_1840 = 1840;
    private final int Y_1850 = 1850;
    private final int Y_1855 = 1855;
    private final int Y_1860 = 1860;

    @Test
    public void testImportArchDesc() throws IOException, JAXBException {
        Path inputFile = Path.of(ARCHDESC_DIR, ARCHDESC_FILE);

        ImportArchDesc iad = new ImportArchDesc(new ApTypeService(),null,null,new ConfigElzaArchDesc(), Collections.emptyList(),null);
        ApuSourceBuilder builder = iad.importArchDesc(inputFile, ARCHDESC_DIR + "/" + PROPERTIES_FILE);

        Path outputPath = Path.of(ARCHDESC_DIR, APUSRC_XML);
        try(OutputStream fos = Files.newOutputStream(outputPath)) {
            builder.build(fos);
        }

        // testing root: Test datace
        Apu apu = builder.getApuByDesc(null);
        List<ItemDateRange> ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        assertTrue(ranges.size() == 1);

        var drRoot = ranges.get(0);
        LocalDateTimeRange idra = new LocalDateTimeRange(drRoot);
        assertTrue(idra.getFrom().getYear() == Y_1810);
        assertTrue(idra.getTo().getYear() == Y_1860);
        assertFalse(drRoot.isVisible());

        // testing s1
        apu = builder.getApuByDesc("s1");
        ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        assertTrue(ranges.size() == 2);

        idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == Y_1820);
        assertTrue(idra.getTo().getYear() == Y_1830);

        var dr1 = ranges.get(1);
        idra = new LocalDateTimeRange(dr1);
        assertTrue(idra.getFrom().getYear() == Y_1840);
        assertTrue(idra.getTo().getYear() == Y_1855);
        assertFalse(dr1.isVisible());

        // testing s2
        apu = builder.getApuByDesc("s2");
        ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        assertTrue(ranges.size() == 1);

        idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == Y_1810);
        assertTrue(idra.getTo().getYear() == Y_1860);

        // testing P1
        apu = builder.getApuByDesc("P1");
        ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        assertTrue(ranges.size() == 1);

        idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == Y_1820);
        assertTrue(idra.getTo().getYear() == Y_1830);

        // testing P2
        apu = builder.getApuByDesc("P2");
        ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        assertTrue(ranges.size() == 1);

        idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == Y_1840);
        assertTrue(idra.getTo().getYear() == Y_1850);

        // testing P3
        apu = builder.getApuByDesc("P3");
        ranges = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        assertTrue(ranges.size() == 1);

        idra = new LocalDateTimeRange(ranges.get(0));
        assertTrue(idra.getFrom().getYear() == Y_1840);
        assertTrue(idra.getTo().getYear() == Y_1850);

        Files.delete(Path.of(ARCHDESC_DIR, APUSRC_XML));
    }

}
