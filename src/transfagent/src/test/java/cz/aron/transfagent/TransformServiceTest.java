package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;

//@Disabled
@SpringBootTest
public class TransformServiceTest extends AbstractCommonTest {

    private final static String DAO_UUID = "61259486-3786-4877-85b5-f845ee038132";

    private final String DIR_DAO = "dao";

    @Test
    public void testTransformService() throws IOException, JAXBException {
        var srcDataDir = Path.of(DIR_TO_DAO, DAO_UUID);
        var destDataDir = Path.of(DIR_TEST_RESOURCES, DIR_DAO);

        var daoInputDir = destDataDir.resolve(DAO_UUID);

        FileUtils.copyDirectory(srcDataDir.toFile(), daoInputDir.toFile());
        transformService.transform(daoInputDir, srcDataDir);

        Path daoUuidXml = daoInputDir.resolve("dao-" + DAO_UUID + ".xml");

        Dao dao;
        Unmarshaller unmarshaller = ApuxFactory.createUnmarshaller();
        try (InputStream is = Files.newInputStream(daoUuidXml)) {
            dao = ((JAXBElement<Dao>) unmarshaller.unmarshal(is)).getValue();
        }
        assertTrue(dao.getUuid().equals(DAO_UUID));

        int size;
        for (DaoBundle bundle : dao.getBndl()) {
            switch (bundle.getType().value()) {
            case "Published":
                size = 5;
                break;
            case "HighResView":
            case "Thumbnail":
                size = 4;
                break;
            default:
                size = 0;
            }
            assertTrue(bundle.getFile().size() == size);
        }

        FileUtils.deleteDirectory(destDataDir.toFile());
    }

}
