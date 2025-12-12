package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.service.AttachmentSource.AttachmentDesc;

@Service
public class AttachmentService {
	
	private final AttachmentRepository attachmentRepository;
	
	private final StorageService storageService;

	public AttachmentService(AttachmentRepository attachmentRepository, StorageService storageService) {
		this.attachmentRepository = attachmentRepository;
		this.storageService = storageService;
	}
	
	/**
	 * Update attachment records related to apuSource
	 * @param apuSource attachment of this apuSource will be updated
	 * @param builder apu source builder, get UUIDs of attachments
	 * @param attachments list of attachments
	 * @return true - updated, false - not updated
	 * 
	 * //TODO rozsirit AttachmentDesc o UUID a nemit jako parametr builder 
	 */
	@Transactional
	public boolean updateAttachments(ApuSource apuSource, ApuSourceBuilder builder, List<AttachmentDesc> attachments) {
		
		boolean updated = false;
		
		var dbAttachments = attachmentRepository.findByApuSource(apuSource);
		var existingAttachments = new ArrayListValuedHashMap<NameUUIDLastModifiedKey, Attachment>();
		dbAttachments
				.forEach(attachment -> existingAttachments.put(NameUUIDLastModifiedKey.of(attachment), attachment));

		if (CollectionUtils.isNotEmpty(attachments)) {
			var mainApu = builder.getMainApu();
			Validate.isTrue(attachments.size() == mainApu.getAttchs().size(),
					"Attachment size does not match, attachments: %i, xml: %i", attachments.size(),
					mainApu.getAttchs().size());

			var attIt = mainApu.getAttchs().iterator();
			var fileIt = attachments.iterator();
			while (attIt.hasNext() && fileIt.hasNext()) {
				var att = attIt.next();
				var attPath = fileIt.next();

				Validate.notNull(att.getFile(), "Attachment file is null");
				Validate.notNull(att.getFile().getUuid(), "Attachment file without UUID");

				var key = new NameUUIDLastModifiedKey(UUID.fromString(att.getFile().getUuid()),
						attPath.getName(), attPath.getLastModified());
				var existing = existingAttachments.get(key);
				if (existing.isEmpty()) {
					// attachment record doesn't exist, create new
					updated = true;
					createAttachment(apuSource, attPath.getName(), UUID.fromString(att.getFile().getUuid()),
							attPath.getLastModified());
				} else {
					// attachment record exist, remove from existingAttachment map
					existing.remove(existing.size() - 1);
					if (existing.isEmpty()) {
						existingAttachments.remove(key);
					}					
				}
			}
		}
		
		if (existingAttachments.size() > 0) {
			// drop old attachment
			updated = true;
			var toBeDeleted = existingAttachments.values();
			attachmentRepository.deleteInBatch(toBeDeleted);
		}
		
		return updated;
	}
	
	private void createAttachment(ApuSource apuSource, String fileName, UUID uuid, long lastModified) {
		var attachment = new Attachment();
		attachment.setApuSource(apuSource);
		attachment.setFileName(fileName);
		attachment.setUuid(uuid);
		attachment.setLastModified(lastModified);
		attachment = attachmentRepository.save(attachment);
	}
	
	/**
	 * Zkopiruje prilohy z existujicich ApuSource do ciloveho 
	 * @param targetApuSource apuSource, do ktereho se kopiruji prilohy
	 * @param srcApuSources apuSource, ze kterych se kopiruji prilohy 
	 * @return List<Path> plne cesty k nakopirovanym priloham ulozenym v targetApuSource 
	 * @throws IOException
	 */
	@Transactional
	public List<AttachmentDesc> copyAttachmentsFromApus(ApuSource targetApuSource, Collection<ApuSource> srcApuSources)
			throws IOException {
		return copyAttachmentsFromApus(storageService.getApuDataDir(targetApuSource.getDataDir()), srcApuSources);
	}

	/**
	 * Zkopiruje prilohy z existujicich ApuSource do adresare 
	 * @param targetDir adresar, kam jsou prilohy kopirovany
	 * @param srcApuSources apuSource, ze kterych se kopiruji prilohy
	 * @return ist<Path> plne cesty k nakopirovanym priloham ulozenym v cilovem adresari
	 * @throws IOException
	 */
	@Transactional
	public List<AttachmentDesc> copyAttachmentsFromApus(Path targetDir, Collection<ApuSource> srcApuSources)
			throws IOException {
		var ret = new ArrayList<AttachmentDesc>();
		for (var srcApuSource : srcApuSources) {
			var srcDir = storageService.getApuDataDir(srcApuSource.getDataDir());
			for (var attachment : attachmentRepository.findByApuSource(srcApuSource)) {
				var attachmentFile = srcDir.resolve(attachment.getFileName());
				var targetFile = targetDir.resolve(attachment.getFileName());
				Files.copy(attachmentFile, targetFile, StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
				ret.add(new AttachmentDesc(targetFile, targetFile.getFileName().toString(),
						Files.getLastModifiedTime(targetFile).toMillis()));
			}
		}
		return ret;
	}
	
	/**
	 * Get mapping filename->uuid for given apuSource
	 * @param apuSource to get mapping for
	 * @return {@code Map<String, String>}
	 */
	@Transactional
	public Map<String, String> getNameUuids(ApuSource apuSource) {
		var dbAttachments = attachmentRepository.findByApuSource(apuSource);
		var ret = new HashMap<String, String>();
		for(var attachment:dbAttachments) {
			ret.put(attachment.getFileName(), attachment.getUuid().toString());
		}
		return ret;
	}
	
	public static class NameUUIDLastModifiedKey {
		
		private final UUID uuid;
		
		private final String name;
		
		private final long lastModified;

		public NameUUIDLastModifiedKey(UUID uuid, String name, long lastModified) {
			this.uuid = uuid;
			this.name = name;
			this.lastModified = lastModified;
		}

		@Override
		public int hashCode() {
			return Objects.hash(lastModified, name, uuid);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NameUUIDLastModifiedKey other = (NameUUIDLastModifiedKey) obj;
			return lastModified == other.lastModified && Objects.equals(name, other.name)
					&& Objects.equals(uuid, other.uuid);
		}
		
		public static NameUUIDLastModifiedKey of(Attachment attachment) {
			return new NameUUIDLastModifiedKey(attachment.getUuid(), attachment.getFileName(), attachment.getLastModified());
		}

	}

}
