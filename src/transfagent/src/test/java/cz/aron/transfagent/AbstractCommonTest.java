package cz.aron.transfagent;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.DaoRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.importfromdir.ImportArchDescService;
import cz.aron.transfagent.service.importfromdir.ImportDirectService;
import cz.aron.transfagent.service.importfromdir.ImportFindingAidService;
import cz.aron.transfagent.service.importfromdir.ImportFundService;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;
import cz.aron.transfagent.service.importfromdir.TransformService;

public abstract class AbstractCommonTest {

    final static String DIR_TEST_RESOURCES = "target/test-resources";

    final static String DIR_DATA = DIR_TEST_RESOURCES + "/data";

    final static String DIR_ERROR = DIR_TEST_RESOURCES + "/error";

    final static String DIR_TMP = DIR_TEST_RESOURCES + "/tmp";

    final static String SRC_TEST_RESOURCES_FILES = "src/test/resources/files";

    final String DIR_FROM_INSTITUTION = SRC_TEST_RESOURCES_FILES + "/institutions";

    final String DIR_TO_INSTITUTION = DIR_TEST_RESOURCES + "/input/institutions";

    final String DIR_FROM_FUND = SRC_TEST_RESOURCES_FILES + "/fund";

    final String DIR_TO_FUND = DIR_TEST_RESOURCES + "/input/fund";

    final String DIR_FROM_ARCH_DESC = SRC_TEST_RESOURCES_FILES + "/archdesc";

    final String DIR_TO_ARCH_DESC = DIR_TEST_RESOURCES + "/input/archdesc";

    final String DIR_FROM_FINDING_AID = SRC_TEST_RESOURCES_FILES + "/findingaids";

    final String DIR_TO_FINDING_AID = DIR_TEST_RESOURCES + "/input/findingaids";

    final String DIR_FROM_DIRECT = SRC_TEST_RESOURCES_FILES + "/direct";

    final String DIRECT_ERR = "direct-err";

    final String DIR_FROM_DIRECT_ERR = SRC_TEST_RESOURCES_FILES + "/" + DIRECT_ERR;

    final String DIR_TO_DIRECT = DIR_TEST_RESOURCES + "/input/direct";

    final String DIR_TO_DAO = SRC_TEST_RESOURCES_FILES + "/dao";

    final static String INSTITUTION_CODE = "225201010";

    final String INSTITUTION_DIR = "institution-" + INSTITUTION_CODE;

    final static String FUND_CODE = "CR2303";

    final String FUND_DIR = "fund-" + FUND_CODE;

    final String ARCH_DESC_DIR = "archdesc-" + FUND_CODE;

    final String FINDING_AID_DIR = "findingaid-" + FUND_CODE;

    final String FILE_DIRECT = "apux-03.xml";

    final String FILE_DIRECT_EMPTY = "apux-00.xml";

    final String DIRECT_DIR = "direct-03";

    @Autowired
    ImportInstitutionService importInstitutionService;

    @Autowired
    ImportFindingAidService importFindingAidService;

    @Autowired
    ImportArchDescService importArchDescService;
    
    @Autowired
    ImportDirectService importDirectService;

    @Autowired
    ImportFundService importFundService;

    @Autowired
    TransformService transformService;

    @Autowired
    ArchivalEntityRepository archivalEntityRepository;

    @Autowired
    EntitySourceRepository entitySourceRepository;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    FindingAidRepository findingAidRepository;

    @Autowired
    public AttachmentRepository attachmentRepository;

    @Autowired
    public ApuSourceRepository apuSourceRepository;

    @Autowired
    CoreQueueRepository coreQueueRepository;

    @Autowired
    ArchDescRepository archDescRepository;

    @Autowired
    FundRepository fundRepository;

    @Autowired
    DaoRepository daoRepository;
    
    @Autowired
    public TransactionTemplate tt;

    @BeforeEach
    protected void deleteAll() {
    	attachmentRepository.deleteAll();
        entitySourceRepository.deleteAll();
        archivalEntityRepository.deleteAll();
        daoRepository.deleteAll();
        coreQueueRepository.deleteAll();
        archDescRepository.deleteAll();
        fundRepository.deleteAll();
        institutionRepository.deleteAll();
        apuSourceRepository.deleteAll();
    }

    @AfterAll
    public static void deleteDirectory() throws IOException {
        FileUtils.deleteDirectory(new File(DIR_DATA));
        FileUtils.deleteDirectory(new File(DIR_ERROR));
        FileUtils.deleteDirectory(new File(DIR_TMP));
    }

    /**
     * Aktuální datum jako řetězec znaků
     * 
     * @return YYYYMMDD
     */
    protected String getDateDir() {
        return String.format("%1$tY%1$tm%1$td", new Date()); 
    }

}
