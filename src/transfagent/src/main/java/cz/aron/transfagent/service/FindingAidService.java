package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.FindingAidDef;
import cz.aron.api.rest.model.FindingAidsResponse;
import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.controller.vo.FindingAidDefVO;
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

@Service
public class FindingAidService {
	
	private static final Logger log = LoggerFactory.getLogger(FindingAidService.class);
	
	private final ApuSourceService apuSourceService;
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final FindingAidRepository findingAidRepository;
	
	private final FundRepository fundRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private final AttachmentRepository attachmentRepository;
	
	private final AttachmentService attachmentService;
	
	//private final FundRepository fundRepository;
	
	private final EntityManager em;

	public FindingAidService(ApuSourceService apuSourceService, ApuSourceRepository apuSourceRepository,
			FindingAidRepository findingAidRepository, FundRepository fundRepository,
			CoreQueueRepository coreQueueRepository, AttachmentRepository attachmentRepository, AttachmentService attachmentService,
			EntityManager em) {
		super();
		this.apuSourceService = apuSourceService;
		this.apuSourceRepository = apuSourceRepository;
		this.findingAidRepository = findingAidRepository;
		this.fundRepository = fundRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentRepository = attachmentRepository;
		this.attachmentService = attachmentService;
		this.em = em;
	}

	@Transactional
	public FindingAid createFindingAid(String findingaidCode, List<Fund> relatedFunds, Institution institution,
			Path dataDir, Path origDir, ApuSourceBuilder builder, List<Path> attachments, boolean reimportFund, boolean send) {

		// reload funds from db
		var funds = fundRepository.findAllById(relatedFunds.stream().map(f -> f.getId()).collect(Collectors.toList()));

		var findingaidUuid = builder.getMainApu().getUuid();
		Validate.notNull(findingaidUuid, "FindingAid UUID is null");

		var apuSourceUuidStr = builder.getApusrc().getUuid();
		var apuSourceUuid = UUID.fromString(apuSourceUuidStr);

		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FINDING_AID, dataDir,
				origDir.getFileName().toString());

		var findingAid = new FindingAid();
		findingAid.setCode(findingaidCode);
		findingAid.setUuid(UUID.fromString(findingaidUuid));
		findingAid.setApuSource(apuSource);
		findingAid.setInstitution(institution);
		for (var fund : funds) {
			findingAid.addFund(fund);
			if (reimportFund&&fund.getApuSource()!=null) {
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
			List<Path> attachments, boolean reimportFund, boolean send) {
        
        FindingAid findingAid = findingAidRepository.findById(findingAidSrc.getId()).orElseThrow(()->new EntityNotFoundException());
        
		var apuSource = findingAid.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());

		attachmentService.updateAttachments(findingAid.getApuSource(), builder, attachments);
		apuSourceRepository.save(apuSource);

		if (send) {
			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			coreQueueRepository.save(coreQueue);
		}

		if (reimportFund) {
			for(var fund:findingAid.getFunds()) {
				fund.getApuSource().setReimport(true);
			}
		}

		log.info("FindingAid updated code={}, uuid={}, data dir: {}", findingAid.getCode(), findingAid.getUuid(),
				dataDir.toString());
	}
    
    
    @Transactional
    public FindingAidsResponse getFindingAids(String code, String uuid, int page, int pageSize) {
        CriteriaBuilder cbCount = em.getCriteriaBuilder();
        CriteriaQuery<Long> cqCount = cbCount.createQuery(Long.class);
        Root<FindingAid> rootCount = cqCount.from(FindingAid.class);
        cqCount.select(cbCount.count(rootCount));
        addFindingAidPredicate(cqCount, cbCount, rootCount, code, uuid);
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
            addFindingAidPredicate(cq, cb, root, code, uuid);
            List<FindingAid> rows = em.createQuery(cq).setFirstResult(page* pageSize)
                    .setMaxResults(pageSize).getResultList();
            rows.stream().forEach(apu -> {                
                items.add(new FindingAidDefVO(apu));
            });
        }
        return new FindingAidsResponse(items, totalItems.intValue());
    }
    
    
    private <T> void addFindingAidPredicate(CriteriaQuery<T> cq, CriteriaBuilder cb, Root<FindingAid> root, String code, String uuid) {        
        if (code==null && uuid==null) {
            // no filter
            return;
        }
        Predicate p = cb.conjunction();
        if (code!=null) {
            p = cb.and(p, cb.like(root.get("code"), "%"+code+"%"));
        }
        if (uuid!=null) {
            p = cb.and(p, cb.equal(root.get("uuid"), UUID.fromString(uuid)));
        }        
        cq.where(p);
    }      

}
