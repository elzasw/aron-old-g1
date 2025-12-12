package cz.aron.customizations.soaplzen;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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

import cz.aron.customizations.soaplzen.DaoFileStore5Service.InstitutionFindingAidKey;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.service.AttachmentSource.AttachmentDesc;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.InstitutionsService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

/**
 * Class for detecting changes in attachments
 */

@Service
@ConditionalOnProperty(name = "filestore5.autoImportAttachments", havingValue="true")
public class AttachmentImport implements ImportProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(AttachmentImport.class);
	
	private final FileImportService importService;
	
	private final DaoFileStore5Service daoFileStore5Service;
	
	private final InstitutionsService institutionsService;
	
	private final ConfigDaoFileStore5 config;
	
	private final FindingAidService findingAidService;
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final TransactionTemplate tt; 
	
	private OffsetDateTime nextRun = null;
			
	public AttachmentImport(FileImportService importService, DaoFileStore5Service daoFileStore5Service,
			InstitutionsService institutionsService, ConfigDaoFileStore5 config, FindingAidService findingAidService,
			ApuSourceRepository apuSourceRepository, TransactionTemplate tt) {
		this.importService = importService;
		this.daoFileStore5Service = daoFileStore5Service;
		this.institutionsService = institutionsService;
		this.config = config;
		this.findingAidService = findingAidService;
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

		var toReimport = new ArrayList<Integer>();
		var instCodes = config.getInstitutions().stream().map(i -> i.getInstitutionCode()).collect(Collectors.toSet());
		var institutions = institutionsService.getInstitutionsByCodes(instCodes);
		var codeToId = institutions.stream().collect(Collectors.toMap((v) -> v.getCode(), (v) -> v.getId()));

		var attachments = daoFileStore5Service.getAttachments(true);
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
			var faAttachments = faAttachmentsRet.getAttachments();

			var sourceAttachments = new ArrayListValuedHashMap<NameLastModifiedKey, Integer>();
			attachmentDescs.forEach(a->{
				sourceAttachments.put(NameLastModifiedKey.of(a), 1);
			});
			
			if (faPartNumber != null) {
				// pridame prilohu celeho inventare
				var ifRootKey = new InstitutionFindingAidKey(instCode, faCode, null);
				var rootAttDesc = attachments.get(ifRootKey);
				if (rootAttDesc != null) {
					rootAttDesc.forEach(a->{
						sourceAttachments.put(NameLastModifiedKey.of(a), 1);
					});				
				}
			}

			for (var faAttachment : faAttachments) {
				var faKey = NameLastModifiedKey.of(faAttachment);
				var x = sourceAttachments.get(faKey);
				if (x.isEmpty()) {
					// attachment not present, deleted or modified attachment
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
			if (modified||!sourceAttachments.isEmpty()) {
				toReimport.add(faAttachmentsRet.getFindingAid().getApuSource().getId());
			}
		}		

		if (toReimport.isEmpty()) {
			log.info("Attachment changes check finished. No new or modified attachments");
		} else {
			tt.executeWithoutResult(t->{			
	        	ListUtils.partition(toReimport, 1000).forEach(ids->{
	        		apuSourceRepository.setReimportTrueByIds(ids);	
	        	});
			});
			log.info("Attachment changes check finished. Finding aid apu sources marked to be reimported, apu source ids={}", toReimport);	
		}

		nextRun = nowTime.plusSeconds(3600);
	}

	@Override
	public int getPriority() {
		// low priority
		return -1;
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
