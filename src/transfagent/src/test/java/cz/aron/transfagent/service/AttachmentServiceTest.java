package cz.aron.transfagent.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ApuType;
import cz.aron.transfagent.AbstractCommonTest;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.service.AttachmentService.NameUUIDLastModifiedKey;
import cz.aron.transfagent.service.AttachmentSource.AttachmentDesc;

@SpringBootTest
public class AttachmentServiceTest extends AbstractCommonTest {

	@Autowired
	private AttachmentService attachmentService;
	
	@Test
	public void testUpdateAttachment() {
		
		// create ApuSource in db
		var apuSource = createApuSource();		
		tt.executeWithoutResult(t->{
			apuSourceRepository.save(apuSource);	
		});		

		// check attachments not exist
		var emptyAttachments = attachmentRepository.findByApuSource(apuSource);
		assertTrue(emptyAttachments.isEmpty());
			
		var apuSourceBuilder = new ApuSourceBuilder();
		apuSourceBuilder.createApu("main", ApuType.FINDING_AID);		
		
		var attachments = new ArrayList<AttachmentDesc>();		
		addAttachmentDesc(apuSourceBuilder, attachments, "attachment1", 1, UUID.randomUUID().toString());
		addAttachmentDesc(apuSourceBuilder, attachments, "attachment2", 2, UUID.randomUUID().toString());
		addAttachmentDesc(apuSourceBuilder, attachments, "attachment3", 3, UUID.randomUUID().toString());
		
		// store attachments
		boolean modified = tt.execute(t->{
			return attachmentService.updateAttachments(apuSource, apuSourceBuilder, attachments);	
		});
		assertTrue(modified);
		checkExistencyOfAttachments(apuSource, apuSourceBuilder, attachments);

		// update same files
		var newBuilder = new ApuSourceBuilder();
		newBuilder.createApu("main", ApuType.FINDING_AID);		
		var newAttachments = new ArrayList<AttachmentDesc>();
		addAttachmentDesc(newBuilder, newAttachments, attachments.get(0).getName(), 1, apuSourceBuilder.getMainApu().getAttchs().get(0).getFile().getUuid());
		addAttachmentDesc(newBuilder, newAttachments, attachments.get(1).getName(), 2, apuSourceBuilder.getMainApu().getAttchs().get(1).getFile().getUuid());
		addAttachmentDesc(newBuilder, newAttachments, attachments.get(2).getName(), 3, apuSourceBuilder.getMainApu().getAttchs().get(2).getFile().getUuid());
		// store attachments
		modified = tt.execute(t->{
			return attachmentService.updateAttachments(apuSource, newBuilder, newAttachments);	
		});
		assertFalse(modified);
		checkExistencyOfAttachments(apuSource, newBuilder, newAttachments);
		
		// preserve first attachment, remove second and third, add new one
		var newBuilder2 = new ApuSourceBuilder();
		newBuilder2.createApu("main", ApuType.FINDING_AID);		
		var newAttachments2 = new ArrayList<AttachmentDesc>();
		addAttachmentDesc(newBuilder2, newAttachments2, attachments.get(0).getName(), 1, apuSourceBuilder.getMainApu().getAttchs().get(0).getFile().getUuid());
		addAttachmentDesc(newBuilder2, newAttachments2, "attachment4", 1, UUID.randomUUID().toString());		
		// store attachments
		modified = tt.execute(t->{
			return attachmentService.updateAttachments(apuSource, newBuilder2, newAttachments2);	
		});
		assertTrue(modified);
		checkExistencyOfAttachments(apuSource, newBuilder2, newAttachments2);
	}
	
	private void checkExistencyOfAttachments(ApuSource apuSource, ApuSourceBuilder apuSourceBuilder,
			List<AttachmentDesc> attachments) {
		var faAttachments = attachmentRepository.findByApuSource(apuSource);
		assertTrue(faAttachments.size() == attachments.size());

		var existingAttachments = new ArrayListValuedHashMap<NameUUIDLastModifiedKey, Attachment>();
		for (var faAttachment : faAttachments) {
			existingAttachments.put(NameUUIDLastModifiedKey.of(faAttachment), faAttachment);
		}

		// check existency of all attachments
		var mainApu = apuSourceBuilder.getMainApu();
		var attIt = mainApu.getAttchs().iterator();
		var fileIt = attachments.iterator();
		while (attIt.hasNext() && fileIt.hasNext()) {
			var att = attIt.next();
			var attPath = fileIt.next();
			assertTrue(existingAttachments.containsKey(new NameUUIDLastModifiedKey(
					UUID.fromString(att.getFile().getUuid()), att.getName(), attPath.getLastModified())));
		}
	}
	
	private ApuSource createApuSource() {
		var apuSource = new ApuSource();
		apuSource.setDataDir("");
		apuSource.setDateImported(ZonedDateTime.now());
		apuSource.setDeleted(false);
		apuSource.setForce(false);
		apuSource.setDisableSend(false);
		apuSource.setLastSent(0l);
		apuSource.setNumAttachments(0);
		apuSource.setNumDaos(0);
		apuSource.setOrigDir("");
		apuSource.setReimport(false);
		apuSource.setSourceType(SourceType.FINDING_AID);
		apuSource.setUuid(UUID.randomUUID());				
		return apuSource;
	}
		
	private void addAttachmentDesc(ApuSourceBuilder apuSourceBuilder,List<AttachmentDesc> attachments, String name, long lastModified, String uuid) {
		var tmpPath = Paths.get("tmp",name);
		attachments.add(new AttachmentDesc(tmpPath, name, lastModified));
		apuSourceBuilder.addAttachment(apuSourceBuilder.getMainApu(), name, "application/pdf", 123456, uuid);
	}

}
