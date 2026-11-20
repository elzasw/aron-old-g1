package cz.aron.transfagent.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "filestore")
public class ConfigDaoFileStore {

	private Path path;
	
	private String mappingName;
	
	private boolean checkExistency = true;
	
	private Path daoPath;
	
	private String daoUrlPrefix;

	/**
	 * Pokud je false, pouziji se jiz vygenerovane dzi a nahledy a vytvori se jen
	 * popis dao
	 */
	private boolean regenerate = true;

	public Path getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = Paths.get(path);
	}

	public String getMappingName() {
		return mappingName;
	}

	public void setMappingName(String mappingName) {
		this.mappingName = mappingName;
	}

    public boolean isCheckExistency() {
        return checkExistency;
    }

    public void setCheckExistency(boolean checkExistency) {
        this.checkExistency = checkExistency;
    }

	public Path getDaoPath() {
		return daoPath;
	}

	public void setDaoPath(String daoPath) {
		this.daoPath = Paths.get(daoPath);
	}

	public String getDaoUrlPrefix() {
		return daoUrlPrefix;
	}

	public void setDaoUrlPrefix(String daoUrlPrefix) {
		this.daoUrlPrefix = daoUrlPrefix;
	}

	public boolean isRegenerate() {
		return regenerate;
	}

	public void setRegenerate(boolean regenerate) {
		this.regenerate = regenerate;
	}

}
