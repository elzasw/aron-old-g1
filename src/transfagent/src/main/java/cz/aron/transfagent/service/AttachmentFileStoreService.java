package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "attachmentstore.path")
public class AttachmentFileStoreService implements AttachmentSource {

    private final Path attachmentsDir;
    
    private final boolean ignoreImages = true;

    public AttachmentFileStoreService(@Value("${attachmentstore.path}") Path attachmentsDir) {
        this.attachmentsDir = attachmentsDir;
    }

    @Override
    public List<AttachmentDesc> getFundAttachments(String archiveCode, String fundCode, Integer subFundCode)
            throws IOException {
        Path attachmentDir = attachmentsDir.resolve(archiveCode).resolve("AS").resolve(fundCode);
        if (subFundCode != null) {
            attachmentDir = attachmentDir.resolve("" + subFundCode);
        }
        return readAttachments(attachmentDir);
    }

	@Override
	public List<AttachmentDesc> getFindingAidAttachments(String archiveCode, String findingAidCode,
			String findingAidPartNumber) throws IOException {
		Path attachmentDir = attachmentsDir.resolve(archiveCode).resolve("AP").resolve(findingAidCode);
		return readAttachments(attachmentDir);
	}

    private List<AttachmentDesc> readAttachments(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            return Collections.emptyList();
        }
        try (var stream = Files.list(path)) {
            return stream.filter(p -> !Files.isDirectory(p) && (!ignoreImages || !DaoFileStore3Service.isImage(p))).map(
                                                                                                                        p -> new AttachmentDesc(
                                                                                                                                p,
                                                                                                                                p.getFileName()
                                                                                                                                        .toString()))
                    .collect(Collectors.toList());
        }
    }

}
