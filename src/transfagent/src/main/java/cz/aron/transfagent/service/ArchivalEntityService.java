package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.time.ZonedDateTime;
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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.EntitiesResponse;
import cz.aron.api.rest.model.EntityDef;
import cz.aron.transfagent.common.BulkOperation;
import cz.aron.transfagent.controller.vo.EntityDefVO;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.transformation.ArchEntitySourceInfo;

@Service
public class ArchivalEntityService {
	
	private static final Logger log = LoggerFactory.getLogger(ArchivalEntityService.class);

	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final EntitySourceRepository entitySourceRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private final ApuSourceService apuSourceService;
	
	private final EntityManager em;
	
	public ArchivalEntityService(ArchivalEntityRepository archivalEntityRepository,
			EntitySourceRepository entitySourceRepository, CoreQueueRepository coreQueueRepository,
			ApuSourceService apuSourceService, EntityManager em) {
		super();
		this.archivalEntityRepository = archivalEntityRepository;
		this.entitySourceRepository = entitySourceRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.apuSourceService = apuSourceService;
		this.em = em;
	}
	
	@Transactional
	public void createOrUpdateArchivalEntity(Path dataDir, Path origDir, UUID uuid, String entityClass, boolean insertToCoreQueue) {		
		Optional<ArchivalEntity> ae = archivalEntityRepository.findByUuid(uuid);
		if (ae.isPresent()) {
			updateArchivalEntity(ae.get(), dataDir, origDir, uuid, insertToCoreQueue);
		} else {								
			createArchivalEntity(dataDir, origDir, uuid, entityClass, insertToCoreQueue);
		}
	}

	/**
	 * Vytvori novou archivni entitu. Vytvori k ni udalost do CoreQueue
	 * Nastavi status na AVAILABLE
	 * 
	 * @param dataDir       cesta k adresari/zipu s daty
	 * @param origDir       puvodni cesta k adresari/zipu s daty
	 * @param apusrcBuilder apu builder
	 */
	private void createArchivalEntity(Path dataDir, Path origDir, UUID apuSourceUuid, String entityClass, boolean insertToCoreQueue) {		
		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.ARCH_ENTITY, dataDir,
				origDir.getFileName().toString(), 0, 0);
		
		var archivalEntity = new ArchivalEntity();
		archivalEntity.setApuSource(apuSource);
		archivalEntity.setDownload(false);
		archivalEntity.setEntityClass(entityClass);
		archivalEntity.setStatus(EntityStatus.AVAILABLE);
		archivalEntity.setLastUpdate(ZonedDateTime.now());
		archivalEntity.setUuid(apuSourceUuid);		
		archivalEntity = archivalEntityRepository.save(archivalEntity);		
		if (insertToCoreQueue) {
			insertToCoreQueue(apuSource);
		}
		log.info("Archival entity created, uuid={}, entityClass={}", apuSourceUuid, entityClass);
	}
	
	/**
	 * Aktualizuje archivni entitu
	 * 
	 * Aktualizuje ApuSource a vlozi udalost do CoreQueue
	 * 
	 * @param archivalEntity aktualizovana entita
	 * @param dataDir adresar/zip s daty
	 * @param origDir puvodni adresar/zip s daty
	 */
	private void updateArchivalEntity(ArchivalEntity archivalEntity, Path dataDir, Path origDir, UUID apuSourceUuid, boolean insertToCoreQueue) {		
		var apuSource = archivalEntity.getApuSource();
		if (apuSource!=null) {
			// aktualizace apusource
			apuSource.setDataDir(dataDir.toString());
			apuSource.setOrigDir(origDir.getFileName().toString());
		} else {
			apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.ARCH_ENTITY, dataDir,
					origDir.getFileName().toString(), 0, 0);
			archivalEntity.setApuSource(apuSource);
		}
		archivalEntity.setStatus(EntityStatus.AVAILABLE);
		if (insertToCoreQueue) {
			insertToCoreQueue(apuSource);
		}
		
		archivalEntityRepository.reimportConnected(archivalEntity.getId());
		
		log.info("Archival entity updated, uuid={}, entityClass={}", apuSourceUuid, archivalEntity.getEntityClass());
	}
	
	private void insertToCoreQueue(ApuSource apuSource) {
		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);
	}

	/**
	 * Vytvori archivni entity pokud neexistuji. 
	 * 
	 * Nastavi jim stav na ACCESSIBLE a download na true.
	 * 
	 * @param uuids  uuids archivnich entit
	 * @param entityClass trida archivnich entit
	 * @param apuSource apuSource, ktere odkazuje na tuto archivni entitu
	 */
	@Transactional
	public void registerAccessibleEntities(List<UUID> uuids, String entityClass, ApuSource apuSource) {
		var archivalEntities = archivalEntityRepository.findByUuidIn(uuids);
		for(var uuid:uuids) {			
			var archivalEntity = archivalEntities.stream().filter(ae->uuid.equals(ae.getUuid())).findFirst();
			archivalEntity.ifPresentOrElse(x -> {
				// ArchivalEntity existuje, pridam entity source, pokud neexistuje 
				var es  = entitySourceRepository.findByArchivalEntityAndApuSource(x,apuSource);
				if (es==null) {
					var entitySource = new EntitySource();
					entitySource.setApuSource(apuSource);
					entitySource.setArchivalEntity(x);
					entitySourceRepository.save(entitySource);		
				}
				if (x.getStatus()==EntityStatus.NOT_ACCESSIBLE&&x.getApuSource()!=null) {
					x.setStatus(EntityStatus.AVAILABLE);
				}
			}, () -> {
				// ArchivalEntity neexistuje, vytvorim novou
				var newAE = new ArchivalEntity();
				newAE.setApuSource(null);
				newAE.setDownload(true);
				newAE.setEntityClass(entityClass);
				newAE.setStatus(EntityStatus.ACCESSIBLE);
				newAE.setLastUpdate(null);
				newAE.setUuid(uuid);
				newAE = archivalEntityRepository.save(newAE);
				if (apuSource!=null) {
					var entitySource = new EntitySource();
					entitySource.setApuSource(apuSource);
					entitySource.setArchivalEntity(newAE);
					entitySourceRepository.save(entitySource);					
				}
			});			
		}
	}
	
    public ArchivalEntity createAccessibleArchivalEntity(UUID entUuid) {
        var ent = new ArchivalEntity();
        ent.setStatus(EntityStatus.ACCESSIBLE);
        ent.setUuid(entUuid);
        ent = this.archivalEntityRepository.save(ent);
        return ent;
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
	
    private EntitySource createEntitySource(ApuSource apuSource, ArchivalEntity ae) {
        // mark as required by master entity
        EntitySource es = new EntitySource();
        es.setApuSource(apuSource);
        es.setArchivalEntity(ae);
        return entitySourceRepository.save(es);
    }

	@Transactional
	public void entityNotAvailable(UUID uuid) {
		Optional<ArchivalEntity> ae = archivalEntityRepository.findByUuid(uuid);
		ae.ifPresentOrElse(e -> {
			e.setStatus(EntityStatus.NOT_AVAILABLE);
			log.info("Archival entity {} set to NOT_AVAILABLE state",uuid);
		}, () -> {
			log.error("Fail to set entity {} to NOT_AVAILABLE state. Entity not exist", uuid);
		});
	}
	
	
	public List<ArchEntitySourceInfo> getArchEntityApuSourcesWithScheduled(List<UUID> uuids) {
		return archivalEntityRepository.getArchEntityApuSourcesWithScheduled(uuids);
	}

    @Transactional
    public EntitiesResponse getEntities(String code, String uuid,
                                        Integer page, Integer pageSize) {
        CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<ArchivalEntity> rootCount = cqCount.from(ArchivalEntity.class);
        cqCount.select(cbCount.count(rootCount));
        addArchivalEntityPredicate(cqCount, cbCount, rootCount, code, uuid);
        Long totalItems = 0l;
        try {
            totalItems = em.createQuery(cqCount).getSingleResult();
        } catch (NoResultException nrEx) {
            // ignore
        }
        var items = new ArrayList<EntityDef>();
        if (totalItems > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ArchivalEntity> cq = cb.createQuery(ArchivalEntity.class);
            Root<ArchivalEntity> root = cq.from(ArchivalEntity.class);
            root.fetch(ArchivalEntity.JPA_APUSOURCE, JoinType.INNER);
            addArchivalEntityPredicate(cq, cb, root, code, uuid);
            List<ArchivalEntity> rows = em.createQuery(cq).setFirstResult(page * pageSize)
                    .setMaxResults(pageSize).getResultList();
            rows.stream().forEach(apu -> {
                items.add(new EntityDefVO(apu));
            });
        }
        return new EntitiesResponse(items, totalItems.intValue());
    }

    private <T> void addArchivalEntityPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<ArchivalEntity> root,
                                                String code, String uuid) {
        if (code == null && uuid == null) {
            // no filter
            return;
        }
        Predicate p = cb.conjunction();
        if (code != null) {        	
        	try {
        		var tmpUUID = UUID.fromString(code);
        		p = cb.and(p, cb.equal(root.get("uuid"), tmpUUID));        		
        	} catch (IllegalArgumentException e) {
        		// ignore
        	}
        	try {
        		var tmpElzaId = Integer.parseInt(code);
        		p = cb.and(p, cb.equal(root.get("elzaId"), tmpElzaId));
        	} catch (NumberFormatException e) {
        		// ignore	
        	}
        }
        /* z UI nyni chodi pouze code
        if (uuid != null) {
            p = cb.and(p, cb.equal(root.get("uuid"), UUID.fromString(uuid)));
        }*/
        cq.where(p);
    }

    
	@Transactional
	public void markEntityForDownloadAndReimport(UUID uuid) {
		Optional<ArchivalEntity> ae = archivalEntityRepository.findByUuidWithApuSource(uuid);
		if (ae.isPresent()) {
			ArchivalEntity entity = ae.get();
			entity.setDownload(true);
			entity.getApuSource().setReimport(true);
			archivalEntityRepository.save(entity);
			log.info("Archival entity uuid={}, elza_id={}, class={} marked for download and reimport", uuid, entity.getElzaId(), entity.getEntityClass());
		}
	}
    
}
