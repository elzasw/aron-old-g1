package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.FundDef;
import cz.aron.api.rest.model.FundsResponse;
import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.controller.vo.FundDefVO;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.service.FindingAidService.FindingAidAttachments;

@Service
public class FundService {

	private static final Logger log = LoggerFactory.getLogger(FundService.class);

	private final ApuSourceService apuSourceService;

	private final ApuSourceRepository apuSourceRepository;

	private final FundRepository fundRepository;

	private final CoreQueueRepository coreQueueRepository;
	
	private final AttachmentRepository attachmentRepository;
	
	private final EntityManager em;

	public FundService(ApuSourceService apuSourceService, ApuSourceRepository apuSourceRepository,
			FundRepository fundRepository, CoreQueueRepository coreQueueRepository,
			AttachmentRepository attachmentRepository, EntityManager em) {
		this.apuSourceService = apuSourceService;
		this.apuSourceRepository = apuSourceRepository;
		this.fundRepository = fundRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentRepository = attachmentRepository;
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
			String fundCode, String source, String name, Integer nad, Integer subNad, int numAttachments, int numDaos) {

		var fundUuid = apusrcBuilder.getMainApu().getUuid();
		Validate.notNull(fundUuid, "Fund UUID is null");

		var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
		var apuSourceUuid = apuSourceUuidStr == null ? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr);

		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FUND, dataDir,
				origDir.getFileName().toString(), numAttachments, numDaos);

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
	public void updateFund(Fund fund, Path dataDir, Path origDir, boolean insertToCoreQueue, int numAttachments, int numDaos) {
		var oldDir = fund.getApuSource().getDataDir();
		var apuSource = fund.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());
		apuSource.setNumAttachments(numAttachments);
		apuSource.setNumDaos(numDaos);
		apuSourceRepository.save(apuSource);
		if (insertToCoreQueue) {
			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);			
			coreQueueRepository.save(coreQueue);
		}
		log.info("Fund updated code={}, uuid={}, original data dir {}", fund.getCode(), fund.getUuid(), oldDir);
	}
	
	
	public static class FundAttachments {
		
		private final Fund fund;
		
		private final List<Attachment> attachments;

		public FundAttachments(Fund fund, List<Attachment> attachments) {
			this.fund = fund;
			this.attachments = attachments;
		}

		public Fund getFund() {
			return fund;
		}

		public List<Attachment> getAttachments() {
			return attachments;
		}
		
	}

	@Transactional
	public FundAttachments getFundAttachments(Integer institutionId, int nad,
			Integer subNad) {
		Fund fund = null;
		if (subNad == null) {
			fund = fundRepository.findByInstitutionIdAndNadAndSubNadIsNull(institutionId, nad);
		} else {
			fund = fundRepository.findByInstitutionIdAndNadAndSubNad(institutionId,
					nad, subNad);
		}
		if (fund == null) {
			return new FundAttachments(null, Collections.emptyList());
		} else {
			return new FundAttachments(fund, attachmentRepository.findByApuSource(fund.getApuSource()));
		}		
	}
	
	@Transactional(readOnly = true)
    public FundsResponse getFunds(String code, Integer instId, String uuid, int page, int pageSize) {
	    CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<Fund> rootCount = cqCount.from(Fund.class);
        cqCount.select(cbCount.count(rootCount));
        addFundPredicate(cqCount, cbCount, rootCount, code, instId, uuid);
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
            addFundPredicate(cq, cb, root, code, instId, uuid);
            List<Fund> rows = em.createQuery(cq).setFirstResult(page* pageSize)
                    .setMaxResults(pageSize).getResultList();
            rows.stream().forEach(apu -> {                
                items.add(new FundDefVO(apu));
            });
        }
        return new FundsResponse(items, totalItems.intValue());
	}

	public static <T> void addFundPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<Fund> root, String code, Integer instId, String uuid) {        
        if (code==null && uuid==null && instId == null) {
            // no filter
            return;
        }
        
        List<Predicate> andPredicates = new ArrayList<>();
        
        // instId is always AND-ed
        if (instId != null && instId > 0) {
            andPredicates.add(cb.equal(root.get("institution").get("id"), instId));
        }
        
        // code
        List<Predicate> orPredicates = new ArrayList<>();
        
        if (StringUtils.isNotBlank(code)) {
        	boolean predicateCreated = false;
            // try UUID
            try {
                UUID parsedUuid = UUID.fromString(code);
                orPredicates.add(cb.equal(root.get("uuid"), parsedUuid));
                predicateCreated = true;
            } catch (IllegalArgumentException ignored) {
            }

            // try integer
            try {
                Integer id = Integer.valueOf(code);
                orPredicates.add(cb.equal(root.get("id"), id));
                orPredicates.add(cb.equal(root.get("nad"), id));
                predicateCreated = true;
            } catch (NumberFormatException ignored) {
            }

            if (!predicateCreated) {
                // fallback to LIKE
                orPredicates.add(cb.like(
                        cb.lower(root.get("code")),
                        "%" + code.toLowerCase() + "%"
                ));	            
            }
        }
        
        if (!orPredicates.isEmpty()) {
            andPredicates.add(cb.or(orPredicates.toArray(Predicate[]::new)));
        }

        if (!andPredicates.isEmpty()) {
            cq.where(cb.and(andPredicates.toArray(Predicate[]::new)));
        }        
    }
}
