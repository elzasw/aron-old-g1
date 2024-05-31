package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "elza")
public class ConfigElza {

    private boolean disabled = false;

    private String url;

    private String username;

    private String password;

    private boolean soapLogging;
    
    private int maxChildElements = 0;
    
    private boolean archdescFlag = false;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSoapLogging() {
        return soapLogging;
    }

    public void setSoapLogging(boolean soapLogging) {
        this.soapLogging = soapLogging;
    }

    public int getMaxChildElements() {
        return maxChildElements;
    }

    public void setMaxChildElements(int maxChildElements) {
        this.maxChildElements = maxChildElements;
    }

	public boolean isArchdescFlag() {
		return archdescFlag;
	}

	public void setArchdescFlag(boolean archdescFlag) {
		this.archdescFlag = archdescFlag;
	}    

}
