package cz.aron.transfagent.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

import cz.aron.api.rest.model.InstitutionDef;
import cz.aron.api.rest.model.InstitutionResponse;
import cz.aron.transfagent.controller.vo.InstitutionDefVO;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.repository.InstitutionRepository;


@Service
public class InstitutionsService {

	private final EntityManager em;
	
	private final InstitutionRepository institutionRepository;

	public InstitutionsService(EntityManager em, InstitutionRepository institutionRepository) {
		this.em = em;
		this.institutionRepository = institutionRepository;
	}

	@Transactional(readOnly=true)
    public InstitutionResponse getInstitutions(String query, int page, int pageSize) {
	    CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<Institution> rootCount = cqCount.from(Institution.class);
        cqCount.select(cbCount.count(rootCount));
        addInstitutionPredicate(cqCount, cbCount, rootCount, query);
        Long totalItems = 0l;
        try {
            totalItems = em.createQuery(cqCount).getSingleResult();
        } catch (NoResultException nrEx) {
            // ignore
        }
        var items = new ArrayList<InstitutionDef>();
        if (totalItems > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Institution> cq = cb.createQuery(Institution.class);
            Root<Institution> root = cq.from(Institution.class);
            root.fetch(Institution.JPA_APUSOURCE, JoinType.INNER);
            addInstitutionPredicate(cq, cb, root, query);
            List<Institution> rows = em.createQuery(cq).setFirstResult(page* pageSize)
                    .setMaxResults(pageSize).getResultList();            
            rows.stream().forEach(apu -> {                
                items.add(new InstitutionDefVO(apu));
            });
        }
        return new InstitutionResponse(items, totalItems.intValue());
	}

	private <T> void addInstitutionPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<Institution> root, String query) {        
		if (query == null) {
			// no filter
			return;
		}
		Predicate p = cb.conjunction();
		try {
			var tmpUUID = UUID.fromString(query);
			p = cb.and(p, cb.equal(root.get("uuid"), tmpUUID));
			cq.where(p);
			return;
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			var id = Integer.parseInt(query);
			p = cb.and(p, cb.equal(root.get("id"), id));
			cq.where(p);
			return;
		} catch (NumberFormatException nfEx) {
			// ignore
		}		
    }

	public List<Institution> getInstitutionsByCodes(Collection<String> codes) {		
		var ret = new ArrayList<Institution>();
		var set = new HashSet<String>(codes);				
		for(var institution:institutionRepository.findAll()) {
			if (set.contains(institution.getCode())) {
				ret.add(institution);
			}
		}		
		return ret;
	}

}
