package cz.aron.transfagent.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.common.MonitoredFileResource;
import cz.aron.transfagent.config.ConfigDaoFileStore4;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.TransformService;

@ConditionalOnProperty(value="filestore4.path")
@Service
public class DaoFileStore4Service implements DaoImporter  {
    
    private static final Logger log = LoggerFactory.getLogger(DaoFileStore4Service.class);
    
    private final Path path;
    
    private final TransformService transformService;
    
    @SuppressWarnings("serial")
	private final Map<InstitutionFundKey, FundDaoLinks> links = new LinkedHashMap<InstitutionFundKey,FundDaoLinks>(16, (float) 0.75, true) {

        @Override
        protected boolean removeEldestEntry(Entry<InstitutionFundKey, FundDaoLinks> eldest) {
            return this.size()>2;
        }

    };
    
    public DaoFileStore4Service(ConfigDaoFileStore4 config, TransformService transformService) {
        this.path = config.getPath();
        this.transformService = transformService;
    }

    @Override
    public String getName() {
        return "file4";
    }

    /**
     * Vrati dao handle pokud k instituci, fondu a uuid existuje
     * @param institutionCode kod instituce
     * @param fundCode kod fondu
     * @param uuid uuid urovne popisu
     * @return handle nebo null
     */
    public String getDaoHandle(String institutionCode, int fundCode, String uuid) {
    	FileStore4Handle handle = FileStore4Handle.of(institutionCode, fundCode, uuid);         
        Map<String, List<String>> links = getLinks(handle.getIfKey());
        if (links==null) {
            return null;
        }
        return links.containsKey(uuid)?handle.toString():null;
    }

	@Override
	public void importDaoFile(Dao dao, Path daoDir) {
		FileStore4Handle handle = FileStore4Handle.fromHandle(dao.getHandle());
		Map<String, List<String>> links = getLinks(handle.getIfKey());
		if (links != null) {
			List<String> daoLinks = links.get(handle.getUuid());
			if (daoLinks != null) {
				try {
					transformService.transformHttpUrls(daoDir, daoLinks);
					dao.setState(DaoState.READY);
				} catch (Exception e) {
					log.error("Fail to import dao id={}, handle={}, uuid={}", dao.getId(), dao.getHandle(),
							dao.getUuid());
					throw new RuntimeException();
				}
			} else {
				log.error("Fail to import dao, no links to files, id={}, handle={}, uuid={}", dao.getId(),
						dao.getHandle(), dao.getUuid());
				dao.setState(DaoState.INACCESSIBLE);
			}
		} else {
			dao.setState(DaoState.INACCESSIBLE);
		}
	}

	private synchronized Map<String, List<String>> getLinks(InstitutionFundKey key) {
		FundDaoLinks resource = links.get(key);
		if (resource == null) {
			Path zipPath = path.resolve(key.getInstitutionCode()).resolve("" + key.getFundCode() + ".zip");
			resource = new FundDaoLinks(zipPath);
			links.put(key, resource);
		}
		return resource.getResource();
	}

    private static class InstitutionFundKey {
        
        private final String institutionCode;
        
        private final int fundCode;

        public InstitutionFundKey(String institutionCode, int fundCode) {
            this.institutionCode = institutionCode;
            this.fundCode = fundCode;
        }

        public String getInstitutionCode() {
            return institutionCode;
        }

        public int getFundCode() {
            return fundCode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fundCode, institutionCode);
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
            return fundCode == other.fundCode && Objects.equals(institutionCode, other.institutionCode);
        }
        
        public String getHandle() {
            return institutionCode + "/" + fundCode;
        }

    }
    
	private static class FundDaoLinks extends MonitoredFileResource<Map<String, List<String>>> {

		public FundDaoLinks(Path path) {
			super(path);
		}

		@Override
		public Map<String, List<String>> reloadResources() {
			if (monitoredPath == null || !Files.isRegularFile(monitoredPath)) {
				return null;
			}
			Map<String, List<String>> ret = new HashMap<>();
			try (ZipFile zf = new ZipFile(monitoredPath.toFile());) {
				ZipEntry ze = zf.getEntry("images.csv");
				BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));
				br.lines().forEach(l -> {
					String[] splitted = l.split(",");
					if (splitted.length == 2) {
						String uuid = splitted[0];
						String url = splitted[1];
						List<String> urls = ret.get(uuid);
						if (urls == null) {
							urls = new ArrayList<>();
							ret.put(uuid, urls);
						}
						urls.add(url);
					}
				});
			} catch (IOException ioEx) {
				log.error("Fail to load dao links from path {}", monitoredPath, ioEx);
				throw new UncheckedIOException(ioEx);
			}
			if (ret.isEmpty()) {
				log.warn("Dao links loaded from path {} NO DATA!!!", monitoredPath);
			} else {
				log.info("Dao links loaded from path {}", monitoredPath);
			}
			return ret;
		}

	}
	
	private static class FileStore4Handle {

		private final InstitutionFundKey ifKey;

		private final String uuid;

		public FileStore4Handle(InstitutionFundKey ifKey, String uuid) {
			this.ifKey = ifKey;
			this.uuid = uuid;
		}

		public InstitutionFundKey getIfKey() {
			return ifKey;
		}

		public String getUuid() {
			return uuid;
		}
		
		@Override
		public String toString() {
			return ifKey.getHandle() + "/" + uuid;
		}

		private static FileStore4Handle of(String institutionCode, int fundCode, String uuid) {
			return new FileStore4Handle(new InstitutionFundKey(institutionCode, fundCode), uuid);
		}

		private static FileStore4Handle fromHandle(String handleStr) {
			String[] splitted = handleStr.split("/");
			if (splitted.length != 3) {
				throw new RuntimeException();
			}
			return new FileStore4Handle(new InstitutionFundKey(splitted[0], Integer.parseInt(splitted[1])),
					splitted[2]);
		}

	}
    
}
