package cz.aron.transfagent.elza;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.common.BulkOperation;
import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.IdProjection;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ArchivalEntityImportService.ArchivalEntityImporter;
import cz.aron.transfagent.service.ElzaExportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;
import cz.tacr.elza.ws.core.v1.ExportRequestException;
import cz.tacr.elza.ws.types.v1.EntitiesRequest;
import cz.tacr.elza.ws.types.v1.ExportRequest;
import cz.tacr.elza.ws.types.v1.ExportResponseData;
import cz.tacr.elza.ws.types.v1.IdentifierList;

@Service
public class ArchivalEntityImporterElza implements ArchivalEntityImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ArchivalEntityImporterElza.class);
	
	public static final String ENTITY_CLASS_ELZA = "elza";
	
	private final ApuSourceService apuSourceService;
	
	private final StorageService storageService;
	
	private final ElzaExportService elzaExportService;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final EntitySourceRepository entitySourceRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private final TransactionTemplate transactionTemplate;
	
	private final ConfigurationLoader configurationLoader;
	
	private final DatabaseDataProvider databaseDataProvider;
	
	private final ApTypeService apTypeService;
	
	private final ConfigElza configElza;

	public ArchivalEntityImporterElza(ApuSourceService apuSourceService, StorageService storageService,
			ElzaExportService elzaExportService, ArchivalEntityRepository archivalEntityRepository,
			EntitySourceRepository entitySourceRepository, CoreQueueRepository coreQueueRepository,
			TransactionTemplate transactionTemplate, ConfigurationLoader configurationLoader,
			DatabaseDataProvider databaseDataProvider, ApTypeService apTypeService,
			ConfigElza configElza) {
		this.apuSourceService = apuSourceService;
		this.storageService = storageService;
		this.elzaExportService = elzaExportService;
		this.archivalEntityRepository = archivalEntityRepository;
		this.entitySourceRepository = entitySourceRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.transactionTemplate = transactionTemplate;
		this.configurationLoader = configurationLoader;
		this.databaseDataProvider = databaseDataProvider;
		this.apTypeService = apTypeService;
		this.configElza = configElza;
	}

	@Override
	public List<String> importedClasses() {
		return Collections.singletonList(ENTITY_CLASS_ELZA);
	}

	@Override
	public boolean importEntity(IdProjection id) {
		var ret = new MutableBoolean();
		var archivalEntityId = id.getId();
		var archivalEntity = archivalEntityRepository.findById(archivalEntityId);
		archivalEntity.ifPresentOrElse(ae -> {
			if (importEntity(ae)) {
				log.info("ArchivalEntity imported {}", archivalEntityId);
			}
			ret.setTrue();
		}, () -> {
			log.error("ArchivalEntity id={}, not exist", archivalEntityId);
			ret.setFalse();
		});
		return ret.getValue();
	}

	private boolean importEntity(final ArchivalEntity ae) {
		Path tmpDir;
		try {
			tmpDir = downloadEntity(ae);
		} catch (NotAvailableException e) {
			transactionTemplate.executeWithoutResult(t -> {
				markEntityAsNotAvailable(ae);
			});
			return false;
		}

		try {
			transactionTemplate.execute(t -> {
				importEntityTrans(t, tmpDir, ae);
				return null;
			});
		} catch (ImportAp.MarkAsNonAvailableException e) {
			log.error("Entity not imported, id={}, uuid={}, elzaId={}", ae.getId(), ae.getUuid(), ae.getElzaId(), e);
			transactionTemplate.executeWithoutResult(t -> {
				markEntityAsNotAvailable(ae);
			});
			return false;
		}
		return true;
	}

	private void markEntityAsNotAvailable(ArchivalEntity ae) {
	    log.info("Marking entity as not available, id={}", ae.getId());
	    
        ArchivalEntity dbEntity = archivalEntityRepository.getOne(ae.getId());
        dbEntity.setStatus(EntityStatus.NOT_AVAILABLE);
        archivalEntityRepository.save(dbEntity);
    }
	
	private void importEntityTrans(TransactionStatus t, final Path tmpDir, final ArchivalEntity ae) {
		ApuSourceBuilder apuSourceBuilder;
		final var importAp = new ImportAp(apTypeService, new ElzaNameBuilder(apTypeService));
		Path dataDir;

		boolean deleteTmpDirectory = true;
		try {
			apuSourceBuilder = importAp.importAp(tmpDir.resolve("ap.xml"), ae.getUuid(), databaseDataProvider);
			try (var os = Files.newOutputStream(tmpDir.resolve("apusrc.xml"))) {
				apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
			}
			dataDir = storageService.moveToDataDir(tmpDir);
			saveEntity(dataDir, importAp, apuSourceBuilder, ae, importAp.getRequiredEntities(),
					apuSourceBuilder.getReferencedEntities());
			deleteTmpDirectory = false;
		} catch (ImportAp.MarkAsNonAvailableException e) {
			// vyhodim dale
			throw e;
		} catch (Exception e) {
			log.error("Fail to process downloaded ap.xml, dir={}, elzaId={}, uuid={}", tmpDir, ae.getElzaId(),
					ae.getUuid(), e);
			throw new IllegalStateException(e);
		} finally {
			if (deleteTmpDirectory)
				try {
					FileSystemUtils.deleteRecursively(tmpDir);
				} catch (IOException e1) {
					log.error("Fail to delete directory {}", tmpDir, e1);
				}
		}
	}

    /**
	 * Save entity to DB
	 * 
	 * Method requires active transaction
	 * 
	 * @param dataDir
	 * @param importAp
	 * @param apuSourceBuilder
	 * @param ae Detached source entity
     * @param requiredEntities 
     * @param referencedEntities
	 */
	private void saveEntity(Path dataDir, ImportAp importAp, 
			ApuSourceBuilder apuSourceBuilder, ArchivalEntity ae, 
			Set<Integer> requiredEntities, 
			Set<UUID> referencedEntities) {
	    log.debug("Saving entity, dataDir: {}, entityId: {}, parentElzaId: {}", dataDir.toString(), 
	              ae.getId(), importAp.getParentElzaId());
		// store subsequent hierarchical entities
		ArchivalEntity parentEntity;
		if (importAp.getParentElzaId() != null) {
			parentEntity = addAccessibleByElzaId(importAp.getParentElzaId());
		} else {
			parentEntity = null;
		}

		final ArchivalEntity srcArchivalEntity = archivalEntityRepository.getOne(ae.getId());
		boolean needReindex = false;
		if (srcArchivalEntity.getUuid() == null) {
			// we have to add uuid from downloaded data and check that it does not exists
			UUID uuid = importAp.getApUuid();
			var archivalEntity = archivalEntityRepository.findByUuid(uuid);
			if (archivalEntity.isPresent()) {
			    ArchivalEntity dbArchEntity = archivalEntity.get();
			    mergeEntities(dbArchEntity, srcArchivalEntity, parentEntity);
				return;
			} else {
				srcArchivalEntity.setUuid(uuid);
				needReindex = true;
			}
		}
		srcArchivalEntity.setEntityClass(importAp.getEntityClass());
		// update elza id
		if (srcArchivalEntity.getElzaId() == null) {			
			// check if elzaId not present with other record
			var archivalEntity = archivalEntityRepository.findByElzaId(importAp.getElzaId());
			if(archivalEntity.isPresent()) {
			    ArchivalEntity dbArchEntity = archivalEntity.get();
                mergeEntities(dbArchEntity, srcArchivalEntity, parentEntity);
                return;
			}
			srcArchivalEntity.setElzaId(importAp.getElzaId());
		}
		srcArchivalEntity.setParentEntity(parentEntity);
		if(srcArchivalEntity.getStatus()!=EntityStatus.AVAILABLE) {
			needReindex = true;
		}
		var savedArchivalEntity = saveApuSource(dataDir, srcArchivalEntity, apuSourceBuilder, requiredEntities, referencedEntities);
		
		// entity received new uuid
		if(needReindex) {
		    log.debug("Connected entities has to be reindexed.");
			// -> we have to reindex all entities having this entity as parent
			reindexAfterEntityChanged(savedArchivalEntity);
		}
	}

    private void mergeEntities(ArchivalEntity dbArchEntity, 
                               ArchivalEntity srcArchivalEntity, 
                               ArchivalEntity parentEntity) {
        log.info("Merging entities, srcEntityId: {} (elzaId: {}, uuid: {}, status: {}), targetEntityId: {} (elzaId: {}, uuid: {}, status: {})", 
                 srcArchivalEntity.getId(),
                 srcArchivalEntity.getElzaId(),
                 srcArchivalEntity.getUuid(),
                 srcArchivalEntity.getStatus(),
                 dbArchEntity.getId(),
                 dbArchEntity.getElzaId(),
                 dbArchEntity.getUuid(),
                 dbArchEntity.getStatus()
                 );
        // update current db record with parent and elzaId
        if(srcArchivalEntity.getElzaId()!=null) {
            dbArchEntity.setElzaId(srcArchivalEntity.getElzaId());
        }
        if(srcArchivalEntity.getUuid()!=null) {
            dbArchEntity.setUuid(srcArchivalEntity.getUuid());
            // reset UUID in old entity (avoid conflicts)
            srcArchivalEntity.setUuid(null);
        }
        if(dbArchEntity.getEntityClass()==null) {
            dbArchEntity.setEntityClass(srcArchivalEntity.getEntityClass());
        }
        dbArchEntity.setParentEntity(parentEntity);
        archivalEntityRepository.save(dbArchEntity);
        
        // move all links from this entity to new ae
        List<EntitySource> ess = entitySourceRepository.findByArchivalEntity(srcArchivalEntity);
        for(EntitySource es: ess) {
            es.setArchivalEntity(dbArchEntity);
            entitySourceRepository.save(es);            
        }
        if(ess.size()>0) {
            entitySourceRepository.flush();
        }
        
        // correct parent in connected entities
        var connectedEntities = archivalEntityRepository.findAllByParentEntity(srcArchivalEntity);
        for(var ce: connectedEntities) {
            ce.setParentEntity(dbArchEntity);
            archivalEntityRepository.save(ce);            
        }
        if(connectedEntities.size()>0) {
            archivalEntityRepository.flush();
        }
        
        // drop redundant
        archivalEntityRepository.delete(srcArchivalEntity);
        archivalEntityRepository.flush();
        
        if(dbArchEntity.getStatus()!=EntityStatus.ACCESSIBLE) {
            // reindex if already downloaded
            reindexAfterEntityChanged(srcArchivalEntity);
        }
    }

    private void reindexAfterEntityChanged(ArchivalEntity archivalEntity) {
		// reindex directly connected items
		List<ArchivalEntity> ents = archivalEntityRepository.findAllByParentEntity(archivalEntity);
		for(ArchivalEntity ent: ents) {
		    var apuSource = ent.getApuSource();
		    // not all connected entities has to exists		    
		    if(apuSource!=null) {
		        apuSourceService.reimport(apuSource);
		    }
		}
		// reindex all connected sources on whole subtree
		archivalEntityRepository.reimportConnected(archivalEntity.getId());
	}

	private ArchivalEntity saveApuSource(Path dataDir, ArchivalEntity ae, ApuSourceBuilder apuSourceBuilder, 
	                           Set<Integer> requiredEntities, 
	                           Set<UUID> referencedEntities) {
		UUID apusrcUuid = UUID.fromString(apuSourceBuilder.getApusrc().getUuid());
		var apuSource = apuSourceService.createApuSource(apusrcUuid, SourceType.ARCH_ENTITY, dataDir, "");

		ae.setStatus(EntityStatus.AVAILABLE);
		ae.setApuSource(apuSource);
		ArchivalEntity ret = archivalEntityRepository.save(ae);
		
		List<EntitySource> createdEntitySource;
		if(requiredEntities.size()>0) {
		    createdEntitySource = storeReqEnts(apuSource, requiredEntities);
		} else {
		    createdEntitySource = Collections.emptyList();
		}
		
		updateSourceEntityLinks(apuSource, referencedEntities, createdEntitySource);		

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);
		
		return ret;
	}

	/**
	 * 
	 * @param apuSource
	 * @param referencedEntities
	 * @param extraEntitySource List of already saved entitities
	 */
	public void updateSourceEntityLinks(ApuSource apuSource, Set<UUID> referencedEntities, 
	                                    List<EntitySource> extraEntitySource) {
	    Set<EntitySource> validEntSrcs;
	    if(extraEntitySource!=null&&extraEntitySource.size()>0) {
	        validEntSrcs = new HashSet<>();
	        validEntSrcs.addAll(extraEntitySource);
	    } else {
	        validEntSrcs = Collections.emptySet();
	    }
	    
	    // find existing links
	    List<EntitySource> entLinks = this.entitySourceRepository.findByApuSourceJoinFetchArchivalEntity(apuSource);
	    
	    Set<UUID> processedUuid = new HashSet<>();
	    List<EntitySource> deleteEntLinks = new ArrayList<>();
	    // 
	    for(var entLink: entLinks) {
	        // skip recently added entity sources
	        if(validEntSrcs.contains(entLink)) {
	            continue;
	        }
	        
	        if(referencedEntities.contains(entLink.getArchivalEntity().getUuid())) {
	            processedUuid.add(entLink.getArchivalEntity().getUuid());
	        } else {
	            deleteEntLinks.add(entLink);
	        }
	    }
	    if(deleteEntLinks.size()>0) {
	        BulkOperation.run(deleteEntLinks, 1000, batch -> entitySourceRepository.deleteInBatch(batch));
	    }
	    
	    // prepare new links
	    List<UUID> createEntLinks = new ArrayList<>();
	    for(var refEnt: referencedEntities) {
	        if(!processedUuid.contains(refEnt)) {
	            createEntLinks.add(refEnt);
	        }
	    }
	    
	    if(createEntLinks.size()>0)
	    {
	        // create source links
	        BulkOperation.run(createEntLinks, 1000,
                          batch -> {
                              // get referenced ents
                              var archEnts = this.archivalEntityRepository.findByUuidIn(batch);
                              Map<UUID, ArchivalEntity> lookup = archEnts.stream().
                                      collect(Collectors.toMap(ArchivalEntity::getUuid, Function.identity() ) );
                              for(var entUuid: batch) {
                                  var ent = lookup.get(entUuid);
                                  if(ent==null) {
                                      ent = createAccessibleArchivalEntity(entUuid);
                                  }
                                  // create source link
                                  createEntitySource(apuSource, ent);
                              }
                          });
	    }
	    // mark as inaccessible any unreferenced archival entities
	    entitySourceRepository.flush();
	    List<ArchivalEntity> ents = this.archivalEntityRepository.findNewlyUnaccesibleEntities();
	    if(CollectionUtils.isNotEmpty(ents)) {
	        log.debug("New unaccessible entities detected, count: {}", ents.size());
	        for(var ent: ents) {
	            log.debug("Marking entity as not accessible, id = {}, elzaId = {}, uuid = {}", ent.getId(), ent.getElzaId(), ent.getUuid());
	            ent.setStatus(EntityStatus.NOT_ACCESSIBLE);
	            this.archivalEntityRepository.save(ent);
	        }
	    }
    }

    private ArchivalEntity addAccessibleByElzaId(Integer elzaId) {
		Optional<ArchivalEntity> request = archivalEntityRepository.findByElzaId(elzaId);
		return request.orElseGet(()->{
			ArchivalEntity ae = new ArchivalEntity();
			ae.setElzaId(elzaId);
			ae.setStatus(EntityStatus.ACCESSIBLE);

			return archivalEntityRepository.save(ae);
		});
	}
    
    public ArchivalEntity createAccessibleArchivalEntity(UUID entUuid) {
        var ent = new ArchivalEntity();
        ent.setStatus(EntityStatus.ACCESSIBLE);
        ent.setUuid(entUuid);
        ent = this.archivalEntityRepository.save(ent);
        return ent;
    }

    private EntitySource createEntitySource(ApuSource apuSource, ArchivalEntity ae) {
        // mark as required by master entity
        EntitySource es = new EntitySource();
        es.setApuSource(apuSource);
        es.setArchivalEntity(ae);
        return entitySourceRepository.save(es);
    }
    
    private List<EntitySource> storeReqEnts(ApuSource apuSource, Set<Integer> reqEnts) {
	    List<EntitySource> storedEntSources = new ArrayList<>();
	    BulkOperation.run(reqEnts, 1000, batch -> storedEntSources.addAll(storeReqEntsInternal(apuSource, batch)));
	    return storedEntSources;
    }
    
    private List<EntitySource> storeReqEntsInternal(ApuSource apuSource, List<Integer> batch) {
        List<EntitySource> storedEntSources = new ArrayList<>();
        
        List<ArchivalEntity> ents = archivalEntityRepository.findByElzaIds(batch);
        List<Integer> newIds; 
        if(ents.size()>0) {
            Set<Integer> existingElzaIds = new HashSet<>();
                        
            newIds = new ArrayList<>(batch.size()-ents.size());
            
            for(var ent: ents) {
                existingElzaIds.add(ent.getElzaId());
                
                // entity in DB -> create only entity source
                storedEntSources.add( createEntitySource(apuSource, ent) );
            }
        } else {
            newIds = batch;
        }
        
        // insert into DB
        for(Integer elzaId: newIds) {
            ArchivalEntity ae = new ArchivalEntity();
            ae.setElzaId(elzaId);
            ae.setStatus(EntityStatus.ACCESSIBLE);
            ae = archivalEntityRepository.save(ae);
            
            storedEntSources.add( createEntitySource(apuSource, ae) );
        }
        
        return storedEntSources;
    }
    
	private void downloadEntity(ArchivalEntity ae, Path dir) throws IOException, NotAvailableException {
	    log.debug("Downloading entity from Elza, uuid: {}, elzaId: {}", ae.getUuid(), ae.getElzaId());

	    var identifierList = new IdentifierList();
        String ident;
        if(ae.getUuid()!=null) {
            ident = ae.getUuid().toString();
        } else 
        if(ae.getElzaId()!=null) {
            ident = ae.getElzaId().toString();
        } else {
            log.error("Missing identifier for archivale entity, id: {}", ae.getId());
            throw new RuntimeException("Failed to import archival entity: "+ae.getId());
        }
        identifierList.getIdentifier().add(ident);
        var entitiesRequest = new EntitiesRequest();
        entitiesRequest.setIdentifiers(identifierList);
        var exportRequest = new ExportRequest();
        exportRequest.setRequiredFormat("http://elza.tacr.cz/schema/v2");
        exportRequest.setEntities(entitiesRequest);
        var exportService = elzaExportService.get();
        
		ExportResponseData response;
		try {
			response = exportService.exportData(exportRequest);
		} catch (ExportRequestException ere) {
			// Elza was called but entity was not exported
			// -> entity is not available for download
			log.info("Entity is not available in the Elza, id={}, elzaId={}, uuid={}, message={}", ae.getId(),
					ae.getElzaId(), ae.getUuid(), ere.getMessage());
			throw new NotAvailableException(ae);
		} catch (SOAPFaultException sfEx) {
			if (StringUtils.contains(sfEx.getMessage(), "Entity has been deleted")) {
				log.info("Entity is deleted in the Elza, id={}, elzaId={}, uuid={}", ae.getId(), ae.getElzaId(),
						ae.getUuid());
				throw new NotAvailableException(ae);
			}
			throw sfEx;
		}

        var dataHandler = response.getBinData();
        
        var targetApPath = dir.resolve("ap.xml");
        if(Files.exists(targetApPath)) {
            var prevApPath = dir.resolve("apPrev.xml");
            Files.deleteIfExists(prevApPath);
            Files.move(targetApPath, prevApPath);
        }
        
        try(var os = Files.newOutputStream(targetApPath)) {
            dataHandler.writeTo(os);
        } catch (NoSuchFileException e) {
        	log.warn("Target path not exist {}", targetApPath,e);
        	// zkusim jeste jednou pro pripad, kdy byl smazan adresar
        	try {
        		Files.createDirectories(dir);
        		try(var os = Files.newOutputStream(targetApPath)) {
        			dataHandler.writeTo(os);
        		}
        	} catch (IOException e1) {
        		log.error("Fail to write downloaded ap",e);
                throw new IllegalStateException(e);
        	}        	
        } catch (IOException e) {
            log.error("Fail to write downloaded ap",e);
            throw new IllegalStateException(e);
        }
	}
	
	private Path downloadEntity(ArchivalEntity ae) throws NotAvailableException {
		Path tempDir = null;
		try {
			tempDir = storageService.createTempDir("ae-" + ae.getId().toString());
			downloadEntity(ae, tempDir);
			return tempDir;
		} catch (Exception e) {
			try {
				if (tempDir != null) {
					FileSystemUtils.deleteRecursively(tempDir);
				}
			} catch (IOException e1) {
				log.error("Fail to delete directory", e1);
			}
			
			if (e instanceof NotAvailableException) {
				log.warn("Fail to download entity, not exist in Elza, id={}, uuid={}", ae.getId(), ae.getUuid());
				throw (NotAvailableException) e;
			} else if (e instanceof RuntimeException) {
				log.error("Fail to download entity, id={}", ae.getId(), e);
				throw (RuntimeException) e;
			}
			log.error("Fail to download entity, id={}", ae.getId(), e);
			throw new IllegalStateException(e);
		}
	}
    
	static class NotAvailableException extends Exception {

        private ArchivalEntity entity;

        public NotAvailableException(final ArchivalEntity entity) {
            this.entity = entity;
        }

        private static final long serialVersionUID = 1L;
	};
	
	
	@Override
	public Result reimport(ApuSource apuSource) {
		if(apuSource.getSourceType()!=SourceType.ARCH_ENTITY) {
			return Result.UNSUPPORTED;
		}

		ArchivalEntity archEntity = archivalEntityRepository.findByApuSource(apuSource);
		if(archEntity==null) {
			log.error("Missing archival entity: {}", apuSource.getId());
			return Result.UNSUPPORTED;
		}
		if (archEntity.getEntityClass()!=null&&apTypeService.getTypeName(archEntity.getEntityClass())==null) {
			return Result.UNSUPPORTED;
		}
		if(archEntity.getStatus()!=EntityStatus.AVAILABLE) {
			log.warn("Archival entity {} cannot be reimported, status: {}", apuSource.getId(), archEntity.getStatus());
			return Result.NOCHANGES;
		}

        Path apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        if(archEntity.isDownload()) {
            try {
                downloadEntity(archEntity, apuDir);
            } catch (NotAvailableException e) {
            	transactionTemplate.executeWithoutResult(t -> {
            		markEntityAsNotAvailable(archEntity);
            	});
                return Result.NOCHANGES;
            } catch(IOException e) {
                log.error("Failed to download entity, targetDir: {}", apuDir.toString(), e);
                return Result.FAILED;
            }
            archEntity.setDownload(false);
            archivalEntityRepository.save(archEntity);
        }

		ApuSourceBuilder apuSourceBuilder;
		final var importAp = new ImportAp(apTypeService, new ElzaNameBuilder(apTypeService));
		try {
			apuSourceBuilder = importAp.importAp(apuDir.resolve("ap.xml"), archEntity.getUuid(), databaseDataProvider);
			apuSourceBuilder.setUuid(apuSource.getUuid());
				
			var apuSrcChanged = false;
			var apuSrcXmlPath = apuDir.resolve(StorageService.APUSRC_XML);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
            apuSourceBuilder.build(baos, new ApuValidator(configurationLoader.getConfig()));
            byte [] newContent = baos.toByteArray();
            if (!StorageService.isContentEqual(apuSrcXmlPath, newContent)) {
                Files.write(apuSrcXmlPath, newContent);
                apuSrcChanged = true;
            }
			
			// update parent ref
			boolean parentChanged = false;
			
			ArchivalEntity parentEntity = archEntity.getParentEntity();
			if(importAp.getParentElzaId()!=null) {			    
			    if(parentEntity==null||!importAp.getParentElzaId().equals(parentEntity.getElzaId())) {
			        parentEntity = addAccessibleByElzaId(importAp.getParentElzaId());
			        archEntity.setParentEntity(parentEntity);
			        archivalEntityRepository.save(archEntity);
			        
			        parentChanged = true;
			    }
			} else {
			    if(parentEntity!=null) {
                    archEntity.setParentEntity(null);
                    archivalEntityRepository.save(archEntity);
                    
                    parentChanged = true;
			    }
			}
			
			// TODO: spustit reimport pripojenych APU v pripade zmeny 
			//       napojeni rodice
			if(parentChanged) {
			    // not finished
			}
			
			List<EntitySource> ess = storeReqEnts(apuSource, importAp.getRequiredEntities());
			this.updateSourceEntityLinks(apuSource, apuSourceBuilder.getReferencedEntities(), ess);
			
            if (!apuSrcChanged) {
                return ReimportProcessor.Result.NOCHANGES;
            }
		} catch (Exception e) {
			log.error("Fail to process downloaded ap.xml, dir={}", apuDir, e);
			return Result.FAILED;
		}
		return Result.REIMPORTED;
	}
	
	@Override
	public boolean isDefault() {
		if (configElza!=null&&configElza.isDisabled()) {
			return false;
		} else {
			return true;
		}
	}


}
