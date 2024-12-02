package cz.aron.transfagent.config;

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

}
