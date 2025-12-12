package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public interface AttachmentSource {
    
    public List<AttachmentDesc> getFundAttachments(String archiveCode, String fundCode, Integer subFundCode) throws IOException;

    public List<AttachmentDesc> getFindingAidAttachments(String archiveCode, String findingAidCode, String findingAidPartNumber) throws IOException;
    
    public static class AttachmentDesc {

        private final Path path;

        private final String name;
        
        private final long lastModified;

        public AttachmentDesc(Path path, String name, long lastModified) {
            this.path = path;
            this.name = name;
            this.lastModified = lastModified;
        }

        public Path getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

		public long getLastModified() {
			return lastModified;
		}

		@Override
		public int hashCode() {
			return Objects.hash(lastModified, name, path);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AttachmentDesc other = (AttachmentDesc) obj;
			return lastModified == other.lastModified && Objects.equals(name, other.name)
					&& Objects.equals(path, other.path);
		}        
		
    }

}
