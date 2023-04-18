package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.ApuSourceDef;
import cz.aron.api.rest.model.ApuSourceResponse;
import cz.aron.transfagent.controller.vo.ApuSourceDefVO;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;

@Service
public class ApuSourceService {
    
    private static final Logger log = LoggerFactory.getLogger(ApuSourceService.class);
		
	private final ApuSourceRepository apuSourceRepository;
	
	private final EntityManager em;
	
	public ApuSourceService(final ApuSourceRepository apuSourceRepository, EntityManager em) {
		this.apuSourceRepository = apuSourceRepository;
		this.em = em;
	}

	@Transactional
	public ApuSource createApuSource(UUID apusrcUuid, SourceType sourceType, Path dataDir, String origDir) {
		
		var apuSource = new cz.aron.transfagent.domain.ApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir);
		apuSource.setSourceType(sourceType);
		apuSource.setUuid(apusrcUuid);
		apuSource.setDeleted(false);
		apuSource.setReimport(false);
		apuSource.setDateImported(ZonedDateTime.now());
		apuSource = apuSourceRepository.save(apuSource);
		
		apuSourceRepository.flush();
		return apuSource;
	}

	public ApuSource reimport(ApuSource apuSource) {
		apuSource.setReimport(true);
		return apuSourceRepository.save(apuSource);
	}

	@Transactional
    public ApuSourceResponse getApuSources(String datadir, String uuid, int page, int pageSize) {
        CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<ApuSource> rootCount = cqCount.from(ApuSource.class);
        cqCount.select(cbCount.count(rootCount));
        addApuSourcePredicate(cqCount, cbCount, rootCount, datadir, uuid);
        Long totalItems = 0l;
        try {
            totalItems = em.createQuery(cqCount).getSingleResult();
        } catch (NoResultException nrEx) {
            // ignore
        }
        var items = new ArrayList<ApuSourceDef>();
        if (totalItems > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ApuSource> cq = cb.createQuery(ApuSource.class);
            Root<ApuSource> root = cq.from(ApuSource.class);
            addApuSourcePredicate(cq, cb, root, datadir, uuid);
            List<ApuSource> rows = em.createQuery(cq).setFirstResult(page* pageSize)
                    .setMaxResults(pageSize).getResultList();
            rows.stream().forEach(apu -> {                
                items.add(new ApuSourceDefVO(apu));
            });
        }
        return new ApuSourceResponse(items, totalItems.intValue());
    }

    private <T> void addApuSourcePredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<ApuSource> root, String datadir, String uuid) {        
        if (datadir==null && uuid==null) {
            // no filter
            return;
        }
        if (uuid==null) {
            try {
                uuid = UUID.fromString(datadir).toString();
                datadir=null;
            } catch (Exception e) {
                // ignore
            }
        }        
        Predicate p = cb.conjunction();
        if (datadir!=null) {
            p = cb.and(p, cb.like(root.get("dataDir"), "%"+datadir+"%"));
        }
        if (uuid!=null) {
            p = cb.and(p, cb.equal(root.get("uuid"), UUID.fromString(uuid)));
        }        
        cq.where(p);
    }
    
    @Transactional
    public ApuSourceDef reimportApuSource(int apuSourceId) {
        var apuSource = apuSourceRepository.findById(apuSourceId).orElseThrow(() -> {
            log.error("Fail to reimport apu source id={}, not exist", apuSourceId);
            return new EntityNotFoundException();
        });
        if (!apuSource.isReimport()) {
            apuSource.setReimport(true);
            log.info("Reimport apu source id={}", apuSourceId);
        } else {
            log.warn("Apu source id={}, already marked for reimport", apuSourceId);
        }
        return new ApuSourceDefVO(apuSource);
    }
    
}
