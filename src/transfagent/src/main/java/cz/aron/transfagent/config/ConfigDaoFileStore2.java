package cz.aron.transfagent.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "filestore2")
public class ConfigDaoFileStore2 {

	private Path path;
	
	// stary format
	private boolean oldFormat = false;
	
	// interval znovunacteni v minutach
	private int refresh = 10;

	public Path getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = Paths.get(path);
	}

	public boolean isOldFormat() {
		return oldFormat;
	}

	public void setOldFormat(boolean oldFormat) {
		this.oldFormat = oldFormat;
	}

	public int getRefresh() {
		return refresh;
	}

	public void setRefresh(int refresh) {
		this.refresh = refresh;
	}

}
