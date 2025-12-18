package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
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

import cz.aron.api.rest.model.FindingAidDef;
import cz.aron.api.rest.model.FindingAidsResponse;
import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.controller.vo.FindingAidDefVO;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.service.AttachmentSource.AttachmentDesc;


@Service
public class FindingAidService {

	private static final Logger log = LoggerFactory.getLogger(FindingAidService.class);

	private final ApuSourceService apuSourceService;

	private final ApuSourceRepository apuSourceRepository;

	private final FindingAidRepository findingAidRepository;

	private final FundRepository fundRepository;

	private final CoreQueueRepository coreQueueRepository;

	private final AttachmentService attachmentService;

	private final AttachmentRepository attachmentRepository;
	
	// private final FundRepository fundRepository;

	private final EntityManager em;

	public FindingAidService(ApuSourceService apuSourceService, ApuSourceRepository apuSourceRepository,
			FindingAidRepository findingAidRepository, FundRepository fundRepository,
			CoreQueueRepository coreQueueRepository, AttachmentService attachmentService, 
			AttachmentRepository attachmentRepository, EntityManager em) {
		super();
		this.apuSourceService = apuSourceService;
		this.apuSourceRepository = apuSourceRepository;
		this.findingAidRepository = findingAidRepository;
		this.fundRepository = fundRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentService = attachmentService;
		this.attachmentRepository = attachmentRepository;
		this.em = em;
	}

	@Transactional
	public FindingAid createFindingAid(String findingaidCode, List<Fund> relatedFunds, Institution institution,
			Path dataDir, Path origDir, ApuSourceBuilder builder, List<AttachmentDesc> attachments, boolean reimportFund,
			boolean send, Integer evidenceNumber, Integer partNumber, int numAttachments, int numDaos, String findingAidName) {

		// reload funds from db
		var funds = fundRepository.findAllById(relatedFunds.stream().map(f -> f.getId()).collect(Collectors.toList()));

		var findingaidUuid = builder.getMainApu().getUuid();
		Validate.notNull(findingaidUuid, "FindingAid UUID is null");

		var apuSourceUuidStr = builder.getApusrc().getUuid();
		var apuSourceUuid = UUID.fromString(apuSourceUuidStr);

		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FINDING_AID, dataDir,
				origDir.getFileName().toString(), numAttachments, numDaos);

		var findingAid = new FindingAid();
		findingAid.setCode(findingaidCode);
		findingAid.setName(findingAidName);
		findingAid.setUuid(UUID.fromString(findingaidUuid));
		findingAid.setApuSource(apuSource);
		findingAid.setInstitution(institution);
		findingAid.setEvidenceNumber(evidenceNumber);
		findingAid.setPartNumber(partNumber);
		for (var fund : funds) {
			findingAid.addFund(fund);
			if (reimportFund && fund.getApuSource() != null) {
				fund.getApuSource().setReimport(true);
			}
		}
		findingAid = findingAidRepository.save(findingAid);

		attachmentService.updateAttachments(findingAid.getApuSource(), builder, attachments);

		if (send) {
			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			coreQueueRepository.save(coreQueue);
		}

		log.info("FindingAid created code={}, uuid={}", findingaidCode, findingaidUuid);
		return findingAid;
	}

	@Transactional
	public void updateFindingAid(FindingAid findingAidSrc, Path dataDir, Path origDir, ApuSourceBuilder builder,
			List<AttachmentDesc> attachments, boolean reimportFund, boolean send, Integer evidenceNumber, Integer partNumber,
			int numAttachments, int numDaos, String findingAidName) {

		FindingAid findingAid = findingAidRepository.findById(findingAidSrc.getId())
				.orElseThrow(() -> new EntityNotFoundException());

		if (!Objects.equals(findingAid.getEvidenceNumber(), evidenceNumber)) {
			findingAid.setEvidenceNumber(evidenceNumber);
		}
		if (!Objects.equals(findingAid.getPartNumber(), partNumber)) {
			findingAid.setPartNumber(partNumber);
		}
		if (!Objects.equals(findingAid.getName(), findingAidName)) {
			findingAid.setName(findingAidName);
		}

		var apuSource = findingAid.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());
		apuSource.setNumAttachments(numAttachments);
		apuSource.setNumDaos(numDaos);

		attachmentService.updateAttachments(findingAid.getApuSource(), builder, attachments);
		apuSourceRepository.save(apuSource);

		if (send) {
			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			coreQueueRepository.save(coreQueue);
		}

		if (reimportFund) {
			for (var fund : findingAid.getFunds()) {
				fund.getApuSource().setReimport(true);
			}
		}

		log.info("FindingAid updated code={}, uuid={}, data dir: {}", findingAid.getCode(), findingAid.getUuid(),
				dataDir.toString());
	}
	
	/**
	 * Get finding aid and attachments
	 * @param institutionId id of institution
	 * @param findingAidCode finding aid code
	 * @param findingAidPartNumber finding aid part number
	 * @return {@code FindingAidAttachments} findingAid part is null when finding aid was not found
	 */
	@Transactional(readOnly = true)
	public FindingAidAttachments getFindingAidAttachments(Integer institutionId, int findingAidCode,
			Integer findingAidPartNumber) {
		FindingAid findingAid = null;
		if (findingAidPartNumber == null) {
			findingAid = findingAidRepository.findByInstitutionIdAndEvidenceNumberAndPartNumberIsNull(institutionId, findingAidCode);
		} else {
			findingAid = findingAidRepository.findByInstitutionIdAndEvidenceNumberAndPartNumber(institutionId,
					findingAidCode, findingAidPartNumber);
		}
		if (findingAid == null) {
			return new FindingAidAttachments(null, Collections.emptyList());
		} else {
			return new FindingAidAttachments(findingAid, attachmentRepository.findByApuSource(findingAid.getApuSource()));
		}
	}
	
	
	public static class FindingAidAttachments {
		
		private final FindingAid findingAid;
		
		private final List<Attachment> attachments;

		public FindingAidAttachments(FindingAid findingAid, List<Attachment> attachments) {
			super();
			this.findingAid = findingAid;
			this.attachments = attachments;
		}

		public FindingAid getFindingAid() {
			return findingAid;
		}

		public List<Attachment> getAttachments() {
			return attachments;
		}
		
	}

	@Transactional(readOnly=true)
	public FindingAidsResponse getFindingAids(String code, Integer instId, String uuid, int page, int pageSize) {
		CriteriaBuilder cbCount = em.getCriteriaBuilder();
		CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
		Root<FindingAid> rootCount = cqCount.from(FindingAid.class);
		rootCount.join(FindingAid.JPA_FUNDS, JoinType.INNER);
		cqCount.select(cbCount.count(rootCount));
		addFindingAidPredicate(cqCount, cbCount, rootCount, code, instId, uuid);
		Long totalItems = 0l;
		try {
			totalItems = em.createQuery(cqCount).getSingleResult();
		} catch (NoResultException nrEx) {
			// ignore
		}
		var items = new ArrayList<FindingAidDef>();
		if (totalItems > 0) {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<FindingAid> cq = cb.createQuery(FindingAid.class);
			Root<FindingAid> root = cq.from(FindingAid.class);
			root.join(FindingAid.JPA_FUNDS, JoinType.INNER);
			root.fetch(FindingAid.JPA_APUSOURCE, JoinType.INNER);
			addFindingAidPredicate(cq, cb, root, code, instId, uuid);
			List<FindingAid> rows = em.createQuery(cq).setFirstResult(page * pageSize).setMaxResults(pageSize)
					.getResultList();
			rows.stream().forEach(apu -> {
				items.add(new FindingAidDefVO(apu));
			});
		}
		return new FindingAidsResponse(items, totalItems.intValue());
	}

	private <T> void addFindingAidPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<FindingAid> root, String code,
			Integer instId, String uuid) {
		if (code == null && uuid == null && instId == null) {
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
                orPredicates.add(cb.equal(root.get("evidenceNumber"), id));
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
