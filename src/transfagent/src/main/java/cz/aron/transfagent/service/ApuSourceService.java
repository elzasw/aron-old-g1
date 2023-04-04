package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.ApuSourceDef;
import cz.aron.api.rest.model.ApuSourceResponse;
import cz.aron.api.rest.model.ApuSourceType;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;

@Service
public class ApuSourceService {
		
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
            List<ApuSource> rows = em.createQuery(cq).setFirstResult((page-1)* pageSize)
                    .setMaxResults(pageSize).getResultList();
            rows.stream().forEach(apu -> {
                var as = new ApuSourceDef();
                as.setApusourceId(apu.getId().longValue());
                as.setDataDir(apu.getDataDir());
                as.setDeleted(apu.isDeleted());
                as.setImported(apu.getDateImported().toOffsetDateTime());
                as.setReimport(apu.isReimport());
                switch(apu.getSourceType()) {
                case ARCH_DESCS:
                    as.setSourceType(ApuSourceType.ARCH_DESCS);
                    break;
                case ARCH_ENTITY:
                    as.setSourceType(ApuSourceType.ARCH_ENTITY);
                    break;
                case COLLECTION:
                    as.setSourceType(ApuSourceType.COLLECTION);
                    break;
                case DIRECT:
                    as.setSourceType(ApuSourceType.DIRECT);
                    break;
                case FINDING_AID:
                    as.setSourceType(ApuSourceType.FINDING_AID);
                    break;
                case FUND:
                    as.setSourceType(ApuSourceType.FUND);
                    break;
                case INSTITUTION:
                    as.setSourceType(ApuSourceType.INSTITUTION);
                    break;
                }
                as.setUuid(apu.getUuid().toString());
                items.add(as);
            });
        }
        return new ApuSourceResponse(items, totalItems.intValue());
    }

    private <T> void addApuSourcePredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<ApuSource> root, String datadir, String uuid) {        
        if (datadir==null && uuid==null) {
            // no filter
            return;
        }
        Predicate p = cb.conjunction();
        if (datadir!=null) {
            p = cb.and(p, cb.like(root.get("dataDir"), "%"+datadir+"%"));
        }
        if (uuid!=null) {
            p = cb.and(p, cb.equal(root.get("uuid"), uuid));
        }        
        cq.where(p);
    }
}
