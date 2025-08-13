package cz.aron.customizations.soaplzen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.common.MonitoredFileResource;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.peva.PermalinkGenerator;
import cz.aron.transfagent.service.AttachmentSource;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.TransformService;
import cz.aron.transfagent.service.importfromdir.TransformService.TileThumbnailUrl;

@ConditionalOnProperty(value = "filestore5.path")
@Service
public class DaoFileStore5Service implements DaoImporter, AttachmentSource, PermalinkGenerator {

	private static final Logger log = LoggerFactory.getLogger(DaoFileStore5Service.class);

	@SuppressWarnings("serial")
	private final Map<InstitutionFindingAidKey, FindingAidDaoLinks> links = new LinkedHashMap<>(16, (float) 0.75,
			true) {

		@Override
		protected boolean removeEldestEntry(Entry<InstitutionFindingAidKey, FindingAidDaoLinks> eldest) {
			return this.size() > 20;
		}

	};
	
	private Map<InstitutionFindingAidKey, List<AttachmentDesc>> attachments = new HashMap<>();
	
	private long lastAttachmentReload = 0;

	private final Path path;

	private final TransformService transformService;

	private final ConfigDaoFileStore5 config;

	public DaoFileStore5Service(ConfigDaoFileStore5 config, TransformService transformService) {
		this.path = config.getPath();
		this.transformService = transformService;
		this.config = config;		
		log.info("DaoFileStore5Service started, path {}", path);
	}

	@Override
	public String getName() {
		return "file5";
	}

	@Override
	public void importDaoFile(Dao dao, Path daoDir) {
		FileStore5Handle handle = FileStore5Handle.fromHandle(dao.getHandle());
		
		var ifKey = handle.getIfKey();
		var inst = config.getInstitutionByCode(ifKey.getInstitutionCode());
		int findingAidPartNumber = 0;
		if (ifKey.getFindingAidPartNumber() != null) {
			findingAidPartNumber = Integer.parseInt(ifKey.getFindingAidPartNumber());
		}		
		var permaLink = String.format("/ippimage/%s%04d%03d", inst.getNid(), ifKey.getFindingAidCode(), findingAidPartNumber);		
		List<ImageUrl> daoLinks = getLinks(handle.getIfKey());
		if (daoLinks != null) {
			var links = daoLinks.stream().map(imageUrl -> new TileThumbnailUrl(imageUrl.getUrl() + ".dzi",
					imageUrl.getUrl(), imageUrl.isSelected(), imageUrl.isSelected()?permaLink:null )).collect(Collectors.toList());
			try {
				transformService.transformHttpUrlsWithThumbnails(daoDir, links);
				dao.setState(DaoState.READY);
			} catch (Exception e) {
				log.error("Fail to import dao id={}, handle={}, uuid={}", dao.getId(), dao.getHandle(), dao.getUuid());
				throw new RuntimeException();
			}
		} else {
			dao.setState(DaoState.INACCESSIBLE);
		}
	}

	@Override
	public List<AttachmentDesc> getFundAttachments(String archiveCode, String fundCode, Integer subFundCode)
			throws IOException {		
		return Collections.emptyList();
	}

	@Override
	public List<AttachmentDesc> getFindingAidAttachments(String archiveCode, String findingAidCode,
			String findingAidPartNumber) throws IOException {
		
		InstitutionFindingAidKey ifKey = new InstitutionFindingAidKey(archiveCode, Integer.parseInt(findingAidCode), findingAidPartNumber);
		var attachments = getAttachments();						
		var att = attachments.get(ifKey);
		if (att == null) {
			return Collections.emptyList();
		} else {
			return att;			
		}				
	}

	private synchronized Map<InstitutionFindingAidKey, List<AttachmentDesc>> getAttachments() {		
		var currentTime = System.currentTimeMillis();		
		if ( currentTime - lastAttachmentReload > 5*60*1000) { // znovu nactu po peti minutach
			try {
				attachments = readAttachments();
				lastAttachmentReload = currentTime;
			} catch (IOException e) {
				log.error("Fail to read attachments", e);
			}
		}
		return attachments;				
	}

	private Map<InstitutionFindingAidKey, List<AttachmentDesc>> readAttachments() throws IOException {
		var attPath = path.resolve("ap").resolve("pdf");
		Map<InstitutionFindingAidKey, List<AttachmentDesc>> tmpAttachments = new HashMap<>();
		read(attPath, tmpAttachments);
		return tmpAttachments;
	}
	
	private void read(Path path, Map<InstitutionFindingAidKey, List<AttachmentDesc>> tmpAttachments) throws IOException {
		if (Files.isRegularFile(path)) {
			var key = fromFile(path);
			if (key != null) {
				tmpAttachments.compute(key, (k, v) -> {
					if (v == null) {
						v = new ArrayList<AttachmentDesc>();
					}
					v.add(new AttachmentDesc(path, path.getFileName().toString()));
					return v;
				});
			}
		} else if (Files.isDirectory(path)) {
			try (var stream = Files.list(path)) {
				var itDir = stream.iterator();
				while(itDir.hasNext()) {
					Path dir = itDir.next();
					read(dir, tmpAttachments);
				}				
			}
		}
	}

	private InstitutionFindingAidKey fromFile(Path file) {		
		var fileName = file.getFileName().toString();
		var splitted = fileName.split("_");
		if (splitted.length < 3) {
			return null;
		}
		
		String archiveCode = null;
		for (var inst : config.getInstitutions()) {
			if (Objects.equals(splitted[0], inst.getShortcut())) {
				archiveCode = inst.getInstitutionCode();
				break;
			}
		}
		if (archiveCode == null) {
			return null;
		}
		int apCode = -1;
		String partNumber = null;
		var splittedApCode = splitted[1].split("\\.");
		if (splittedApCode.length == 1) {
			if (splittedApCode[0].startsWith("ap")) {
				try {
					apCode = Integer.parseInt(splittedApCode[0].substring(2));
				} catch (NumberFormatException e) {
					return null;
				}
			} else {
				return null;
			}
		} else if (splittedApCode.length == 2) {
			if (splittedApCode[0].startsWith("ap")) {
				try {
					apCode = Integer.parseInt(splittedApCode[0].substring(2));
					partNumber = "" + Integer.parseInt(splittedApCode[1]);
				} catch (NumberFormatException e) {
					return null;
				}				
			} else {
				return null;
			}
		} else {
			return null;
		}		
		return new InstitutionFindingAidKey(archiveCode, apCode, partNumber);				
	}
	
	
	private List<ImageUrl> getLinks(InstitutionFindingAidKey ifKey) {
		FindingAidDaoLinks resource = links.get(ifKey);
		if (resource == null) {
			String shortcut = "";
			for (var inst : config.getInstitutions()) {
				if (Objects.equals(ifKey.getInstitutionCode(), inst.getInstitutionCode())) {
					shortcut = inst.getShortcut();
					break;
				}
			}			
			String fileName = null;
			if (ifKey.getFindingAidPartNumber() != null) {
				fileName = String.format("ap%1$04d-%2$03d.csv", ifKey.getFindingAidCode(),
						Integer.parseInt(ifKey.getFindingAidPartNumber()));
			} else {
				fileName = String.format("ap%1$04d.csv", ifKey.getFindingAidCode());
			}
			var zipPath = path.resolve("web").resolve("aron").resolve("ap-csv").resolve(shortcut).resolve(fileName);
			resource = new FindingAidDaoLinks(zipPath);
			links.put(ifKey, resource);
		}
		return resource.getResource();
	}

	public String getFindingAidDaoHandle(String archiveCode, String findingAidCode, String findingAidPartNumber) {
		FileStore5Handle handle = FileStore5Handle.of(archiveCode, Integer.parseInt(findingAidCode),
				findingAidPartNumber);
		var links = getLinks(handle.getIfKey());
		if (links == null) {
			return null;
		}
		return handle.toString();
	}

	private static class InstitutionFindingAidKey {

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
	
	private static class ImageUrl {
		
		private final String url;
		
		private final boolean selected;

		public ImageUrl(String url, boolean selected) {
			this.url = url;
			this.selected = selected;
		}

		public String getUrl() {
			return url;
		}

		public boolean isSelected() {
			return selected;
		}
		
	}
	
	/*
	try (InputStream is = new FileInputStream("input.csv");
            BOMInputStream bis = new BOMInputStream(is);
	*/

	private class FindingAidDaoLinks extends MonitoredFileResource<List<ImageUrl>> {

		public FindingAidDaoLinks(Path path) {
			super(path);
		}

		@Override
		public List<ImageUrl> reloadResources() {
			if (monitoredPath == null || !Files.isRegularFile(monitoredPath)) {
				log.info("ApDaoLinks path not exist {}", monitoredPath);
				return null;
			}
			var ret = new ArrayList<ImageUrl>();
			try (var is = new FileInputStream(monitoredPath.toFile());
					var bis = new BOMInputStream(is);
					Reader in = new InputStreamReader(bis);) {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').setTrim(true).build()
						.parse(in);
				for (CSVRecord record : records) {
					String url = DaoFileStore5Service.this.config.getImageServer()
							+ record.get(0).replaceAll("\\\\", "/");
					var selected = "1".equals(record.get(1));
					ret.add(new ImageUrl(url, selected));
				}
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				log.error("ApDaoLinks fail to read {}", monitoredPath);
			}
			if (ret.isEmpty()) {
				log.warn("ApDao links loaded from path {} NO DATA!!!", monitoredPath);
			} else {
				log.info("ApDao links loaded from path {}", monitoredPath);
			}
			return ret;
		}

	}

	private static class FileStore5Handle {

		private final InstitutionFindingAidKey ifKey;

		public FileStore5Handle(InstitutionFindingAidKey ifKey) {
			this.ifKey = ifKey;
		}

		public InstitutionFindingAidKey getIfKey() {
			return ifKey;
		}

		@Override
		public String toString() {
			return ifKey.getHandle();
		}

		private static FileStore5Handle of(String institutionCode, int findingAidCode, String findingAidPartNumber) {
			return new FileStore5Handle(
					new InstitutionFindingAidKey(institutionCode, findingAidCode, findingAidPartNumber));
		}

		private static FileStore5Handle fromHandle(String handleStr) {
			String[] splitted = handleStr.split("/");
			if (splitted.length == 4) {
				return new FileStore5Handle(
						new InstitutionFindingAidKey(splitted[1], Integer.parseInt(splitted[2]), splitted[3]));
			} else if (splitted.length == 3) {
				return new FileStore5Handle(
						new InstitutionFindingAidKey(splitted[1], Integer.parseInt(splitted[2]), null));
			} else {
				throw new RuntimeException();
			}
		}

	}

	@Override
	public String generateFundPermalink(String archiveCode, String fundCode, Integer subFundCode) {
		ConfigDaoFileStore5Institution institution = null;
		for (var inst : config.getInstitutions()) {
			if (inst.getInstitutionCode().equals(archiveCode)) {
				institution = inst;
				break;
			}
		}
		if (institution == null) {
			return null;
		}
		var builder = new StringBuilder();
		builder.append("/").append(institution.getShortcut()).append("/")
				.append(fundCode);
		if (subFundCode != null) {
			builder.append("-");
			builder.append(String.format("%03d", subFundCode));
		}
		return builder.toString();
	}

	@Override
	public String generateApPermalink(String archiveCode, String findingAidCode, String findingAidPartNumber) {
		ConfigDaoFileStore5Institution institution = null;
		for (var inst : config.getInstitutions()) {
			if (inst.getInstitutionCode().equals(archiveCode)) {
				institution = inst;
				break;
			}
		}
		if (institution == null) {
			return null;
		}
		var builder = new StringBuilder();
		builder.append("/").append(institution.getShortcut()).append("/ap");
		try {
			var code = Integer.parseInt(findingAidCode);
			builder.append(String.format("%04d", code));
		} catch (NumberFormatException e) {
			builder.append(findingAidCode);
		}		
		if (findingAidPartNumber != null) {
			builder.append("-");
			try {
				var partNumber = Integer.parseInt(findingAidPartNumber);				
				builder.append(String.format("%03d", partNumber));
			} catch (NumberFormatException e) {
				builder.append(findingAidPartNumber);
			}
		}
		return builder.toString();
	}

}
