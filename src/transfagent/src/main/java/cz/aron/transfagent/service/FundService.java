package cz.aron.transfagent.service;

import java.nio.file.Path;
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

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.FundDef;
import cz.aron.api.rest.model.FundsResponse;
import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.controller.vo.FundDefVO;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;

@Service
public class FundService {

	private static final Logger log = LoggerFactory.getLogger(FundService.class);

	private final ApuSourceService apuSourceService;

	private final ApuSourceRepository apuSourceRepository;

	private final FundRepository fundRepository;

	private final CoreQueueRepository coreQueueRepository;
	
	private final AttachmentService attachmentService;
	
	private final EntityManager em;

	public FundService(ApuSourceService apuSourceService, ApuSourceRepository apuSourceRepository,
			FundRepository fundRepository, CoreQueueRepository coreQueueRepository, AttachmentService attachmentService,
			EntityManager em) {
		this.apuSourceService = apuSourceService;
		this.apuSourceRepository = apuSourceRepository;
		this.fundRepository = fundRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentService = attachmentService;
		this.em = em;
	}

	/**
	 * Vytvori novy fond.
	 * 
	 * Vytvori ApuSource, vytvori Fund, vlozi udalost do CoreQueue
	 * 
	 * @param institution   Instituce
	 * @param dataDir       cesta k adresari/zipu s daty
	 * @param origDir       puvodni cesta k adresari/zipu s daty
	 * @param apusrcBuilder apu builder
	 * @param fundCode      kod fondu
	 */
	@Transactional
	public Fund createFund(Institution institution, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder,
			String fundCode, String source, String name, Integer nad, Integer subNad) {

		var fundUuid = apusrcBuilder.getMainApu().getUuid();
		Validate.notNull(fundUuid, "Fund UUID is null");

		var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
		var apuSourceUuid = apuSourceUuidStr == null ? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr);

		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FUND, dataDir,
				origDir.getFileName().toString());

		var fund = new Fund();
		fund.setApuSource(apuSource);
		fund.setInstitution(institution);
		fund.setCode(fundCode);
		fund.setSource(source);
		fund.setUuid(UUID.fromString(fundUuid));
		fund.setName(name);
		fund.setNad(nad);
		fund.setSubNad(subNad);
		fund = fundRepository.save(fund);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);

		log.info("Fund created code={}, uuid={}", fundCode, fundUuid);
		return fund;
	}

	/**
	 * Aktualizuje fond
	 * 
	 * Aktualizuje ApuSource a vlozi udalost do CoreQueue
	 * 
	 * @param fund aktualizovany fond
	 * @param dataDir adresar/zip s daty
	 * @param origDir puvodni adresar/zip s daty
	 * @param insertToCoreQueue pokud je true vlozi udalost do CoreQueue
	 */
	@Transactional
	public void updateFund(Fund fund, Path dataDir, Path origDir, boolean insertToCoreQueue) {
		var oldDir = fund.getApuSource().getDataDir();
		var apuSource = fund.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());
		apuSourceRepository.save(apuSource);
		if (insertToCoreQueue) {
			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);			
			coreQueueRepository.save(coreQueue);
		}
		log.info("Fund updated code={}, uuid={}, original data dir {}", fund.getCode(), fund.getUuid(), oldDir);
	}
	
	
	@Transactional
    public FundsResponse getFunds(String code, String uuid, int page, int pageSize) {
	    CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<Fund> rootCount = cqCount.from(Fund.class);
        cqCount.select(cbCount.count(rootCount));
        addFundPredicate(cqCount, cbCount, rootCount, code, uuid);
        Long totalItems = 0l;
        try {
            totalItems = em.createQuery(cqCount).getSingleResult();
        } catch (NoResultException nrEx) {
            // ignore
        }
        var items = new ArrayList<FundDef>();
        if (totalItems > 0) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Fund> cq = cb.createQuery(Fund.class);
            Root<Fund> root = cq.from(Fund.class);
            root.fetch(Fund.JPA_APUSOURCE, JoinType.INNER);
            addFundPredicate(cq, cb, root, code, uuid);
            List<Fund> rows = em.createQuery(cq).setFirstResult(page* pageSize)
                    .setMaxResults(pageSize).getResultList();
            rows.stream().forEach(apu -> {                
                items.add(new FundDefVO(apu));
            });
        }
        return new FundsResponse(items, totalItems.intValue());
	}

	private <T> void addFundPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<Fund> root, String code, String uuid) {        
        if (code==null && uuid==null) {
            // no filter
            return;
        }
        Predicate p = cb.conjunction();
        if (code!=null) {
        	boolean predicateCreated = false;
        	try {
        		var tmpUUID = UUID.fromString(code);
        		p = cb.and(p, cb.equal(root.get("uuid"), tmpUUID));
        		predicateCreated = true;
        	} catch (IllegalArgumentException e) {
        		// ignore
        	}
        	try {
        		var id = Integer.parseInt(code);
        		p = cb.and(p, cb.equal(root.get("id"), id));
        		predicateCreated = true;
        	} catch (NumberFormatException e) {
        		// ignore
        	}        	
        	if (!predicateCreated) {
        		p = cb.and(p, cb.like(root.get("code"), "%"+code+"%"));
        	}
        }
        
        /* Z UI se nyni zasila pouze code
        if (uuid!=null) {
            p = cb.and(p, cb.equal(root.get("uuid"), UUID.fromString(uuid)));
        }*/        
        cq.where(p);
    }
}
