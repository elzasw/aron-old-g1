package cz.aron.customizations.soaplzen;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.service.AttachmentSource;

@ConditionalOnProperty(value = "filestore5.path")
@Service
public class FileStore5AttachmentSource implements AttachmentSource {

	private static final Logger log = LoggerFactory.getLogger(FileStore5AttachmentSource.class);		

	private final Path path;
	
	private final ConfigDaoFileStore5 config;
	
	private Map<InstitutionFindingAidKey, List<AttachmentDesc>> faAttachments = new HashMap<>();
	
	private Map<InstitutionFundKey, List<AttachmentDesc>> fundAttachments = new HashMap<>();
	
	private long lastAttachmentReload = 0;
	
	public FileStore5AttachmentSource(ConfigDaoFileStore5 config) {
		this.path = config.getPath();
		this.config = config;		
		log.info("FileStore5AttachmentSource started, path {}", path);
	}

	
	@Override
	public List<AttachmentDesc> getFundAttachments(String archiveCode, String fundCode, Integer subFundCode)
			throws IOException {
		var ifKey = new InstitutionFundKey(archiveCode, fundCode, subFundCode);
		var attachments = getAttachments(false).getFundAttachments();
		var att = attachments.get(ifKey);
		if (att == null) {
			return Collections.emptyList();
		} else {
			return att;
		}
	}


	@Override
	public List<AttachmentDesc> getFindingAidAttachments(String archiveCode, String findingAidCode,
			String findingAidPartNumber) throws IOException {
		var ifKey = new InstitutionFindingAidKey(archiveCode, Integer.parseInt(findingAidCode), findingAidPartNumber);
		var attachments = getAttachments(false).getFaAttachments();						
		var att = attachments.get(ifKey);
		if (att == null) {
			return Collections.emptyList();
		} else {
			if (findingAidPartNumber != null) {
				// pridame prilohu celeho inventare
				var ifRootKey = new InstitutionFindingAidKey(archiveCode, Integer.parseInt(findingAidCode), null);
				var rootAtt = attachments.get(ifRootKey);
				if (rootAtt != null) {
					var tmp = new ArrayList<AttachmentDesc>(att);
					tmp.addAll(rootAtt);
					return tmp;
				}
			}						
			return att;			
		}				
	}


	protected synchronized Keys getAttachments(boolean forceReload) {		
		var currentTime = System.currentTimeMillis();		
		if ( currentTime - lastAttachmentReload > 5*60*1000 || forceReload) { // znovu nactu po peti minutach
			try {
				readAttachments();
				lastAttachmentReload = currentTime;
			} catch (IOException e) {
				log.error("Fail to read attachments", e);
			}			
		}
		return new Keys(faAttachments, fundAttachments);				
	}
	
	
	private void readAttachments() throws IOException {
		var attPath = path.resolve("ap").resolve("pdf");		
		// check existency of directory
		if (!Files.isDirectory(attPath)) {
			log.error("Directory not exist {}", attPath);
			throw new IOException("Directory not exist " + attPath);
		}
		
		var newFaAttachments = new HashMap<InstitutionFindingAidKey, List<AttachmentDesc>>();
		var newFundAttachments = new HashMap<InstitutionFundKey, List<AttachmentDesc>>();
		read(attPath, newFaAttachments, newFundAttachments);
		faAttachments = newFaAttachments;
		fundAttachments = newFundAttachments;
		log.info("Attachments metadata reloaded from path {}", attPath);
	}

	private void read(Path path, Map<InstitutionFindingAidKey, List<AttachmentDesc>> newFaAttachments,
			Map<InstitutionFundKey, List<AttachmentDesc>> newFundAttachments) throws IOException {
		try (var stream = Files.find(path, Integer.MAX_VALUE, (p, a) -> {
			if (Files.isRegularFile(p)) {
				processFile(p, a, newFaAttachments, newFundAttachments);				
			}
			return false;
		}, FileVisitOption.FOLLOW_LINKS)) {
			stream.count();
		}
	}
	
	
	private void processFile(Path file, BasicFileAttributes attrs, Map<InstitutionFindingAidKey, List<AttachmentDesc>> newFaAttachments,
			Map<InstitutionFundKey, List<AttachmentDesc>> newFundAttachments) {
		var fileName = file.getFileName().toString();
		var splitted = fileName.split("_");
		if (splitted.length < 3) {
			return;
		}		
		String archiveCode = config.getInstitutionCodeByShortcut(splitted[0]);
		if (archiveCode == null) {
			return;
		}
		int apCode = -1;
		String partNumber = null;		
		var splittedApCode = splitted[1].split("\\.");
		if (splittedApCode.length == 1) {
			if (splittedApCode[0].startsWith("ap")) {
				try {
					apCode = Integer.parseInt(splittedApCode[0].substring(2));
				} catch (NumberFormatException e) {
					return;
				}
			} else {
				return;
			}
		} else if (splittedApCode.length == 2) {
			if (splittedApCode[0].startsWith("ap")) {
				try {
					apCode = Integer.parseInt(splittedApCode[0].substring(2));
					partNumber = "" + Integer.parseInt(splittedApCode[1]);
				} catch (NumberFormatException e) {
					return;
				}				
			} else {
				return;
			}
		} else {
			return;
		}
		
		if (apCode == 0) {
			// prilohu pridam k fondu
			try {				
				newFundAttachments.compute(new InstitutionFundKey(archiveCode,""+Integer.parseInt(splitted[2]), null) , (k, v) -> {
					if (v == null) {
						v = new ArrayList<AttachmentDesc>();
					}
					v.add(new AttachmentDesc(file, file.getFileName().toString(), attrs.lastModifiedTime().toMillis()));
					return v;
				});
			} catch (NumberFormatException e) {
				// ignore invalid number
				return;
			}					
		} else {
			// prilohu pridam k pomucce
			newFaAttachments.compute(new InstitutionFindingAidKey(archiveCode, apCode, partNumber), (k, v) -> {
				if (v == null) {
					v = new ArrayList<AttachmentDesc>();
				}
				v.add(new AttachmentDesc(file, file.getFileName().toString(), attrs.lastModifiedTime().toMillis()));
				return v;
			});
		}
		
	}

	public static class Keys {
		
		private final Map<InstitutionFindingAidKey, List<AttachmentDesc>> faAttachments;
		
		private final Map<InstitutionFundKey, List<AttachmentDesc>> fundAttachments;

		public Keys(Map<InstitutionFindingAidKey, List<AttachmentDesc>> faAttachments,
				Map<InstitutionFundKey, List<AttachmentDesc>> fundAttachments) {
			this.faAttachments = faAttachments;
			this.fundAttachments = fundAttachments;
		}

		public Map<InstitutionFindingAidKey, List<AttachmentDesc>> getFaAttachments() {
			return faAttachments;
		}

		public Map<InstitutionFundKey, List<AttachmentDesc>> getFundAttachments() {
			return fundAttachments;
		}

	}
	
	public static class InstitutionFundKey {
		
		private final String institutionCode;
		
		private final String fundCode;
		
		private final Integer subFundCode;

		public InstitutionFundKey(String institutionCode, String fundCode, Integer subFundCode) {
			this.institutionCode = institutionCode;
			this.fundCode = fundCode;
			this.subFundCode = subFundCode;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fundCode, institutionCode, subFundCode);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InstitutionFundKey other = (InstitutionFundKey) obj;
			return Objects.equals(fundCode, other.fundCode) && Objects.equals(institutionCode, other.institutionCode)
					&& Objects.equals(subFundCode, other.subFundCode);
		}

		public String getInstitutionCode() {
			return institutionCode;
		}

		public String getFundCode() {
			return fundCode;
		}

		public Integer getSubFundCode() {
			return subFundCode;
		}
		
	}


	public static class InstitutionFindingAidKey {

		private final String institutionCode;

		private final int findingAidCode;

		private final String findingAidPartNumber;

		public InstitutionFindingAidKey(String institutionCode, int findingAidCode, String findingAidPartNumber) {
			this.institutionCode = institutionCode;
			this.findingAidCode = findingAidCode;
			this.findingAidPartNumber = findingAidPartNumber;
		}

		public String getInstitutionCode() {
			return institutionCode;
		}

		public int getFindingAidCode() {
			return findingAidCode;
		}

		public String getFindingAidPartNumber() {
			return findingAidPartNumber;
		}

		public String getHandle() {
			if (findingAidPartNumber != null) {
				return "ap/" + institutionCode + "/" + findingAidCode + "/" + findingAidPartNumber;
			} else {
				return "ap/" + institutionCode + "/" + findingAidCode;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(findingAidCode, findingAidPartNumber, institutionCode);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InstitutionFindingAidKey other = (InstitutionFindingAidKey) obj;
			return findingAidCode == other.findingAidCode
					&& Objects.equals(findingAidPartNumber, other.findingAidPartNumber)
					&& Objects.equals(institutionCode, other.institutionCode);
		}

	}


}
