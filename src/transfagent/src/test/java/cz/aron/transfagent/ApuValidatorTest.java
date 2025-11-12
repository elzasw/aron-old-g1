
package cz.aron.transfagent;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.elza.ImportInstitution;

//@Disabled
@SpringBootTest
public class ApuValidatorTest {

    private final static String INST_DIR = "src/test/resources/files/institutions/institution-225201010";

    private final String INST_FILE = "institution-225201010.xml";

    private final String INST_PREFIX = "institution-";

    private final String XML_EXT = ".xml";

    private final static String APUSRC_XML = "apusrc.xml";

    private ApuValidator validator;

    private ApuSourceBuilder apusrcBuilder;

    @Autowired
    ConfigurationLoader configurationLoader;

    public void initData() throws IOException, JAXBException {
        validator = new ApuValidator(configurationLoader.getConfig());

        String tmp = INST_FILE.substring(INST_PREFIX.length());
        String instCode = tmp.substring(0, tmp.length() - XML_EXT.length());

        ImportInstitution importInstitution = new ImportInstitution(null);
        apusrcBuilder = importInstitution.importInstitution(Path.of(INST_DIR, INST_FILE), instCode, null);
    }

    @Test
    public void testApuValidatorSucess() throws IOException, JAXBException {
        initData();
        try (OutputStream fos = Files.newOutputStream(Path.of(INST_DIR, APUSRC_XML))) {
            apusrcBuilder.build(fos, validator);
        }

        Files.delete(Path.of(INST_DIR, APUSRC_XML));
    }

    @Test
    public void testApuValidatorException() throws IOException, JAXBException {
        initData();
        validator.getMapItems().remove("INST_CODE");

        Assertions.assertThrows(IllegalStateException.class, () -> {
            try (OutputStream fos = Files.newOutputStream(Path.of(INST_DIR, APUSRC_XML))) {
                apusrcBuilder.build(fos, validator);
            }
        });
    }

}
