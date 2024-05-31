package cz.aron.transfagent.peva;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.apux._2020.ApuType;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.peva.ImportPevaFundInfo.FundIgnored;
import cz.aron.transfagent.peva.ImportPevaFundInfo.FundProvider;
import cz.aron.transfagent.peva.ImportPevaFundInfo.FundRemoved;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.AttachmentService;
import cz.aron.transfagent.service.DaoFileStore3Service;
import cz.aron.transfagent.service.DaoImportService;
import cz.aron.transfagent.service.DaoImportService.DaoSource;
import cz.aron.transfagent.service.DaoImportService.DaoSourceRef;
import cz.aron.transfagent.service.FundService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFundService.FundImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class ImportPevaFund implements FundImporter, FundProvider {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaFund.class);
	
	private static final String PREFIX = "pevafund-";
	
	private static final String FUND_SOURCE = "peva";
	
    private final FundRepository fundRepository;
    
    private final ArchDescRepository archDescRepository;
    
    private final InstitutionRepository institutionRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
    
    private final FundService fundService;
    
    private final StorageService storageService;

    private final ConfigPeva2 configPeva2;
    
    private final TransactionTemplate tt;
    
    private final Peva2CodeListProvider codeListProvider;
    
    private final Peva2CachedEntityDownloader entityDownloader;
    
    private final ArchivalEntityService archivalEntityService;
    
    private final AttachmentService attachmentService;
    
    private final DaoImportService daoImportService;
    
    private final DaoFileStore3Service daoFileStore3Service;
    
    private final ApTypeService apTypeService;

	public ImportPevaFund(FundRepository fundRepository, InstitutionRepository institutionRepository,
			DatabaseDataProvider databaseDataProvider, ConfigurationLoader configurationLoader, ConfigPeva2 configPeva2,
			FundService fundService, StorageService storageService, TransactionTemplate tt,
			Peva2CodeListDownloader codeListDownloader, Peva2CachedEntityDownloader entityDownloader,
			ArchivalEntityService archivalEntityService, AttachmentService attachmentService,
			ArchDescRepository archDescRepository, DaoImportService daoImportService,
			@Nullable DaoFileStore3Service daoFileStore3Service,ApTypeService apTypeService) {
		super();
		this.fundRepository = fundRepository;
		this.institutionRepository = institutionRepository;
		this.databaseDataProvider = databaseDataProvider;
		this.configurationLoader = configurationLoader;
		this.configPeva2 = configPeva2;
		this.fundService = fundService;
		this.storageService = storageService;
		this.tt = tt;
		this.codeListProvider = new Peva2CodeListProvider(codeListDownloader);
		this.entityDownloader = entityDownloader;
		this.archivalEntityService = archivalEntityService;
		this.attachmentService = attachmentService;
		this.archDescRepository = archDescRepository;
		this.daoImportService = daoImportService;
		this.daoFileStore3Service = daoFileStore3Service;
		this.apTypeService = apTypeService;
	}
    
	@Override
	public ImportResult processPath(Path path) {
		
		if (!path.getFileName().toString().startsWith(PREFIX)) {
			return ImportResult.UNSUPPORTED;
		}

		var pevaFundXml = getPevaFundXmlPath(path);
		
		if (pevaFundXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", path);
            return ImportResult.FAIL;
		}
		
		if (processFundPeva(path,pevaFundXml.get())) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.UNSUPPORTED;	
		}
	}
	
	
	private static Optional<Path> getPevaFundXmlPath(Path path) {
		List<Path> xmls;
		try (var stream = Files.list(path)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) &&  f.getFileName().toString().startsWith(PREFIX)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
        	log.error("Fail to find peva fund xml path={}",path);
            throw new UncheckedIOException(ioEx);
        }
		
		return xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();	
	}
	

	private boolean processFundPeva(Path dir, Path fundXml) {
        var fileName = fundXml.getFileName().toString();
        var tmp = fileName.substring(PREFIX.length());
        var fundCode = tmp.substring(0, tmp.length() - ".xml".length());
        
        var fund = fundRepository.findByCode(fundCode);
        UUID fundUuid = (fund!=null)?fund.getUuid():null;
        
        var archDescRoot = getArchDescUUID(fund);

        var ifi = new ImportPevaFundInfo(configPeva2.getFundProperties(), apTypeService, storageService);
        ApuSourceBuilder apusrcBuilder;
 
		try {
			apusrcBuilder = ifi.importFundInfo(fundXml, fundUuid, databaseDataProvider, this, codeListProvider,
					entityDownloader, archDescRoot);
		} catch (IOException e) {
			log.error("Fail to import fund, path={}", fundXml, e);
			throw new UncheckedIOException(e);
		} catch (JAXBException e) {
			log.error("Fail to import fund, fail to parse data, path={}", fundXml, e);
			throw new IllegalStateException(e);
		} catch (FundIgnored fu) {
			return true;
		} catch (FundRemoved fr) {
			if (fund != null) {
				tt.executeWithoutResult(t->{
					var fund2 = fundRepository.findByCode(fundCode);
					if (fund!=null) {
						fund2.getApuSource().setDeleted(true);
						log.info("Fund marked deleted {}", fundCode);
					}
				});
			}
			return true;
		}
        
		var attachments = new ArrayList<Path>();
		if (fund != null) {
			apusrcBuilder.setUuid(fund.getApuSource().getUuid());
			tt.executeWithoutResult(t -> {
				fundRepository.findById(fund.getId()).ifPresentOrElse(f -> {
					attachments.addAll(readAllAttachments(dir, f, apusrcBuilder));
				}, () -> {
					throw new IllegalStateException("Fund not exist id="+fund.getId());
				});

			});
		}

        var institutionCode = ifi.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
			log.error("Fail to import fund, institution not exist, path={}, institution={}", fundXml, institutionCode);
            throw new IllegalStateException("The entry Institution code={" + institutionCode + "} must exist.");
        }
        
        var daos = addDaos(apusrcBuilder, ifi.getInstitutionCode(), ifi.getNadNumber());

        try (var fos = Files.newOutputStream(dir.resolve(StorageService.APUSRC_XML))) {
            apusrcBuilder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
        	log.error("Fail to import fund, path={}", fundXml,ioEx);
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
        	log.error("Fail to import fund, path={}", fundXml,e);
            throw new IllegalStateException(e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
        	log.error("Fail to import fund, fail to move to directory={}",dir,e);
            throw new IllegalStateException(e);
        }                
        
        tt.executeWithoutResult(t->{        	        	        	
        	var fnd = fund;        	
        	if (fnd == null) {
                fnd = fundService.createFund(institution, dataDir, dir, apusrcBuilder, fundCode, FUND_SOURCE);            
            } else {
            	// aktualizace uuid fondu, doporuceno pro vyvojovou instanci
				if (configPeva2.getFundProperties() != null && configPeva2.getFundProperties().isUpdateUUID()) {
					var fundUUIDStr = apusrcBuilder.getMainApu().getUuid();
					if (StringUtils.isNotBlank(fundUUIDStr)
							&& !Objects.equals(fund.getUuid(), UUID.fromString(fundUUIDStr))) {
						log.info("Fund {} uuid updated {}->{}", fund.getCode(), fund.getUuid(), fundUUIDStr);
						fundRepository.findById(fund.getId()).ifPresent(f -> {
							f.setUuid(UUID.fromString(fundUUIDStr));
						});
					}
				}
				fundService.updateFund(fnd, dataDir, dir, true);
            }
            var originatorUuids = ifi.getOriginatorIds().stream().map(id->UUID.fromString(id)).collect(Collectors.toList());
            archivalEntityService.registerAccessibleEntities(originatorUuids, ImportPevaOriginator.ENTITY_CLASS, fnd.getApuSource());
            var geoUuids = ifi.getGeos().stream().map(id->UUID.fromString(id)).collect(Collectors.toList());
            archivalEntityService.registerAccessibleEntities(geoUuids, ImportPevaGeo.ENTITY_CLASS, fnd.getApuSource());
            if(!ifi.getCAMOriginatorIds().isEmpty()) {
				var camUuids = ifi.getCAMOriginatorIds().stream().map(id->UUID.fromString(id)).collect(Collectors.toList());
				archivalEntityService.registerAccessibleEntities(camUuids, null,
						fund.getApuSource());
			}
            attachmentService.updateAttachments(fnd.getApuSource(), apusrcBuilder, attachments);
            if (daoFileStore3Service!=null) {
                daoImportService.updateDaos(fnd.getApuSource(), daos);
            }
        });
        return true;
    }

	public static NadPrimarySheet readPevaFundFromDir(Path directory) throws IOException, JAXBException {
						
		var pevaFundXml = getPevaFundXmlPath(directory);
		
		if (pevaFundXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", directory);
            throw new FileNotFoundException("pevafund-*.xml");
		}
		
		GetNadSheetResponse gnsr = Peva2XmlReader.unmarshalGetNadSheetResponse(pevaFundXml.get());
		return gnsr.getNadPrimarySheet();	
	}

	@Override
	public NadPrimarySheet getFundByUUID(UUID uuid) throws IOException, JAXBException {		
		var path = tt.execute(t->{
			var fund = fundRepository.findByUuid(uuid);
			if (fund==null) {
				return null;
			}
			return storageService.getDataPath().resolve(fund.getApuSource().getDataDir());				
		});
		if (path==null) {
			return null;
		}
		return readPevaFundFromDir(path);
	}
	

	@Override
	public ReimportProcessor.Result reimport(ApuSource apuSource, Fund fund, Path apuPath) {
		
		if (!apuPath.getFileName().toString().startsWith(PREFIX)) {
			return ReimportProcessor.Result.UNSUPPORTED;
		}
		
		var fileName = PREFIX + fund.getCode() + ".xml";		
		ApuSourceBuilder apuSourceBuilder;
		var ifi = new ImportPevaFundInfo(configPeva2.getFundProperties(), apTypeService, storageService);
		try {
			apuSourceBuilder = ifi.importFundInfo(apuPath.resolve(fileName), fund.getUuid(), databaseDataProvider, this,
					codeListProvider, entityDownloader, getArchDescUUID(fund));
			apuSourceBuilder.setUuid(apuSource.getUuid());
			var attachments = new ArrayList<Path>();
			tt.executeWithoutResult(t -> {
				fundRepository.findById(fund.getId()).ifPresentOrElse(f -> {
					attachments.addAll(readAllAttachments(apuSource, f, apuSourceBuilder));
				}, () -> {
					throw new IllegalStateException("Fund not exist id="+fund.getId());
				});

			});

			var daos = addDaos(apuSourceBuilder, ifi.getInstitutionCode(), ifi.getNadNumber());
			
			// compare original apusrc.xml and newly generated
			var apuSrcXmlPath = apuPath.resolve(StorageService.APUSRC_XML);			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			apuSourceBuilder.build(baos, new ApuValidator(configurationLoader.getConfig()));
			byte [] newContent = baos.toByteArray();
			if (StorageService.isContentEqual(apuSrcXmlPath, newContent)) {				
				// zjistim jestli nebyla odeslana nejaka novejsi entita do aron core
				if(ifi.getCAMOriginatorIds().isEmpty()||camEntitiesNotChanged(ifi.getCAMOriginatorIds(), apuSource.getLastSent())) {				
					return ReimportProcessor.Result.NOCHANGES;
				}
			}
			Files.write(apuSrcXmlPath, newContent);
			
			tt.executeWithoutResult(t -> {
				// reload
				fundRepository.findById(fund.getId()).ifPresentOrElse(f -> {
					fundService.updateFund(f, apuPath, apuPath, false);
					var uuids = ifi.getOriginatorIds().stream().map(id -> UUID.fromString(id))
							.collect(Collectors.toList());
					archivalEntityService.registerAccessibleEntities(uuids, ImportPevaOriginator.ENTITY_CLASS,
							fund.getApuSource());
					if(!ifi.getCAMOriginatorIds().isEmpty()) {
						var camUuids = ifi.getCAMOriginatorIds().stream().map(id->UUID.fromString(id)).collect(Collectors.toList());
						archivalEntityService.registerAccessibleEntities(camUuids, null,
								fund.getApuSource());
					}					
					attachmentService.updateAttachments(apuSource, apuSourceBuilder, attachments);
					if (daoFileStore3Service!=null) {
					    daoImportService.updateDaos(apuSource,daos);
					}
				}, () -> {
					log.error("Fund not exist id={}",fund.getId());
					throw new IllegalStateException();
				});
			});
		} catch (Exception e) {
			log.error("Fail to reimport {}, dir={}", fileName, apuPath, e);
			return ReimportProcessor.Result.FAILED;
		}
		log.info("Fund id={}, uuid={} reimported", fund.getId(), fund.getUuid());
		return ReimportProcessor.Result.REIMPORTED;
	}
	
	
	private boolean camEntitiesNotChanged(List<String> uuids, Long lastSent) {		
		if (lastSent==null) {
			// jeste nebylo odeslano, nemelo by nastat
			return false;
		}		
		List<UUID> tmp = uuids.stream().map(uuid->UUID.fromString(uuid)).collect(Collectors.toList());		
		for(var aeSourceInfo :archivalEntityService.getArchEntityApuSourcesWithScheduled(tmp)) {
			if (aeSourceInfo.getLastSent()!=null && aeSourceInfo.getLastSent()>lastSent) {
				// byla odeslana novejsi entita
				return false;
			}
			if (aeSourceInfo.getLastScheduled()!=null) {
				// bude odeslana novejsi entita
				return false;
			}
		}
		return true;
	}
	
	
	private List<Path> readAllAttachments(ApuSource apuSource, Fund fund, ApuSourceBuilder apuSourceBuilder) {
		var ret = new ArrayList<Path>();
		var fundProp = configPeva2.getFundProperties();
		if (fundProp != null && fundProp.isAggregateAttachments()&&fund!=null) {
			// copy attachments
			try {
				var attachments = attachmentService.copyAttachmentsFromApus(apuSource, fund.getFindingAids().stream()
						.map(fa -> fa.getApuSource()).collect(Collectors.toList()));
				for(var attachment:attachments) {
					String mimetype = null;
					try {
						mimetype = this.storageService.detectMimetype(attachment);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					apuSourceBuilder.addAttachment(apuSourceBuilder.getMainApu(), attachment.getFileName().toString(), mimetype);
					ret.add(attachment);
				}				
			} catch (IOException e) {
				log.error("Fund, fail to import funding aid attachment, apuSource={}",apuSource.getUuid(),e);
				throw new UncheckedIOException(e);
			}
		}
		return ret;
	}
	
	
	private List<Path> readAllAttachments(Path targetDir, Fund fund, ApuSourceBuilder apuSourceBuilder) {
		var ret = new ArrayList<Path>();
		var fundProp = configPeva2.getFundProperties();
		if (fundProp != null && fundProp.isAggregateAttachments() && fund != null) {
			// copy attachments
			try {
				var attachments = attachmentService.copyAttachmentsFromApus(targetDir,
						fund.getFindingAids().stream().map(fa -> fa.getApuSource()).collect(Collectors.toList()));
				for (var attachment : attachments) {
					String mimetype = null;
					try {
						mimetype = this.storageService.detectMimetype(attachment);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					apuSourceBuilder.addAttachment(apuSourceBuilder.getMainApu(), attachment.getFileName().toString(),
							mimetype);
					ret.add(attachment);
				}
			} catch (IOException e) {
				log.error("Fund, fail to import funding aid attachment, fund={}",fund.getUuid(),e);
				throw new UncheckedIOException(e);
			}
		} else {
		    //TODO mix all together
		    // read own attachment from dir
		    // get root apu
	        var apu = apuSourceBuilder.getMainApu();	        
	        try (var stream = Files.list(targetDir)) {
	            stream.forEach(f -> {
	                if (Files.isRegularFile(f) && !f.getFileName().toString().startsWith(PREFIX)
	                        && !"protokol.txt".equals(f.getFileName().toString())
	                        && !StorageService.APUSRC_XML.equals(f.getFileName().toString())) {

	                    String mimetype = null;
	                    try {
	                        mimetype = this.storageService.detectMimetype(f);
	                    } catch (IOException e) {
	                        throw new UncheckedIOException(e);
	                    }
	                    apuSourceBuilder.addAttachment(apu, f.getFileName().toString(), mimetype);
	                    ret.add(f);
	                }
	            });
	        } catch (IOException e) {
	            log.error("Fail to read attachments from directory {}", targetDir);
	            throw new UncheckedIOException(e);
	        }		    
		}
		return ret;
	}

    private Collection<DaoSource> addDaos(ApuSourceBuilder apuSourceBuilder, String archiveCode, String fundCode) {
        if (daoFileStore3Service != null) {
            var handle = daoFileStore3Service.getFundDaoHandle(archiveCode, fundCode, null);
            if (handle != null) {
                var mainUUID = UUID.fromString(apuSourceBuilder.getMainApu().getUuid());
                var daoRefs = new HashSet<DaoSourceRef>();                
                daoRefs.add(new DaoSourceRef(mainUUID,handle));                
                var daoSource = new DaoSource(daoFileStore3Service.getName(),daoRefs);
                ApuSourceBuilder.addDao(apuSourceBuilder.getMainApu(), mainUUID);
                var part = ApuSourceBuilder.getFirstPart(apuSourceBuilder.getMainApu(), CoreTypes.PT_FUND_INFO);
                ApuSourceBuilder.addEnum(part, "DIGITAL", "Ano", false);
                return Collections.singletonList(daoSource);
            }
        }
        return Collections.emptyList();
    }

	private UUID getArchDescUUID(Fund fund) {
		ArchDesc archDesc = null;
		if (fund != null) {
			archDesc = archDescRepository.findByFund(fund);
			if (archDesc != null) {
				var apuSrcPath = storageService.getApuDataDir(archDesc.getApuSource().getDataDir())
						.resolve(StorageService.APUSRC_XML);
				try {
					var apuSource = ApuSourceBuilder.read(apuSrcPath);
					var apus = apuSource.getApus().getApu();
					if (!apus.isEmpty()) {
						var apu = apus.get(0);
						if (apu.getType() == ApuType.ARCH_DESC) {
							return UUID.fromString(apu.getUuid());
						}
					}
				} catch (JAXBException e) {
					log.error("Fail to read apusource {}", apuSrcPath, e);
					return null;
				}
			}
		}
		return null;
	}

	
	private List<DaoSource> getDaoSources(ImportArchDesc iad) {
        var daoSources = new ArrayList<DaoSource>();
        iad.getDaoRefs().forEach((s, r) -> {
            switch (s) {
            case "dspace":
            case "file":
            case "file2":
                daoSources.add(new DaoSource(s,
                        r.stream().map(x -> new DaoSourceRef(x.getUuid(), x.getHandle())).collect(Collectors.toSet())));
                break;
            default:
                throw new IllegalArgumentException("Unknown dao source " + s);
            }
        });
        return daoSources;
    }
	
}
