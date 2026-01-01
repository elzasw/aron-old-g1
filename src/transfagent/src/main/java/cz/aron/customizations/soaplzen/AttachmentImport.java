package cz.aron.customizations.soaplzen;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.customizations.soaplzen.FileStore5AttachmentSource.InstitutionFindingAidKey;
import cz.aron.customizations.soaplzen.FileStore5AttachmentSource.InstitutionFundKey;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.service.AttachmentSource.AttachmentDesc;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.FundService;
import cz.aron.transfagent.service.InstitutionsService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

/**
 * Class for detecting changes in fund and finding aid attachments. Schedule
 * reimport on ApuSource where some change is detected.
 * 
 *  Detected changes 
 *   - add
 *   - modification
 *   - delete
 */
@Service
@ConditionalOnProperty(name = "filestore5.autoImportAttachments", havingValue = "true")
public class AttachmentImport implements ImportProcessor {

	private static final Logger log = LoggerFactory.getLogger(AttachmentImport.class);

	private final FileImportService importService;

	private final FileStore5AttachmentSource attachmentSource;

	private final InstitutionsService institutionsService;

	private final ConfigDaoFileStore5 config;

	private final FindingAidService findingAidService;

	private final FundService fundService;

	private final ApuSourceRepository apuSourceRepository;

	private final TransactionTemplate tt;

	private OffsetDateTime nextRun = null;

	public AttachmentImport(FileImportService importService, FileStore5AttachmentSource attachmentSource,
			InstitutionsService institutionsService, ConfigDaoFileStore5 config, FindingAidService findingAidService,
			FundService fundService, ApuSourceRepository apuSourceRepository, TransactionTemplate tt) {
		this.importService = importService;
		this.attachmentSource = attachmentSource;
		this.institutionsService = institutionsService;
		this.config = config;
		this.findingAidService = findingAidService;
		this.fundService = fundService;
		this.apuSourceRepository = apuSourceRepository;
		this.tt = tt;
	}

	@PostConstruct
	void init() {
		importService.registerImportProcessor(this);
	}

	@Override
	public void importData(ImportContext ic) {
		OffsetDateTime nowTime = OffsetDateTime.now();
		if (nextRun != null && nextRun.isAfter(OffsetDateTime.now())) {
			// TODO nejake lepsi planovani
			return;
		}

		var instCodes = config.getInstitutions().stream().map(i -> i.getInstitutionCode()).collect(Collectors.toSet());
		var institutions = institutionsService.getInstitutionsByCodes(instCodes);
		var codeToId = institutions.stream().collect(Collectors.toMap((v) -> v.getCode(), (v) -> v.getId()));

		var keys = attachmentSource.getAttachments(true);
				
		var start = Instant.now();
		var numReimports = importFindingAidAttachments(codeToId, keys.getFaAttachments());
		log.info("Finding aid attachment changes check finished. Num reimported: {}, time: {}", numReimports, Duration.between(start, Instant.now()).toSeconds());
		
		start = Instant.now();
		numReimports = importFundAttachments(codeToId, keys.getFundAttachments());
		log.info("Fund attachment changes check finished. Num reimported: {}, time: {}", numReimports, Duration.between(start, Instant.now()).toSeconds());

		nextRun = nowTime.plusSeconds(3600);
	}

	@Override
	public int getPriority() {
		// low priority
		return -1;
	}

	private int importFindingAidAttachments(Map<String, Integer> codeToId,
			Map<InstitutionFindingAidKey, List<AttachmentDesc>> attachments) {

		var toReimport = new HashSet<Integer>();
		var processed = new HashSet<Integer>();

		// compare attachments from attachment source with existing in database 
		for (var entry : attachments.entrySet()) {
			var key = entry.getKey();
			var attachmentDescs = entry.getValue();

			var instCode = key.getInstitutionCode();
			var faCode = key.getFindingAidCode();
			var faPartNumberStr = key.getFindingAidPartNumber();
			Integer faPartNumber = null;
			if (faPartNumberStr != null) {
				try {
					faPartNumber = Integer.parseInt(faPartNumberStr);
				} catch (NumberFormatException e) {
					continue;
				}
			}
			var instId = codeToId.get(instCode);

			var modified = false;
			var faAttachmentsRet = findingAidService.getFindingAidAttachments(instId, faCode, faPartNumber);
			if (faAttachmentsRet.getFindingAid() == null) {
				// finding aid not found
				continue;
			}

			processed.add(faAttachmentsRet.getFindingAid().getApuSource().getId());

			var faAttachments = faAttachmentsRet.getAttachments();

			var sourceAttachments = new ArrayListValuedHashMap<NameLastModifiedKey, Integer>();
			attachmentDescs.forEach(a -> {
				sourceAttachments.put(NameLastModifiedKey.of(a), 1);
			});

			if (faPartNumber != null) {
				// pridame prilohu celeho inventare
				var ifRootKey = new InstitutionFindingAidKey(instCode, faCode, null);
				var rootAttDesc = attachments.get(ifRootKey);
				if (rootAttDesc != null) {
					rootAttDesc.forEach(a -> {
						sourceAttachments.put(NameLastModifiedKey.of(a), 1);
					});
				}
			}

			for (var faAttachment : faAttachments) {
				var faKey = NameLastModifiedKey.of(faAttachment);
				var x = sourceAttachments.get(faKey);
				if (x.isEmpty()) {
					// attachment not present in source, deleted or modified attachment
					modified = true;
				} else {
					// attachment exist
					x.remove(x.size() - 1);
					if (x.isEmpty()) {
						sourceAttachments.remove(NameLastModifiedKey.of(faAttachment));
					}
				}
			}

			// modification already detected or some new or modified attachment
			if (modified || !sourceAttachments.isEmpty()) {
				toReimport.add(faAttachmentsRet.getFindingAid().getApuSource().getId());
			}
		}

		// reimport apu sources containing attachments not processed by previous step (all attachments deleted in source)
		tt.executeWithoutResult(t -> {
			try (var stream = apuSourceRepository.findIdsByFindingAidAndAttachmentAndInstitution(codeToId.values())) {
				stream.forEach(id -> {
					if (!processed.contains(id)) {
						toReimport.add(id);
					}
				});
			}
		});

		if (!toReimport.isEmpty()) {
			tt.executeWithoutResult(t -> {
				var toReimportList = new ArrayList<Integer>(toReimport);
				ListUtils.partition(toReimportList, 1000).forEach(ids -> {
					apuSourceRepository.setReimportTrueByIds(ids);
				});
			});
		}		
		return toReimport.size();
	}

	private int importFundAttachments(Map<String, Integer> codeToId,
			Map<InstitutionFundKey, List<AttachmentDesc>> attachments) {

		var toReimport = new HashSet<Integer>();
		var processed = new HashSet<Integer>();

		// compare attachments from attachment source with existing in database
		for (var entry : attachments.entrySet()) {
			var key = entry.getKey();
			var attachmentDescs = entry.getValue();

			var instCode = key.getInstitutionCode();
			var fundCodeStr = key.getFundCode();
			var fundSubCode = key.getSubFundCode();
			var instId = codeToId.get(instCode);
			var fundCode = Integer.parseInt(fundCodeStr);

			var modified = false;
			var fundAttachmentsRet = fundService.getFundAttachments(instId, fundCode, fundSubCode);
			if (fundAttachmentsRet.getFund() == null) {
				// fund not found
				continue;
			}

			processed.add(fundAttachmentsRet.getFund().getApuSource().getId());

			var fundAttachments = fundAttachmentsRet.getAttachments();

			var sourceAttachments = new ArrayListValuedHashMap<NameLastModifiedKey, Integer>();
			attachmentDescs.forEach(a -> {
				sourceAttachments.put(NameLastModifiedKey.of(a), 1);
			});

			for (var fundAttachment : fundAttachments) {
				var faKey = NameLastModifiedKey.of(fundAttachment);
				var x = sourceAttachments.get(faKey);
				if (x.isEmpty()) {
					// attachment not present, deleted or modified attachment
					modified = true;
				} else {
					// attachment exist
					x.remove(x.size() - 1);
					if (x.isEmpty()) {
						sourceAttachments.remove(NameLastModifiedKey.of(fundAttachment));
					}
				}
			}

			// modification already detected or some new or modified attachment
			if (modified || !sourceAttachments.isEmpty()) {
				toReimport.add(fundAttachmentsRet.getFund().getApuSource().getId());
			}
		}

		// reimport apu sources containing attachments not processed by previous step (all attachments deleted in source)
		tt.executeWithoutResult(t -> {
			try (var stream = apuSourceRepository.findIdsByFundAndAttachmentAndInstitution(codeToId.values())) {
				stream.forEach(id -> {
					if (!processed.contains(id)) {
						toReimport.add(id);
					}
				});
			}
		});

		if (!toReimport.isEmpty()) {
			var toReimportList = new ArrayList<Integer>(toReimport);
			tt.executeWithoutResult(t -> {
				ListUtils.partition(toReimportList, 1000).forEach(ids -> {
					apuSourceRepository.setReimportTrueByIds(ids);
				});
			});
		}		
		return toReimport.size();
	} 

	private static class NameLastModifiedKey {

		private final String name;

		private final long lastModified;

		public NameLastModifiedKey(String name, long lastModified) {
			this.name = name;
			this.lastModified = lastModified;
		}

		public static NameLastModifiedKey of(AttachmentDesc attachment) {
			return new NameLastModifiedKey(attachment.getName(), attachment.getLastModified());
		}

		public static NameLastModifiedKey of(Attachment attachment) {
			return new NameLastModifiedKey(attachment.getFileName(), attachment.getLastModified());
		}

		@Override
		public int hashCode() {
			return Objects.hash(lastModified, name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NameLastModifiedKey other = (NameLastModifiedKey) obj;
			return lastModified == other.lastModified && Objects.equals(name, other.name);
		}

	}

}
