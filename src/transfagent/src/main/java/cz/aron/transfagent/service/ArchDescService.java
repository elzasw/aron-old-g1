package cz.aron.transfagent.service;

import java.io.IOException;
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.io.Files;

import cz.aron.api.rest.model.ArchDescDef;
import cz.aron.api.rest.model.ArchDescResponse;
import cz.aron.transfagent.controller.vo.ArchDescDefVO;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.elza.ImportArchDescCommand;
import cz.aron.transfagent.repository.ArchDescRepository;

@Service
public class ArchDescService {
	
	private static final Logger log = LoggerFactory.getLogger(ArchDescService.class);
	
	private final ArchDescRepository archDescRepository;
	
	private final EntityManager em;
	
	private final StorageService storageService;

	public ArchDescService(ArchDescRepository archDescRepository, EntityManager em, StorageService storageService) {
		this.archDescRepository = archDescRepository;
		this.em = em;
		this.storageService = storageService;
	}

	
	@Transactional
    public ArchDescResponse getArchDesc(String query, Integer instId, int page, int pageSize) {
	    CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<ArchDesc> rootCount = cqCount.from(ArchDesc.class);
        cqCount.select(cbCount.count(rootCount));
        addArchDescPredicate(cqCount, cbCount, rootCount, query, instId);
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
            addArchDescPredicate(cq, cb, root, query, instId);
            List<ArchDesc> rows = em.createQuery(cq).setFirstResult(page* pageSize)
                    .setMaxResults(pageSize).getResultList();            
            rows.stream().forEach(apu -> {                
                items.add(new ArchDescDefVO(apu));
            });
        }
        return new ArchDescResponse(items, totalItems.intValue());
	}
	
	public static <T> void addArchDescPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<ArchDesc> root, String query, Integer instId) {
     
		if (query == null && instId == null) {
			// no filter
			return;
		}
		
		List<Predicate> andPredicates = new ArrayList<>();
        
        // instId is always AND-ed
        if (instId != null && instId > 0) {
            andPredicates.add(cb.equal(root.get("fund").get("institution").get("id"), instId));
        }
		
        // code
        List<Predicate> orPredicates = new ArrayList<>();
        
        if (StringUtils.isNotBlank(query)) {
            // try UUID
            try {
                UUID parsedUuid = UUID.fromString(query);
                orPredicates.add(cb.equal(root.get("uuid"), parsedUuid));
            } catch (IllegalArgumentException ignored) {
            }

            // try integer
            try {
                Integer id = Integer.valueOf(query);
                orPredicates.add(cb.equal(root.get("id"), id));
                orPredicates.add(cb.equal(root.get("fund").get("nad"), id));
            } catch (NumberFormatException ignored) {
            }
        }
        
        if (!orPredicates.isEmpty()) {
            andPredicates.add(cb.or(orPredicates.toArray(Predicate[]::new)));
        }

        if (!andPredicates.isEmpty()) {
            cq.where(cb.and(andPredicates.toArray(Predicate[]::new)));
        }        

    }

	public void scheduleArchDescImport(long fundVersionId) {		
		var cmdFile = storageService.getInputPath().resolve("commands").resolve(ImportArchDescCommand.createCommandFileName(fundVersionId)).toFile();
		try {
			Files.touch(cmdFile);
			log.info("Import archdesc {} scheduled.", fundVersionId);
		} catch (IOException e) {
			log.error("Fail to write to file {}", cmdFile);	
		}		
	}

}
