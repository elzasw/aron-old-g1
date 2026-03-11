package cz.aron.customizations.soahk;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigDaoFileStore;


/**
 * Sluzba zpristupnujici Dao ulozene na disku. Seznam souboru je v koreni v souboru data.csv ve formatu (uuid;cesta)
 */
@ConditionalOnProperty(value="filestore.path")
@Service
public class DaoFileStoreService {
	
	private static final Logger log = LoggerFactory.getLogger(DaoFileStoreService.class);
	
	protected static final String SERVICE_NAME = "file";
	
	private static final String DAO_CSV = "dao.csv";
	
	private final ConfigDaoFileStore config;
	
	private final Map<String,Path> uuidToPath;
	
	private FileTime lastModification = null;
	
	private long lastCheck = 0;
	
	public DaoFileStoreService(ConfigDaoFileStore config) throws IOException {
		this.config = config;
		if (config.getMappingName()==null) {
			config.setMappingName(DAO_CSV);
		}
		this.uuidToPath = init();
	}

	/**
	 * Vrati cestu k adresari s Dao nebo null pokud pro zadane id Dao neexistuje
	 * 
	 * @param id identifikator Dao
	 * @return Path nebo null pokud Dao neexistuje
	 */
	public synchronized Path getDaoDir(String id) {		
		checkMapping();		
		Path relativePath = uuidToPath.get(id);
		if (relativePath == null) {
			return null;
		}
		Path daoDir = config.getPath().resolve(relativePath);
		if (!config.isCheckExistency()||Files.isDirectory(daoDir)) {
			return daoDir;
		} else {
			return null;
		}
	}
	
	private Map<String, Path> init() throws IOException {
		Map<String, Path> map = new HashMap<>();
		if (config.getPath() != null) {
			Path mappingFile = config.getPath().resolve(config.getMappingName());
			if (!Files.isRegularFile(mappingFile)) {
				log.warn("File data.csv not found {}.", mappingFile);
				return map;
			}
			lastModification = Files.getLastModifiedTime(mappingFile);
			Reader in = new FileReader(mappingFile.toFile());
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').setTrim(true).build().parse(in);
			for (CSVRecord record : records) {
				String uuid = record.get(0);
				String path = record.get(1);
				map.put(uuid, Paths.get(path));
			}
			log.info("File data.csv read, num mappings:{}, path:{}",map.size(), mappingFile);
		}		
		return map;
	}
	
	private void checkMapping() {
		if (config.getPath() != null && (System.currentTimeMillis() - lastCheck) > 5000) {
			lastCheck = System.currentTimeMillis();
			Path mappingFile = config.getPath().resolve(config.getMappingName());
			if (!Files.isRegularFile(mappingFile)) {
				return;
			}
			try {
				FileTime lastMod = Files.getLastModifiedTime(mappingFile);
				if (lastModification == null || lastMod.compareTo(lastModification) != 0) {
					uuidToPath.clear();
					uuidToPath.putAll(init());
					log.info("Dao mapping updated from data.csv");
				}
			} catch (IOException ioEx) {
				log.error("Fail to update mapping from data.csv", ioEx);
			}
		}
	}

}
