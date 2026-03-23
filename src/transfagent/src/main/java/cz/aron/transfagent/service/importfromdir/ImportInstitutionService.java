package cz.aron.transfagent.service.importfromdir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportInstitution;
import cz.aron.transfagent.elza.ImportInstitutionResultBuilder;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ImportProtocol;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

/**
 *  Import instituce ze vstupniho adresare
 */
@Service
public class ImportInstitutionService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportInstitutionService.class);

    private final ApuSourceService apuSourceService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final ArchivalEntityRepository archivalEntityRepository;

    private final EntitySourceRepository entitySourceRepository;

    private final InstitutionRepository institutionRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final TransactionTemplate transactionTemplate;

    private final ConfigurationLoader configurationLoader;

    private final FileImportService fileImportService;
    
    private final ArchivalEntityService archivalEntityService;

    private final String INSTITUTIONS_DIR = "institutions";
    
    private final ImportInstitutionResultBuilder resultBuilder;

    private ImportProtocol protocol;

    public ImportInstitutionService(ApuSourceService apuSourceService, ReimportService reimportService, StorageService storageService,
            ArchivalEntityRepository archivalEntityRepository, EntitySourceRepository entitySourceRepository,
            InstitutionRepository institutionRepository, ApuSourceRepository apuSourceRepository,
            CoreQueueRepository coreQueueRepository, DatabaseDataProvider databaseDataProvider, TransactionTemplate transactionTemplate,
            ConfigurationLoader configurationLoader,
            ArchivalEntityService archivalEntityService,
            FileImportService fileImportService,
            @Nullable ImportInstitutionResultBuilder resultBuilder) {
        this.apuSourceService = apuSourceService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.archivalEntityRepository = archivalEntityRepository;
        this.entitySourceRepository = entitySourceRepository;
        this.institutionRepository = institutionRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.transactionTemplate = transactionTemplate;
        this.configurationLoader = configurationLoader;
        this.archivalEntityService = archivalEntityService;
        this.fileImportService = fileImportService;
        this.resultBuilder = resultBuilder;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }

    @Override
    public int getPriority() { return 9; }
    
    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(INSTITUTIONS_DIR);
    }

	/**
	 * Najde soubor institution-${kod_archivu}.xml, vytori z nej apusrc.xml, vytvori nebo aktualizuje zaznamy v databazi a vytvori odesilaci udalost.
	 * @param dir zpracovavany adresar
	 * @return true - continue processing next directory, false - stop processing 
	 */
	@Override
	public boolean processDirectory(Path dir) {
		List<Path> xmls;
		try (var stream = Files.list(dir)) {
			 xmls = stream
					.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("institution")
							&& f.getFileName().toString().endsWith(".xml"))
					.collect(Collectors.toList());
		} catch (IOException ioEx) {
			log.error("Fail to read directory {}", dir, ioEx);
			throw new UncheckedIOException(ioEx);
		}

		var inst = xmls.stream().filter(p -> p.getFileName().toString().startsWith("institution-")
				&& p.getFileName().toString().endsWith(".xml")).findFirst();

		if (inst.isEmpty()) {
			log.warn("Directory is empty {}", dir);
			return false;
		}

		var fileName = inst.get().getFileName().toString();
		var tmp = fileName.substring("institution-".length());
		var code = tmp.substring(0,tmp.length()-".xml".length());
		
		if ("all".equals(code)) {
			copyInstitutionFiles(dir, inst.get());
			try {
				FileSystemUtils.deleteRecursively(dir);
			} catch (IOException e) {
				log.error("Fail todelete directory {}",dir);
				return false;
			}
		} else {
			// import one institution
	        protocol = new ImportProtocol(dir);
	        protocol.add("Zahájení importu");			
			importInstitution(dir, inst.get(), code);
	        protocol.add("Import byl úspěšně dokončen");
		}
        return true;
    }
	
	/**
	 * Rozkopiruje soubor se vsemi instutucemi do adresaru
	 * @param dir zdrojovy adresar
	 * @param instFilePath soubor s institucemi ve zdrojovem adresari
	 */
	private void copyInstitutionFiles(Path dir, Path instFilePath) {
		try {
			for (var instCode : ImportInstitution.readInstitutionCodes(instFilePath)) {
				Path targetDir = dir.getParent().resolve("institution-"+instCode);
				FileSystemUtils.copyRecursively(dir, targetDir);
				Path target = targetDir.resolve("institution-" + instCode + ".xml");
				Path src = targetDir.resolve(instFilePath.getFileName());
				Files.move(src, target);
			}
		} catch (Exception e) {
			protocol.add("Chyba " + e.getMessage());
			throw new IllegalStateException(e);
		}
	}
	
	private void importInstitution(Path dir, Path instFilePath, String code) {
		var ii = new ImportInstitution(resultBuilder);
		ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = ii.importInstitution(instFilePath, code, null);
        } catch (IOException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

		var institution = institutionRepository.findByCode(code);
		if (institution!=null) {
			apusrcBuilder.getApusrc().setUuid(institution.getApuSource().getUuid().toString());
			apusrcBuilder.getMainApu().setUuid(institution.getUuid().toString());
		}

        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
            apusrcBuilder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
            protocol.add("Chyba " + ioEx.getMessage());
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
            protocol.setLogPath(storageService.getDataPath().resolve(dataDir));
        } catch (IOException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

		if (institution == null) {
			createInstitution(dataDir, dir, apusrcBuilder, code, ii);
		} else {
			updateInstitution(institution, dataDir, dir, apusrcBuilder, code, ii);
		}
		
	}

	private void createInstitution(Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String institutionCode,
			ImportInstitution ii) {

		var institutionUuid = UUID.fromString(apusrcBuilder.getMainApu().getUuid());

		transactionTemplate.execute(t -> {
			UUID apusrcUuid = UUID.fromString(apusrcBuilder.getApusrc().getUuid());
			// instituce neexistuje, vytvorim novou			
			var apuSource = apuSourceService.createApuSource(apusrcUuid, SourceType.INSTITUTION, 
					dataDir, origDir.getFileName().toString(),0,0);

			var newInstitution = new Institution();
			newInstitution.setApuSource(apuSource);
			newInstitution.setCode(institutionCode);
			newInstitution.setSource("source");
			newInstitution.setUuid(institutionUuid);
			newInstitution.setName(ii.getShortName());
			newInstitution = institutionRepository.save(newInstitution);

			if (ii.getApRefUuid() != null) {
				var apUuid = ii.getApRefUuid();
				createArchivalEntityIfNotExist(apUuid, apuSource, institutionCode, false);
			}

			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			coreQueueRepository.save(coreQueue);
			return null;
		});
		log.info("Institution created code={}, uuid={}", institutionCode, institutionUuid);
	}

	private void updateInstitution(Institution institution, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String institutionCode, ImportInstitution ii) {

		var origData = institution.getApuSource().getDataDir();

		transactionTemplate.execute(t -> {
			// aktualizace, pouze zmenim datovy adresar
			var apuSource = institution.getApuSource();
			apuSource.setDataDir(dataDir.toString());
			apuSource.setOrigDir(origDir.getFileName().toString());
			
			if (ii.getApRefUuid() != null) {
				var apUuid = ii.getApRefUuid();
				createArchivalEntityIfNotExist(apUuid, apuSource, institutionCode, true);
			}
			if (!Objects.equals(institution.getName(), ii.getShortName())) {
				institution.setName(ii.getShortName());
			}

			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);

			apuSourceRepository.save(apuSource);
			institutionRepository.save(institution);
			coreQueueRepository.save(coreQueue);
			
			// TODO podminit konfiguraci aby se nevolalo zbytecne
			apuSourceRepository.reimportFundAidByInstitution(institution.getId());
			apuSourceRepository.reimportFindingAidByInstitution(institution.getId());
			return null;
		});
		log.info("Institution updated code={}, uuid={}, original data dir {}", institutionCode, institution.getUuid(), origData);
	}

	private void createArchivalEntityIfNotExist(UUID apUuid, cz.aron.transfagent.domain.ApuSource apuSource,
			String institutionCode, boolean expectExist) {
		// overim existenci ArchivalEntity
		var existingArchivalEntity = archivalEntityRepository.findByUuid(apUuid);
		if (existingArchivalEntity.isEmpty()) {
			// vytvorim archivni entitu
			var archivalEntity = archivalEntityService.createAccessibleArchivalEntity(apUuid);

			var entitySource = new EntitySource();
			entitySource.setApuSource(apuSource);
			entitySource.setArchivalEntity(archivalEntity);
			entitySourceRepository.save(entitySource);
			log.info("ArchivalEntity created {}", apUuid);
		} else {
			if (!expectExist) {
				// nemela by existovat, zaloguji warning
				log.warn("Import institution {}, archival entity {} already exist", institutionCode, apUuid);
			}
		}
	}

    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.INSTITUTION) {
            return Result.UNSUPPORTED;
        }
        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());

        protocol = new ImportProtocol(apuDir);
        protocol.add("Zahájení reimportu");

        var institution = institutionRepository.findByApuSource(apuSource);
        if (institution == null) {
            log.error("Missing institution: {}", apuSource.getId());
            protocol.add("Missing institution: " + apuSource.getId());
            return Result.UNSUPPORTED;
        }
        
        boolean force = apuSource.isForce();
        var apuSrcXmlPath = apuDir.resolve(StorageService.APUSRC_XML);
        
        String fileName = "institution-"+institution.getCode()+".xml";        
        ApuSourceBuilder apuSourceBuilder;
        var ii = new ImportInstitution(resultBuilder);
        try {
            apuSourceBuilder = ii.importInstitution(apuDir.resolve(fileName), institution.getCode(), institution.getUuid());
            apuSourceBuilder.setUuid(apuSource.getUuid());
            var baos = new ByteArrayOutputStream();
            apuSourceBuilder.build(baos, new ApuValidator(configurationLoader.getConfig()));
            var newContent = baos.toByteArray();
            if (!force && StorageService.isContentEqual(apuSrcXmlPath, newContent)) {
	            return Result.NOCHANGES;
            }
            Files.write(apuSrcXmlPath, newContent);
            if (!Objects.equals(institution.getName(), ii.getShortName())) {
				institution.setName(ii.getShortName());
				transactionTemplate.executeWithoutResult(t->{
					institutionRepository.save(institution);	
				});				
			}
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", fileName, apuDir, e);
            protocol.add("Chyba " + e.getMessage());
            return Result.FAILED;
        }
        protocol.add("Institution reimported.");
        log.info("Institution {} reimported.", institution.getCode());
        if (apuSource.isDisableSend()) {
			return ReimportProcessor.Result.REIMPORTED_NO_SEND;
		} else {
			return ReimportProcessor.Result.REIMPORTED;
		}

    }

}
