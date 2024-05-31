package cz.aron.transfagent.service.importfromdir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.elza.dao.ArchDescLevelDaoImporter;
import cz.aron.transfagent.peva.ArchiveFundId;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.AttachmentService;
import cz.aron.transfagent.service.DaoFileStoreService;
import cz.aron.transfagent.service.DaoImportService;
import cz.aron.transfagent.service.DaoImportService.DaoSource;
import cz.aron.transfagent.service.DaoImportService.DaoSourceRef;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.LevelEnrichmentService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.CachedDataProvider;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportArchDescService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportArchDescService.class);

    private final ApTypeService apTypeService;

    private final ApuSourceService apuSourceService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final FundRepository fundRepository;

    private final InstitutionRepository institutionRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final ArchDescRepository archDescRepository;

    private final TransactionTemplate transactionTemplate;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
    
    private final ArchivalEntityService archivalEntityService;
    
    private final FileImportService fileImportService;
    
    private final DaoImportService daoImportService;
    
    // TODO some interface like  'LocalDaoSource'
    private final DaoFileStoreService daoFileStoreService;
    
    private final List<ArchDescLevelDaoImporter> archDescLevelDaoImporters;
    
    private final LevelEnrichmentService levelEnrichmentService;
    
    private final ConfigElzaArchDesc configArchDesc;
    
    private final AttachmentService attachmentService;

    final private String ARCHDESC_DIR = "archdesc";

    public ImportArchDescService(ApTypeService apTypeService, ApuSourceService apuSourceService,
            ReimportService reimportService, StorageService storageService, FundRepository fundRepository,
            InstitutionRepository institutionRepository, ApuSourceRepository apuSourceRepository,
            CoreQueueRepository coreQueueRepository, ArchDescRepository archDescRepository,
            TransactionTemplate transactionTemplate, DatabaseDataProvider databaseDataProvider,
            ConfigurationLoader configurationLoader,
            ArchivalEntityService archivalEntityService,
            FileImportService fileImportService,
            DaoImportService daoImportService,
            @Nullable DaoFileStoreService daoFileStoreService,
            @Nullable LevelEnrichmentService levelEnrichmentService,
            @Nullable ConfigElzaArchDesc configArchDesc,
            AttachmentService attachmentService,
            List<ArchDescLevelDaoImporter> archDescLevelDaoImporters) {
        this.apTypeService = apTypeService;
        this.apuSourceService = apuSourceService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.fundRepository = fundRepository;
        this.institutionRepository = institutionRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.archDescRepository = archDescRepository;
        this.transactionTemplate = transactionTemplate;
        this.databaseDataProvider = databaseDataProvider;
        this.configurationLoader = configurationLoader;
        this.archivalEntityService = archivalEntityService;
        this.fileImportService = fileImportService;
        this.daoImportService = daoImportService;
        this.daoFileStoreService = daoFileStoreService;
        this.levelEnrichmentService = levelEnrichmentService;
        this.attachmentService = attachmentService;
        if (configArchDesc==null) {
            // default config
            this.configArchDesc = new ConfigElzaArchDesc();
        } else {
            this.configArchDesc = configArchDesc;
        }
        this.archDescLevelDaoImporters = archDescLevelDaoImporters;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }
    
    @Override
    public int getPriority() { return 7; }    

    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(ARCHDESC_DIR);
    }

    /**
     * Zpracování adresářů s archdesc.xml soubory
     * 
     * @param dir zpracovavany adresar
     */
    @Override
    public boolean processDirectory(Path dir) {

        var archdescXml = getArchDesc(dir);
        if (archdescXml.isEmpty()) {
            log.warn("Directory is empty {}", dir);
            return false;
        }

        var fileName = archdescXml.get().getFileName().toString();
        var tmp = fileName.substring("archdesc-".length());
        var fundCode = tmp.substring(0, tmp.length() - ".xml".length());
        
        transactionTemplate.execute(t -> {
           importArchDesc(t, dir, fundCode, archdescXml.get());
           return null;
        });
        return true;
    }
    
    private Optional<Path> getArchDesc(Path dir) {
    	 try (var stream = Files.list(dir)) {
             return stream
                     .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("archdesc")
                             && f.getFileName().toString().endsWith(".xml"))
                     .findFirst();
         } catch (IOException ioEx) {
             throw new UncheckedIOException(ioEx);
         }
    }

    /**
     * Real import
     * 
     * Has to be run in transaction
     * @param t 
     * @param dir
     * @param fundCode
     * @param archdescXmlPath
     */
    private void importArchDesc(TransactionStatus t, Path dir, String fundCode, Path archdescXmlPath) {

        var iad = new ImportArchDesc(apTypeService, daoFileStoreService, levelEnrichmentService,
                configArchDesc, archDescLevelDaoImporters, configurationLoader.getConfig());
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = iad.importArchDesc(archdescXmlPath, new CachedDataProvider(databaseDataProvider));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }

        var institution = institutionRepository.findByCode(iad.getInstitutionCode());
        if (institution == null) {
            throw new IllegalStateException("The entry Institution code={" + iad.getInstitutionCode() + "} must exist.");
        }

        var fund = fundRepository.findByCodeAndInstitution(fundCode, institution);
        if (fund == null) {
        	if (iad.getFundId()!=null) {
        		fund = fundRepository.findByCodeAndInstitution(ArchiveFundId.createJaFaId(iad.getInstitutionCode(), iad.getFundId().toString(), null), institution);
        	}
        	if (fund==null) {
        		throw new IllegalStateException("The entry Fund code={" + fundCode + "} must exist.");
        	}
        }
        
        // reimportuji fond, aby se vytvorila vazba fond->archdescs
        fund.getApuSource().setReimport(true);

        var archDesc = archDescRepository.findByFund(fund);

        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {            
            if(archDesc!=null) {
                // set UUID from previous version
                var apuSource = archDesc.getApuSource();
                apusrcBuilder.setUuid(apuSource.getUuid());
            }
            apusrcBuilder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (archDesc == null) {
            archDesc = createArchDesc(fund, dataDir, dir, apusrcBuilder);
        } else {
            updateArchDesc(archDesc, dataDir, dir);
        }

        // Request entities and store refs
        Set<UUID> uuids = iad.getApRefs();
        archivalEntityService.updateSourceEntityLinks(archDesc.getApuSource(), uuids, null);
        daoImportService.updateDaos(archDesc.getApuSource(), getDaoSources(iad));
        var attachmentPaths = iad.getAttachments().stream().map(a -> a.getPath()).collect(Collectors.toList());
        attachmentService.updateAttachments(archDesc.getApuSource(), apusrcBuilder, attachmentPaths);
    }

    
	private List<DaoSource> getDaoSources(ImportArchDesc iad) {
		var daoSources = new ArrayList<DaoSource>();
		iad.getDaoRefs().forEach((s, r) -> {
			switch (s) {
			case "dspace":
			case "file":
			case "file2":
			case "file4":
				daoSources.add(new DaoSource(s,
						r.stream().map(x -> new DaoSourceRef(x.getUuid(), x.getHandle())).collect(Collectors.toSet())));
				break;
			default:
				throw new IllegalArgumentException("Unknown dao source " + s);
			}
		});
		return daoSources;
	}

    private ArchDesc createArchDesc(Fund fund, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder) {
    	
        var archDescUuid = UUID.randomUUID();
        var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
        var apuSourceUuid = apuSourceUuidStr == null? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr); 

        var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.ARCH_DESCS, 
            		dataDir, origDir.getFileName().toString());

        var archDesc = new ArchDesc();
        archDesc.setApuSource(apuSource);
        archDesc.setFund(fund);
        archDesc.setUuid(archDescUuid);
        archDesc = archDescRepository.save(archDesc);

        var coreQueue = new CoreQueue();
        coreQueue.setApuSource(apuSource);
        coreQueueRepository.save(coreQueue);

        log.info("ArchDesc created uuid={}", archDescUuid);
        
        return archDesc;
    }

    private void updateArchDesc(ArchDesc archDesc, Path dataDir, Path origDir) {
    	
        var oldDir = archDesc.getApuSource().getDataDir();

        var apuSource = archDesc.getApuSource();
        apuSource.setDataDir(dataDir.toString());
        apuSource.setOrigDir(origDir.getFileName().toString());

        var coreQueue = new CoreQueue();
        coreQueue.setApuSource(apuSource);

        apuSourceRepository.save(apuSource);
        coreQueueRepository.save(coreQueue);

        log.info("ArchDesc updated uuid={}, original data dir {}", archDesc.getUuid(), oldDir);
    }

    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.ARCH_DESCS)
            return Result.UNSUPPORTED;

        var archDesc = archDescRepository.findByApuSource(apuSource);
        if (archDesc == null) {
            log.error("Missing archive description: {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }        
        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        
        var archDescFile = getArchDesc(apuDir);
        if (archDescFile.isEmpty()) {
        	log.error("Fail to reimport archdesc, missing archdes-CODE.xml, dir={}", apuDir);
        	return Result.FAILED;        	
        }                
        var inputFile = archDescFile.get();
             
        ApuSourceBuilder apuSourceBuilder;
		var iad = new ImportArchDesc(apTypeService, daoFileStoreService, levelEnrichmentService, configArchDesc,
				archDescLevelDaoImporters, configurationLoader.getConfig());
        try {
            apuSourceBuilder = iad.importArchDesc(inputFile, new CachedDataProvider(databaseDataProvider));
            apuSourceBuilder.setUuid(apuSource.getUuid());
            
            // compare original apusrc.xml and newly generated
            var apuSrcXmlPath = apuDir.resolve(StorageService.APUSRC_XML);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            apuSourceBuilder.build(baos, new ApuValidator(configurationLoader.getConfig()));
            byte [] newContent = baos.toByteArray();
            if (StorageService.isContentEqual(apuSrcXmlPath, newContent)) {
                return ReimportProcessor.Result.NOCHANGES;
            }
            Files.write(apuSrcXmlPath, newContent);

            // Request entities and store refs
            Set<UUID> uuids = iad.getApRefs();        
            archivalEntityService.updateSourceEntityLinks(archDesc.getApuSource(), uuids, null);
            daoImportService.updateDaos(archDesc.getApuSource(), getDaoSources(iad));            
            var attachmentPaths = iad.getAttachments().stream().map(a->a.getPath()).collect(Collectors.toList());            
            attachmentService.updateAttachments(apuSource, apuSourceBuilder, attachmentPaths);
        } catch (Exception e) {
            log.error("Fail to process, file: {}", inputFile, e);
            return Result.FAILED;
        }
        return Result.REIMPORTED;
    }

}
