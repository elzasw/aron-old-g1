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
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.repository.AttachmentRepository;

@Service
public class AttachmentService {
	
	private final AttachmentRepository attachmentRepository;
	
	private final StorageService storageService;

	public AttachmentService(AttachmentRepository attachmentRepository, StorageService storageService) {
		this.attachmentRepository = attachmentRepository;
		this.storageService = storageService;
	}

	@Transactional
	public void updateAttachments(ApuSource apuSource, ApuSourceBuilder builder, List<Path> attachments) {
		var dbAttachments = attachmentRepository.findByApuSource(apuSource);
		if (dbAttachments.size() > 0) {
			// drop old attachment
			attachmentRepository.deleteInBatch(dbAttachments);
		}

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

				Validate.notNull(att.getFile(), "DaoFile is null");
				Validate.notNull(att.getFile().getUuid(), "DaoFile without UUID");

				createAttachment(apuSource, attPath.getFileName().toString(),
						UUID.fromString(att.getFile().getUuid()));
			}
		}
	}
	
	private void createAttachment(ApuSource apuSource, String fileName, UUID uuid) {
		var attachment = new Attachment();
		attachment.setApuSource(apuSource);
		attachment.setFileName(fileName);
		attachment.setUuid(uuid);
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
	public List<Path> copyAttachmentsFromApus(ApuSource targetApuSource, Collection<ApuSource> srcApuSources)
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
	public List<Path> copyAttachmentsFromApus(Path targetDir, Collection<ApuSource> srcApuSources) throws IOException {
		var ret = new ArrayList<Path>();
		for (var srcApuSource : srcApuSources) {
			var srcDir = storageService.getApuDataDir(srcApuSource.getDataDir());
			for (var attachment : attachmentRepository.findByApuSource(srcApuSource)) {
				var attachmentFile = srcDir.resolve(attachment.getFileName());
				var targetFile = targetDir.resolve(attachment.getFileName());
				Files.copy(attachmentFile, targetFile,
						StandardCopyOption.REPLACE_EXISTING);
				ret.add(targetFile);
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
	
	
}
