package cz.aron.transfagent.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "peva2")
public class ConfigPeva2 {
    
    private String url;
    
    private String username;
    
    private String password;
    
    private String userId;
    
    private String institutionId;
    
    private String role;
    
    private long interval = 3600;
    
    private int batchSize = 100;
    
    private boolean soapLogging;
    
    private String attachmentDir;
    
    private ConfigPeva2FundProperties fundProperties;
    
    private ConfigPeva2FindingAidProperties findingAidProperties;
    
    private ConfigPeva2OriginatorProperties originatorProperties;
    
    private List<ConfigPeva2InstitutionCredentials> institutions;
    
    public ConfigPeva2() {
    	// default values
    	fundProperties = new ConfigPeva2FundProperties();
    	findingAidProperties = new ConfigPeva2FindingAidProperties();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isSoapLogging() {
        return soapLogging;
    }

    public void setSoapLogging(boolean soapLogging) {
        this.soapLogging = soapLogging;
    }

	public String getAttachmentDir() {
		return attachmentDir;
	}

	public void setAttachmentDir(String attachmentDir) {
		this.attachmentDir = attachmentDir;
	}

	public ConfigPeva2FundProperties getFundProperties() {
		return fundProperties;
	}

	public void setFundProperties(ConfigPeva2FundProperties fundProperties) {
		this.fundProperties = fundProperties;
	}

	public ConfigPeva2FindingAidProperties getFindingAidProperties() {
		return findingAidProperties;
	}

	public void setFindingAidProperties(ConfigPeva2FindingAidProperties findingAidProperties) {
		this.findingAidProperties = findingAidProperties;
	}

    public List<ConfigPeva2InstitutionCredentials> getInstitutions() {
        return institutions;
    }

    public void setInstitutions(List<ConfigPeva2InstitutionCredentials> institutions) {
        this.institutions = institutions;
    }

	public ConfigPeva2OriginatorProperties getOriginatorProperties() {
		return originatorProperties;
	}

	public void setOriginatorProperties(ConfigPeva2OriginatorProperties originatorProperties) {
		this.originatorProperties = originatorProperties;
	}


}
