package cz.aron.customizations.soaplzen;

import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "filestore5")
public class ConfigDaoFileStore5 {
	
    private Path path;
    
    private List<ConfigDaoFileStore5Institution> institutions;
    
    private String imageServer;
    
    private String permalinkUrl;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

	public List<ConfigDaoFileStore5Institution> getInstitutions() {
		return institutions;
	}

	public void setInstitutions(List<ConfigDaoFileStore5Institution> institutions) {
		this.institutions = institutions;
	}

	public String getImageServer() {
		return imageServer;
	}

	public void setImageServer(String imageServer) {
		this.imageServer = imageServer;
	}

	public String getPermalinkUrl() {
		return permalinkUrl;
	}

	public void setPermalinkUrl(String permalinkUrl) {
		this.permalinkUrl = permalinkUrl;
	}

	public ConfigDaoFileStore5Institution getInstitutionByCode(String code) {
		for (var inst : institutions) {
			if (inst.getInstitutionCode().equals(code)) {
				return inst;
			}
		}
		return null;
	}
	
	public ConfigDaoFileStore5Institution getInstitutionByShortcut(String shortcut) {
		for (var inst : institutions) {
			if (inst.getShortcut().equals(shortcut)) {
				return inst;
			}
		}
		return null;
	}
	
	public ConfigDaoFileStore5Institution getInstitutionByNid(String nid) {
		for (var inst : institutions) {
			if (inst.getNid().equals(nid)) {
				return inst;
			}
		}
		return null;
	}

}
