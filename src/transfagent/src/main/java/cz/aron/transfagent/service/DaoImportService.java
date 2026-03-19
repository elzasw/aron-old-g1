package cz.aron.transfagent.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.api.rest.model.DaoDef;
import cz.aron.api.rest.model.DaosResponse;
import cz.aron.transfagent.config.ConfigDao;
import cz.aron.transfagent.controller.vo.DaoDefVO;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.DaoRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
public class DaoImportService implements ImportProcessor  {
	
	private static final Logger log = LoggerFactory.getLogger(DaoImportService.class);
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final ConfigDao configDao;
	
	private final FileImportService importService;
	
	private final DaoRepository daoRepository;
	
	private final StorageService storageService;
	
	private final TransactionTemplate transactionTemplate;
	
	private final Map<String,DaoImporter> daoImporters;
	
	private final EntityManager em;
	
	private Set<String> reportedMissingImporter;

	public DaoImportService(ApuSourceRepository apuSourceRepository, ConfigDao configDao,
			FileImportService importService, DaoRepository daoRepository, StorageService storageService,
			TransactionTemplate transactionTemplate, List<DaoImporter> daoImporters, EntityManager em) {
		this.apuSourceRepository = apuSourceRepository;
		this.configDao = configDao;
		this.importService = importService;
		this.daoRepository = daoRepository;
		this.storageService = storageService;
		this.transactionTemplate = transactionTemplate;

		var importersMap = new HashMap<String, DaoImporter>();
		if (CollectionUtils.isNotEmpty(daoImporters)) {
			for (var daoImporter : daoImporters) {
				importersMap.put(daoImporter.getName(), daoImporter);
				log.info("Dao importer {}", daoImporter.getName());
			}
		}
		this.daoImporters = importersMap;
		this.em = em;
	}

    @PostConstruct
    void init() {
        importService.registerImportProcessor(this);
    }

	@Override
	public void importData(ImportContext ic) {
		
		if (daoImporters.isEmpty()) {
            return;
        }

		reportedMissingImporter = null;
		
		int numToSend = 0;
		if (configDao.getQueueSize()!=-1) {
			var numExistingToSend = daoRepository.countByStateAndTransferred(DaoState.READY, false);
			if (numExistingToSend!=null) {
				numToSend = numExistingToSend;
				if (numToSend>configDao.getQueueSize()) {
					log.info("Import dao paused, exists {} daos prepared to be sent", numToSend);
					ic.setFailed(true);  // set failed to pause and restart execution of importers from beginning
					return;
				}
			}
		}

		var daos = daoRepository.findTop1000ByStateOrderById(DaoState.ACCESSIBLE);		
		for (var dao : daos) {
			var daoImporter = daoImporters.get(dao.getSource());
			if (daoImporter == null) {
				logMissingImporter(dao.getSource());
				continue;
			}			
			var uuidStr = dao.getUuid().toString();
			Path daoPath;
			if (configDao.isUseSubdirs()) {
				var subdir = uuidStr.substring(6, 8);			
				daoPath = storageService.getDaoPath().resolve(subdir).resolve(uuidStr);
			} else {
				daoPath = storageService.getDaoPath().resolve(uuidStr);
			}
			try {
				Files.createDirectories(daoPath);
				daoImporter.importDaoFile(dao, daoPath);
				dao.setDataDir(dao.getUuid().toString());
			} catch (Exception e) {
				ic.setFailed(true);
				log.error("Dao file not imported: {}.", dao.getId(), e);
				return;
			}
			transactionTemplate.executeWithoutResult(c -> saveDao(dao));
			ic.addProcessed();
			
			numToSend++;
			if (numToSend>configDao.getQueueSize()) {
				log.info("Import dao paused, exists {} daos prepared to be sent", numToSend);
				return;
			}
		}

	}
	
    private void saveDao(Dao dao) {
        log.debug("Saving Dao to DB, daoId: {}", dao.getId());

        var dbDao = daoRepository.findById(dao.getId())
                .orElseThrow(
                    ()-> {
                        log.error("Dao not exists in DB, id: {}", dao.getId());
                        return new RuntimeException("Dao not exists");
                    }
                );
        dbDao.setDataDir(dao.getDataDir());
        if (dbDao.getUuid() == null) {
            // Dao has no uuid -> connected apusource have to be reindexed
            var apuSource = dbDao.getApuSource();
            apuSource.setReimport(true);
            apuSourceRepository.save(apuSource);
        }
        dbDao.setUuid(dao.getUuid());
        dbDao.setState(dao.getState());
        dbDao.setTransferred(false);
        dbDao.setDownload(dao.isDownload());
        daoRepository.save(dbDao);
    }
	
	private void logMissingImporter(String source) {
		if (reportedMissingImporter == null) {
			reportedMissingImporter = new HashSet<>();
		}
		if (reportedMissingImporter.contains(source)) {
			log.error("No dao importer for source {}", source);
		}
		reportedMissingImporter.add(source);
	}
	
	public static class DaoSource {

		private final String name;

		private final Set<DaoSourceRef> daoRefs;

		public DaoSource(String name, Set<DaoSourceRef> daoRefs) {
			this.name = name;
			this.daoRefs = daoRefs;
		}

		public String getName() {
			return name;
		}

		public Set<DaoSourceRef> getDaoRefs() {
			return daoRefs;
		}

	}	
	
	public static class DaoSourceRef {
		
		private final UUID uuid;
		
		private final String handle;
		
		public DaoSourceRef(UUID uuid, String handle) {
			this.uuid = uuid;
			this.handle = handle;
		}

		public UUID getUuid() {
			return uuid;
		}

		public String getHandle() {
			return handle;
		}

		@Override
		public int hashCode() {
			return Objects.hash(handle, uuid);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DaoSourceRef other = (DaoSourceRef) obj;
			return Objects.equals(handle, other.handle) && Objects.equals(uuid, other.uuid);
		}		
		
	}
	
	/**
	 * Aktualizuje Dao. Vytvori chybejici, deaktivuje nepristupne nebo jiz
	 * neexistujici. Existujici Dao se vyhledavaji podle handle
	 * 
	 * @param apuSource
	 * @param daoRefs   mapa zdroj seznam identifikatoru dao
	 */
    public void updateDaos(ApuSource apuSource, Collection<DaoSource> daoRefs) {
        var daos = daoRepository.findByApuSource(apuSource);
        Map<String, Dao> daoLookup = new HashMap<>();
        for (var dao : daos) {
            if(StringUtils.isNotBlank(dao.getHandle())) {
                daoLookup.put(dao.getHandle(), dao);
            }
        }

        for(var daoSource:daoRefs) {
        	for(var daoRef:daoSource.getDaoRefs()) {
        		var dao = daoLookup.get(daoRef.getHandle());
        		if (dao != null && Objects.equals(daoSource.getName(), dao.getSource())) {
        			if (dao.getState() == DaoState.INACCESSIBLE) {
                        dao.setState(DaoState.ACCESSIBLE);
                        daoRepository.save(dao);
                    }
                    daoLookup.remove(dao.getHandle());
        		} else {
        			// store new dao
                    dao = new Dao();
                    dao.setHandle(daoRef.getHandle());
                    dao.setLastModified(0l);
                    dao.setApuSource(apuSource);
                    dao.setTransferred(false);
                    dao.setSource(daoSource.getName());
                    dao.setState(DaoState.ACCESSIBLE);
                    dao.setUuid(daoRef.getUuid());
                    daoRepository.save(dao);
        		}
        	}
        }

        // zbyle objekty musi byt zneplatneny
        for (var dao : daoLookup.values()) {
            dao.setState(DaoState.INACCESSIBLE);
            daoRepository.save(dao);
        }
    }
	
	    
    public interface DaoImporter {

        /**
         * Nazev importeru, zapisuje se do databaze. Nazev se pouziva pri aktualizaci
         * @return
         */
    	public String getName();
    	
    	/**
    	 * Import dao do pripraveneho adresare.
    	 * @param dao zaznam z databaze s pozadavkem na import. Obsahuje handle nebo jinou identifikaci dao,
    	 * @param daoDir pracovni adresar, do ktereho se maji vygenerovat data k odeslani do AronCore
    	 */
    	public void importDaoFile(Dao dao, Path daoDir);    		
    	
    }
    
    @Transactional
    public DaosResponse getDaos(String uuid, int page, int pageSize) {
        CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<Dao> rootCount = cqCount.from(Dao.class);
        cqCount.select(cbCount.count(rootCount));
        addDaosPredicate(cqCount, cbCount, rootCount, uuid);
        Long totalItems = 0l;
        try {
            totalItems = em.createQuery(cqCount).getSingleResult();
        } catch (NoResultException nrEx) {
            // ignore
        }
        var items = new ArrayList<DaoDef>();
        if (totalItems > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Dao> cq = cb.createQuery(Dao.class);
            Root<Dao> root = cq.from(Dao.class);
            addDaosPredicate(cq, cb, root, uuid);
            List<Dao> rows = em.createQuery(cq).setFirstResult(page * pageSize)
                    .setMaxResults(pageSize).getResultList();
            rows.stream().forEach(dao -> {
                items.add(new DaoDefVO(dao));
            });
        }
        return new DaosResponse(items, totalItems.intValue());
    }
    
    private <T> void addDaosPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<Dao> root, String query) {
        if (query == null) {
            // no filter
            return;
        }
        Predicate p = cb.conjunction();
        if (query != null) {
        	String uuid = null;
        	Integer apuSourceId = null;
        	try {
                uuid = UUID.fromString(query).toString();
            } catch (Exception e) {
                // ignore
            }
            if (uuid == null) {
            	try {
            		apuSourceId = Integer.parseInt(query);
            	} catch (NumberFormatException e) {
					// ignore
				}
			}
            if (uuid != null) {
            	p = cb.and(p, cb.equal(root.get(Dao.JPA_UUID), UUID.fromString(uuid)));
            }
            if (apuSourceId != null) {
            	p = cb.and(p, cb.equal(root.get(Dao.JPA_APUSOURCE).get(ApuSource.JPA_ID), apuSourceId));
            }
        }
        cq.where(p);
    }
    
    @Transactional
    public DaoDef reimportDao(int daoId) {                        
        var dao = daoRepository.findById(daoId).orElseThrow(() -> {
            log.error("Fail to reimport dao id={}, not exist", daoId);
            return new EntityNotFoundException();
        });        
        if (dao.getState()!=DaoState.READY) {
            log.error("Cannot mark dao id={} to be reimported. Current state {}", daoId, dao.getState());
            throw new RuntimeException();
        }
        dao.setState(DaoState.ACCESSIBLE);
        dao.setTransferred(false);
        log.info("Dao id={}, marked for reimport", daoId);
        return new DaoDefVO(dao);
    }

    @Transactional
    public int reimportDaos(Collection<String> uuids) {
        int ret = daoRepository.markForReimport(uuids);
        log.info("Daos uuids in {}, marked for reimport", uuids);
        return ret;
    }

    @Transactional
	public int reimportApuSourceDaos(int apuSourceId) {
		var numMarked = daoRepository.markForReimportByApuSourceId(apuSourceId);
		log.info("Daos from apu source id={}, marked for reimport, num marked {}", apuSourceId, numMarked);
		return numMarked;
	}

    @Transactional
    public void deleteDao(int daoId) {
    	var dao = daoRepository.findById(daoId).orElseThrow(() -> {
            log.error("Fail to reimport dao id={}, not exist", daoId);
            return new EntityNotFoundException();
        });
    	dao.setState(DaoState.INACCESSIBLE);
    }    
    
}
