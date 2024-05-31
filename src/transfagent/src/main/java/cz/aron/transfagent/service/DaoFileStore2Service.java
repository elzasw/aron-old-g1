package cz.aron.transfagent.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigDaoFileStore2;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.TransformService;

@ConditionalOnProperty(value="filestore2.path")
@Service
public class DaoFileStore2Service  implements DaoImporter {
	
	private final static Logger log = LoggerFactory.getLogger(DaoFileStore2Service.class);

	private final ConfigDaoFileStore2 config;
	
	private final TransformService transformService;
	
	/**
	 * Mapovani (archiv,fond,daoId)->seznam souboru k daoId, plna cesta k souborum
	 */
	private final LRUMap<ArchiveFundKey, Map<String, List<Path>>> fundDaoCache = new LRUMap<>(10);
	
	/**
	 * Mapovani (archiv, fond) -> adesar s digitalizaty
	 */
	private Map<ArchiveFundKey, List<Path>> fundDaoPaths = new HashMap<>();
	
	private long lastCheck = 0L;
	
	public DaoFileStore2Service(ConfigDaoFileStore2 config, TransformService transformService) throws IOException {
		this.config = config;
		this.transformService = transformService;
	}

	@Override
	public String getName() {
		return "file2";
	}

	@Override
	public void importDaoFile(Dao dao, Path daoDir) {
		ArchiveFundDao afd = ArchiveFundDao.fromString(dao.getHandle());
		List<Path> daoPaths;
		try {
			daoPaths = getDaos(afd);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}
		try {
			for (Path daoPath : daoPaths) {
				Files.copy(daoPath, daoDir.resolve(daoPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			if (!transformService.transform(daoDir, null)) {
				// delete empty dir
			}
			dao.setState(DaoState.READY);
		} catch (Exception e) {
			log.error("Fail to import dao {}", dao.getUuid(), e);
		}
	}
	
	public List<Path> getDaos(ArchiveFundDao archiveFundDao) throws IOException {
		Map<String, List<Path>> daos = getFundDaos(archiveFundDao.createFundKey());
		return daos.get(archiveFundDao.getDaoId());
	}

	private synchronized Map<ArchiveFundKey, List<Path>> getFundDaoPaths() throws IOException {
		long currentTime = System.currentTimeMillis();
		if ((currentTime-lastCheck) > config.getRefresh()*1000*60) {
			// reload
			fundDaoCache.clear();
			if (config.isOldFormat()) {
				fundDaoPaths = readFundDaoPathsOld();
			} else {
				fundDaoPaths = readFunDaoPaths();
			}
			lastCheck = currentTime;
		}
		return fundDaoPaths;
	}
	
	private synchronized Map<String, List<Path>> getFundDaos(ArchiveFundKey key) throws IOException {		
		// call to force check actualness
		var daoPaths = getFundDaoPaths();		
		Map<String, List<Path>> daos = fundDaoCache.get(key);
		if (daos == null) {
			daos = new HashMap<>();
			List<Path> paths = daoPaths.get(key);
			if (paths == null) {
				fundDaoCache.put(key, daos);
			} else {
				for (Path path : paths) {
					Map<String, List<Path>> tmp = read(path);
					for (Map.Entry<String, List<Path>> entry : tmp.entrySet()) {
						daos.compute(entry.getKey(), (k, v) -> {
							if (v == null) {
								v = new ArrayList<>();
							}
							v.addAll(entry.getValue());
							return v;
						});
					}
				}
				// order paths
				for (var p : daos.values()) {
					p.sort((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()));
				}
				fundDaoCache.put(key, daos);
			}
		}
		return daos;
	}

	/**
	 * Nacte cesty ke vsem dao z jednoho adresare. 
	 * @param path plna cesta k adresari s dao 
	 * @return Map<String, List<Path>> (daoId,vsechny soubory k nemu)
	 * @throws IOException
	 */
	private Map<String, List<Path>> read(Path path) throws IOException {
		Map<String, List<Path>> ret = new HashMap<>();
		Set<ArchiveFundKey> archFund = new HashSet<>();
		try (Stream<Path> stream = Files.list(path)) {
			stream.filter(f -> Files.isRegularFile(f)).forEach(f -> {
				String name = f.getFileName().toString();
				String splitted[] = name.split("_");
				if (splitted.length >= 4) {
					String archive = splitted[0];
					Integer fund = Integer.parseInt(splitted[1]);
					String id = splitted[2];
					ArchiveFundKey key = new ArchiveFundKey(archive, fund);
					archFund.add(key);
					ret.compute(id, (k, v) -> {
						if (v == null) {
							v = new ArrayList<>();
						}
						v.add(f);
						return v;
					});
				}
			});
		}
		return ret;
	}

	/**
	 * Inicializuje mapovani (archiv,fond)->seznam adreasru s dao, plna cesta k adresari
	 * @return Map<ArchiveFundKey, List<Path>>
	 * @throws IOException
	 */
	private Map<ArchiveFundKey, List<Path>> readFunDaoPaths() throws IOException {
		log.info("Initializing filestore2 from {}", config.getPath());
		var archiveDirs = new ArrayList<Path>();
		try (var fundStream = Files.list(config.getPath())) {
			fundStream.filter(fs -> Files.isDirectory(fs)).forEach(fs -> archiveDirs.add(fs));
		}
		var ret = new HashMap<ArchiveFundKey, List<Path>>();
		for (Path archiveDir : archiveDirs) {
			var archiveCode = archiveDir.getFileName().toString();
			try (var fundStream = Files.list(archiveDir)) {
				fundStream.filter(fundDir -> Files.isDirectory(fundDir)).forEach(fundDir -> {
					var splitted = fundDir.getFileName().toString().split("_");
					if (StringUtils.isNotBlank(splitted[0])) {
						try {
							ArchiveFundKey key = new ArchiveFundKey(archiveCode, Integer.parseInt(splitted[0]));
							ret.compute(key, (k, v) -> {
								if (v == null) {
									v = new ArrayList<>();
								}
								v.add(fundDir);
								return v;
							});
						} catch (NumberFormatException ex) {
							log.error("Invalid name of directory {}", fundDir);
						}
					}
				});
			}
		}
		log.info("file2store initialized, num dao dirs {}", ret.size());
		return ret;
	}
	
	/**
	 * Inicializuje mapovani (archiv,fond)->seznam adreasru s dao, plna cesta k adresari
	 * @return Map<ArchiveFundKey, List<Path>>
	 * @throws IOException
	 */
	private Map<ArchiveFundKey, List<Path>> readFundDaoPathsOld() throws IOException {
		log.info("Initializing filestore2 from {}",config.getPath());
		Map<ArchiveFundKey, List<Path>> fundDaoDir = new HashMap<>();
		try (Stream<Path> stream = Files.list(config.getPath())) {
			stream.filter(f -> Files.isDirectory(f)).forEach(dir -> {
				try (Stream<Path> fileStream = Files.list(dir)) {
					fileStream.filter(f -> Files.isRegularFile(f)).findFirst().ifPresent(p -> {
						try {
							ArchiveFundDao afd = ArchiveFundDao.fromString(p.getFileName().toString());
							ArchiveFundKey key = new ArchiveFundKey(afd.getArchive(), afd.getFund());
							fundDaoDir.compute(key, (k, v) -> {
								if (v == null) {
									v = new ArrayList<>();
								}
								v.add(dir);
								return v;
							});
						} catch (IllegalArgumentException iae) {

						}
					});
				} catch (IOException ioEx) {
					log.error("Fail read dir {}", dir, ioEx);
				}
			});
		}
		log.info("file2store initialized, num dao dirs {}", fundDaoDir.size());
		return fundDaoDir;
	}

	public static class ArchiveFundKey {
		private final String archive;
		private final Integer fund;
		
		public ArchiveFundKey(String archive, Integer fund) {
			this.archive = archive;
			this.fund = fund;
		}

		public String getArchive() {
			return archive;
		}

		public Integer getFund() {
			return fund;
		}

		@Override
		public int hashCode() {
			return Objects.hash(archive, fund);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ArchiveFundKey other = (ArchiveFundKey) obj;
			return Objects.equals(archive, other.archive) && Objects.equals(fund, other.fund);
		}
		
	}
	
	public static class ArchiveFundDao {
		private final String archive;
		private final Integer fund;
		private final String daoId;

		public ArchiveFundDao(String archive, Integer fund, String daoId) {
			this.archive = archive;
			this.fund = fund;
			this.daoId = daoId;
		}

		public String getArchive() {
			return archive;
		}

		public Integer getFund() {
			return fund;
		}

		public String getDaoId() {
			return daoId;
		}
		
		@Override
		public String toString() {
			return archive + "_" + fund + "_" + daoId;
		}
		
		public ArchiveFundKey createFundKey() {
			return new ArchiveFundKey(archive,fund);
		}
		
		public static ArchiveFundDao fromString(String str) {
			String splitted[] = str.split("_");
			if (splitted.length >= 3) {
				return new ArchiveFundDao(splitted[0], Integer.parseInt(splitted[1]), splitted[2]);
			} else {
				throw new IllegalArgumentException("");
			}
		}

	}
	
}
