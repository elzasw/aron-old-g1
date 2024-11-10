package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface AttachmentSource {
    
    public List<AttachmentDesc> getFundAttachments(String archiveCode, String fundCode, Integer subFundCode) throws IOException;

    public List<AttachmentDesc> getFindingAidAttachments(String archiveCode, String findingAidCode, String findingAidPartNumber) throws IOException;
    
    public static class AttachmentDesc {

        private final Path path;

        private final String name;

        public AttachmentDesc(Path path, String name) {
            super();
            this.path = path;
            this.name = name;
        }

        public Path getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

    }

}
