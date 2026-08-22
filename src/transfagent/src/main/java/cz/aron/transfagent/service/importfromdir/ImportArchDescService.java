package cz.aron.transfagent.service.importfromdir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import cz.aron.transfagent.elza.ImportArchDescDescBuilder;
import cz.aron.transfagent.elza.ImportArchDescNameBuilder;
import cz.aron.transfagent.elza.ImportArchDescRefreshEntitiesCommand;
import cz.aron.transfagent.elza.ImportArchDescResultBuilder;
import cz.aron.transfagent.elza.dao.ArchDescLevelDaoImporter;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.AttachmentService;
import cz.aron.transfagent.service.DaoImportService;
import cz.aron.transfagent.service.DaoImportService.DaoSource;
import cz.aron.transfagent.service.DaoImportService.DaoSourceRef;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ImportFailureNotify;
import cz.aron.transfagent.service.ImportProtocol;
import cz.aron.transfagent.service.LevelEnrichmentService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.UnrecoverableImportException;
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

    private final CoreQueueRepository coreQueueRepository;

    private final ArchDescRepository archDescRepository;

    private final TransactionTemplate tt;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
    
    private final ArchivalEntityService archivalEntityService;
    
    private final FileImportService fileImportService;
    
    private final DaoImportService daoImportService;
    
    private final List<ArchDescLevelDaoImporter> archDescLevelDaoImporters;
    
    private final LevelEnrichmentService levelEnrichmentService;
    
    private final ConfigElzaArchDesc configArchDesc;
    
    private final AttachmentService attachmentService;
    
    private final ImportArchDescRefreshEntitiesCommand refreshEntitiesCommand;
    
    private final ImportArchDescResultBuilder resultBuilder;
    
    private final ImportArchDescNameBuilder nameBuilder;
    
    private final ImportArchDescDescBuilder descBuilder;

    private final ObjectMapper objectMapper;

    private final List<ImportFailureNotify> importFailureNotifies;

    final private String ARCHDESC_DIR = "archdesc";

    public ImportArchDescService(ApTypeService apTypeService, ApuSourceService apuSourceService,
            ReimportService reimportService, StorageService storageService, FundRepository fundRepository,
            InstitutionRepository institutionRepository,
            CoreQueueRepository coreQueueRepository, ArchDescRepository archDescRepository,
            TransactionTemplate tt, DatabaseDataProvider databaseDataProvider,
            ConfigurationLoader configurationLoader,
            ArchivalEntityService archivalEntityService,
            FileImportService fileImportService,
            DaoImportService daoImportService,
            @Nullable LevelEnrichmentService levelEnrichmentService,
            @Nullable ConfigElzaArchDesc configArchDesc,
            AttachmentService attachmentService,
            List<ArchDescLevelDaoImporter> archDescLevelDaoImporters,
            ImportArchDescRefreshEntitiesCommand refreshEntitiesCommand,
			@Nullable ImportArchDescResultBuilder resultBuilder,
			@Nullable ImportArchDescNameBuilder nameBuilder,
			@Nullable ImportArchDescDescBuilder descBuilder,
			ObjectMapper objectMapper,
			List<ImportFailureNotify> importFailureNotifies) {
        this.apTypeService = apTypeService;
        this.apuSourceService = apuSourceService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.fundRepository = fundRepository;
        this.institutionRepository = institutionRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.archDescRepository = archDescRepository;
        this.tt = tt;
        this.databaseDataProvider = databaseDataProvider;
        this.configurationLoader = configurationLoader;
        this.archivalEntityService = archivalEntityService;
        this.fileImportService = fileImportService;
        this.daoImportService = daoImportService;
        this.levelEnrichmentService = levelEnrichmentService;
        this.attachmentService = attachmentService;
        if (configArchDesc==null) {
            // default config
            this.configArchDesc = new ConfigElzaArchDesc();
        } else {
            this.configArchDesc = configArchDesc;
        }
        this.archDescLevelDaoImporters = archDescLevelDaoImporters;
        this.refreshEntitiesCommand = refreshEntitiesCommand;
        this.resultBuilder = resultBuilder;
        this.nameBuilder = nameBuilder;
        this.descBuilder = descBuilder;
        this.objectMapper = objectMapper;
        this.importFailureNotifies = importFailureNotifies;
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

        // read before the import: on failure the directory moves away, and the context is what
        // tells the source system which of its publications was rejected
        var context = readContext(dir);

        try {
            tt.execute(t -> {
               importArchDesc(t, dir, fundCode, archdescXml.get(), context);
               return null;
            });
        } catch (UnrecoverableImportException e) {
            rejectArchDesc(dir, context, e.getMessage());
        }
        return true;
    }

    /**
     * Park data that can never be imported, and tell whoever supplied it why.
     * <p>
     * Retrying repeats the same failure every cycle, so the directory leaves the input directory for
     * good - with the reason next to the data it belongs to, and reported back through
     * {@link ImportFailureNotify} to the system the data came from.
     *
     * @param dir the staged directory
     * @param context {@code notify.json} of the transfer, may be {@code null}
     * @param errorMessage what is missing or wrong
     */
    private void rejectArchDesc(Path dir, String context, String errorMessage) {
        if (!Files.isDirectory(dir)) {
            // nothing left to park, the import got far enough to move the data away
            log.error("Fail to import archdesc {}: {}", dir, errorMessage);
            notifyImportFailed(context, errorMessage);
            return;
        }
        try {
            new ImportProtocol(dir).add(errorMessage);
        } catch (Exception e) {
            log.error("Fail to write protocol to {}", dir, e);
        }
        notifyImportFailed(context, errorMessage);
        try {
            var errorDir = storageService.moveToErrorDir(dir);
            log.error("ArchDesc {} moved to error directory {}: {}", dir, errorDir, errorMessage);
        } catch (IOException e) {
            log.error("Fail to move {} to error directory.", dir, e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Report the rejection to every registered {@link ImportFailureNotify}. Data staged by hand
     * carries no context and is reported to nobody.
     */
    private void notifyImportFailed(String context, String errorMessage) {
        if (context == null || importFailureNotifies.isEmpty()) {
            return;
        }
        try {
            var notify = objectMapper.readValue(context, new TypeReference<Map<String, Object>>() {});
            for (var importFailureNotify : importFailureNotifies) {
                importFailureNotify.importFailed(notify, errorMessage);
            }
        } catch (Exception e) {
            log.error("Fail to notify import failure", e);
        }
    }

    /**
     * @return the {@code notify.json} the transfer was staged with, {@code null} when the data came
     *         with no context - a directory staged by hand rather than pulled from a source system
     */
    private String readContext(Path dir) {
        var notifyPath = dir.resolve("notify.json");
        if (!Files.isRegularFile(notifyPath)) {
            return null;
        }
        try {
            return Files.readString(notifyPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Fail to read context {}", notifyPath, e);
            return null;
        }
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
     * @param context {@code notify.json} of the transfer, may be {@code null}
     */
    private void importArchDesc(TransactionStatus t, Path dir, String fundCode, Path archdescXmlPath, String context) {

        var iad = new ImportArchDesc(apTypeService, levelEnrichmentService,
                configArchDesc, archDescLevelDaoImporters, configurationLoader.getConfig(), resultBuilder,
                nameBuilder, descBuilder, Collections.emptyMap());
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = iad.importArchDesc(archdescXmlPath, new CachedDataProvider(databaseDataProvider));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new UnrecoverableImportException("Unreadable archdesc xml: " + e.getMessage(), e);
        }

        var institution = institutionRepository.findByCode(iad.getInstitutionCode());
        if (institution == null) {
            throw new UnrecoverableImportException(
                    "The entry Institution code={" + iad.getInstitutionCode() + "} must exist.");
        }

        // Reuse the fund that importArchDesc above already resolved from the XML
        // (fi/@c, else the NAD-based jafa id). Deriving a key from the directory
        // name instead only ever matched funds whose code happens to be
        // "<institutionCode>-<nad>", so no Elza-published archdesc could import.
        var fund = fundRepository.findByUuidAndInstitution(iad.getFundApuUuid(), institution);
        if (fund == null) {
            throw new UnrecoverableImportException("The entry Fund uuid={" + iad.getFundApuUuid()
                    + "}, institution={" + iad.getInstitutionCode() + "} must exist (directory " + fundCode + ").");
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
        } catch (JAXBException | IllegalStateException e) {
            throw new UnrecoverableImportException("Invalid apu source: " + e.getMessage(), e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        int numAttachments = iad.getAttachments().size();
        int numDaos = iad.getDaoRefs().values().stream().mapToInt(Collection::size).sum();
        int numLevels = iad.getNumLevels();

        if (archDesc == null) {
            archDesc = createArchDesc(fund, dataDir, dir, apusrcBuilder, numAttachments, numDaos, numLevels);
        } else {
            updateArchDesc(archDesc, dataDir, dir, apusrcBuilder, numAttachments, numDaos, numLevels);
        }
        
                
        insertToCoreQueue(archDesc.getApuSource(), context);

        // Request entities and store refs
        Set<UUID> uuids = iad.getApRefs();
        archivalEntityService.updateSourceEntityLinks(archDesc.getApuSource(), uuids, null);
        daoImportService.updateDaos(archDesc.getApuSource(), getDaoSources(iad));
        attachmentService.updateAttachments(archDesc.getApuSource(), apusrcBuilder, iad.getAttachments());

        if (configArchDesc.isRefreshEntities()) {
        	refreshEntitiesCommand.scheduleRefresh(archDesc.getId());	
        }
    }

    private void insertToCoreQueue(ApuSource apuSource, String context) {
    	var coreQueue = new CoreQueue();
        coreQueue.setApuSource(apuSource);
        coreQueue.setContext(context);
        coreQueueRepository.save(coreQueue);
    }
    
	private List<DaoSource> getDaoSources(ImportArchDesc iad) {
		var daoSources = new ArrayList<DaoSource>();
		iad.getDaoRefs().forEach((s, r) -> {
			switch (s) {
			case "dspace":
			case "file":
			case "file2":
			case "file4":
			case "file5":
				daoSources.add(new DaoSource(s,
						r.stream().map(x -> new DaoSourceRef(x.getUuid(), x.getHandle())).collect(Collectors.toSet())));
				break;
			default:
				throw new IllegalArgumentException("Unknown dao source " + s);
			}
		});
		return daoSources;
	}

    private ArchDesc createArchDesc(Fund fund, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, int numAttachments, int numDaos, int numLevels) {
    	
        var archDescUuid = UUID.randomUUID();
        var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
        var apuSourceUuid = apuSourceUuidStr == null? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr); 

        var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.ARCH_DESCS, 
            		dataDir, origDir.getFileName().toString(), numAttachments, numDaos);

        var archDesc = new ArchDesc();
        archDesc.setApuSource(apuSource);
        archDesc.setFund(fund);
        archDesc.setUuid(archDescUuid);
        archDesc.setNumLevels(numLevels);
        var rootUuid = apusrcBuilder.getMainApu().getUuid();
        if (rootUuid!=null) {
        	archDesc.setRootUuid(UUID.fromString(apusrcBuilder.getMainApu().getUuid()));	
        }        
        archDesc = archDescRepository.save(archDesc);
        log.info("ArchDesc created uuid={}", archDescUuid);        
        return archDesc;
    }

	private void updateArchDesc(ArchDesc archDesc, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, int numAttachments, int numDaos, int numLevels) {

		var oldDir = archDesc.getApuSource().getDataDir();

		var apuSource = archDesc.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());
		apuSource.setNumAttachments(numAttachments);
		apuSource.setNumDaos(numDaos);

		updateArchDescMetadata(archDesc, apusrcBuilder, numLevels);
		log.info("ArchDesc updated uuid={}, original data dir {}", archDesc.getUuid(), oldDir);
	}    
    
    private void updateArchDescMetadata(ArchDesc archDesc, ApuSourceBuilder apusrcBuilder, int numLevels) {
    	UUID rootUuid = null;
        var mainApu = apusrcBuilder.getMainApu();
        if (mainApu!=null) {
        	if (mainApu.getUuid()!=null) {
        		rootUuid = UUID.fromString(mainApu.getUuid());
        	}
        }        
        if (!Objects.equals(rootUuid, archDesc.getRootUuid())||archDesc.getNumLevels()!=numLevels) {
        	archDesc.setRootUuid(rootUuid);
        	archDesc.setNumLevels(numLevels);            
        }
    }

    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.ARCH_DESCS)
            return Result.UNSUPPORTED;

        var existingArchDesc = archDescRepository.findByApuSource(apuSource);
        if (existingArchDesc == null) {
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
        
        var attachmentNameToUuid = attachmentService.getNameUuids(apuSource);
             
        ApuSourceBuilder apuSourceBuilder;
		var iad = new ImportArchDesc(apTypeService, levelEnrichmentService, configArchDesc,
				archDescLevelDaoImporters, configurationLoader.getConfig(), resultBuilder,
				nameBuilder, descBuilder, attachmentNameToUuid);
        try {
            apuSourceBuilder = iad.importArchDesc(inputFile, new CachedDataProvider(databaseDataProvider));
            apuSourceBuilder.setUuid(apuSource.getUuid());
            
            boolean force = apuSource.isForce();
            
            // compare original apusrc.xml and newly generated
            var apuSrcXmlPath = apuDir.resolve(StorageService.APUSRC_XML);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            apuSourceBuilder.build(baos, new ApuValidator(configurationLoader.getConfig()));
            byte [] newContent = baos.toByteArray();
            if (!force && StorageService.isContentEqual(apuSrcXmlPath, newContent)) {
                return ReimportProcessor.Result.NOCHANGES;
            }
            Files.write(apuSrcXmlPath, newContent);
            
            var numAttachments = iad.getAttachments().size();
            var numDaos = iad.getDaoRefs().values().stream().mapToInt(Collection::size).sum();
            var numLevels = iad.getNumLevels();
            
			tt.executeWithoutResult(t -> {				
				var archDesc = archDescRepository.findByIdWithApuSource(existingArchDesc.getId());
				if (archDesc==null) {
					log.error("ArchDesc not exist id={}", apuSource.getId());
					throw new RuntimeException();
				}
				// Request entities and store refs
				updateArchDescMetadata(archDesc, apuSourceBuilder, numLevels);
				archDesc.getApuSource().setNumAttachments(numAttachments);
				archDesc.getApuSource().setNumDaos(numDaos);
				archivalEntityService.updateSourceEntityLinks(archDesc.getApuSource(), iad.getApRefs(), null);
				daoImportService.updateDaos(archDesc.getApuSource(), getDaoSources(iad));
				attachmentService.updateAttachments(apuSource, apuSourceBuilder, iad.getAttachments());
			});
        } catch (Exception e) {
            log.error("Fail to process, file: {}", inputFile, e);
            return Result.FAILED;
        }
        log.info("ArchDesc reimported uuid={}", apuSource.getUuid());
        if (apuSource.isDisableSend()) {
			return ReimportProcessor.Result.REIMPORTED_NO_SEND;
		} else {
			return ReimportProcessor.Result.REIMPORTED;
		}
    }

}
