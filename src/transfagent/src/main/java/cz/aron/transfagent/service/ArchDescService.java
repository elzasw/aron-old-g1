package cz.aron.transfagent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.ArchDescDef;
import cz.aron.api.rest.model.ArchDescResponse;
import cz.aron.transfagent.controller.vo.ArchDescDefVO;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.repository.ArchDescRepository;

@Service
public class ArchDescService {
	
	private final ArchDescRepository archDescRepository;
	
	private final EntityManager em;

	public ArchDescService(ArchDescRepository archDescRepository, EntityManager em) {
		this.archDescRepository = archDescRepository;
		this.em = em;
	}

	
	@Transactional
    public ArchDescResponse getArchDesc(String query, int page, int pageSize) {
	    CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<ArchDesc> rootCount = cqCount.from(ArchDesc.class);
        cqCount.select(cbCount.count(rootCount));
        addArchDescPredicate(cqCount, cbCount, rootCount, query);
        Long totalItems = 0l;
        try {
            totalItems = em.createQuery(cqCount).getSingleResult();
        } catch (NoResultException nrEx) {
            // ignore
        }
        var items = new ArrayList<ArchDescDef>();
        if (totalItems > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ArchDesc> cq = cb.createQuery(ArchDesc.class);
            Root<ArchDesc> root = cq.from(ArchDesc.class);
            root.fetch(ArchDesc.JPA_APUSOURCE, JoinType.INNER);
            root.fetch(ArchDesc.JPA_FUND, JoinType.INNER);
            addArchDescPredicate(cq, cb, root, query);
            List<ArchDesc> rows = em.createQuery(cq).setFirstResult(page* pageSize)
                    .setMaxResults(pageSize).getResultList();            
            rows.stream().forEach(apu -> {                
                items.add(new ArchDescDefVO(apu));
            });
        }
        return new ArchDescResponse(items, totalItems.intValue());
	}

	private <T> void addArchDescPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<ArchDesc> root, String query) {        
		if (query == null) {
			// no filter
			return;
		}
		Predicate p = cb.conjunction();
		try {
			var tmpUUID = UUID.fromString(query);
			p = cb.and(p, cb.equal(root.get("uuid"), tmpUUID));
			return;
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			var id = Integer.parseInt(query);
			p = cb.and(p, cb.equal(root.get("id"), id));
			return;
		} catch (NumberFormatException nfEx) {
			// ignore
		}
		cq.where(p);
    }

	

}
