package cz.aron.transfagent.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import cz.aron.transfagent.elza.ElzaTypes;

@Configuration
@ConfigurationProperties(prefix = "archdesc")
public class ConfigElzaArchDesc {
    
    // add date to level name
    public boolean addDateToName;
    
    // inherit name+date field from parent when name is empty
    public boolean inheritNameDate;

    // import attachments stored inside xml
    public boolean importAttachments;
    
    // show ZP2015_APPLIED_RESTRICTION_TEXT
    public boolean showAccessRestrictions;
    
    // nazev urovne slozen ze zkratky instituce a nazvu urovne
    public boolean composedShortName;
    
    public boolean processInternalSupplement;
    
    public boolean uniqueLevels;
    
    // generuje ulozeni
    public boolean storage = true;
    
    // mapování přístupových bodů na hodnoty
    private List<ConfigElzaArchDescApMapping> apMappings;
    
    private Set<ConfigElzaInheritedType> inherited = new HashSet<>();;
    
    private boolean inheritAttributes;

    public boolean isAddDateToName() {
        return addDateToName;
    }

    public void setAddDateToName(boolean addDateToName) {
        this.addDateToName = addDateToName;
    }

    public boolean isInheritNameDate() {
        return inheritNameDate;
    }

    public void setInheritNameDate(boolean inheritNameDate) {
        this.inheritNameDate = inheritNameDate;
    }

    public boolean isImportAttachments() {
        return importAttachments;
    }

    public void setImportAttachments(boolean importAttachments) {
        this.importAttachments = importAttachments;
    }

    public boolean isShowAccessRestrictions() {
        return showAccessRestrictions;
    }

    public void setShowAccessRestrictions(boolean showAccessRestrictions) {
        this.showAccessRestrictions = showAccessRestrictions;
    }

	public boolean isComposedShortName() {
		return composedShortName;
	}

	public void setComposedShortName(boolean composedShortName) {
		this.composedShortName = composedShortName;
	}

	public List<ConfigElzaArchDescApMapping> getApMappings() {
		return apMappings;
	}

	public void setApMappings(List<ConfigElzaArchDescApMapping> apMappings) {
		this.apMappings = apMappings;
	}

	public boolean isProcessInternalSupplement() {
		return processInternalSupplement;
	}

	public void setProcessInternalSupplement(boolean processInternalSupplement) {
		this.processInternalSupplement = processInternalSupplement;
	}

	public boolean isUniqueLevels() {
		return uniqueLevels;
	}

	public void setUniqueLevels(boolean uniqueLevels) {
		this.uniqueLevels = uniqueLevels;
	}

	public boolean isStorage() {
		return storage;
	}

	public void setStorage(boolean storage) {
		this.storage = storage;
	}

	public Set<ConfigElzaInheritedType> getInherited() {
		return inherited;
	}

	public void setInherited(List<ConfigElzaInheritedType> inherited) {
		this.inherited.addAll(inherited);
	}
	
	public boolean isInherited(ConfigElzaInheritedType type) {
		return inherited.contains(type);
	}

	public boolean isInheritAttributes() {
		return inheritAttributes;
	}

	public void setInheritAttributes(boolean inheritAttributes) {
		this.inheritAttributes = inheritAttributes;
		if (inheritAttributes) {
			inherited = new HashSet<>();
			ElzaTypes.otherIdMap.forEach((k,v)->{
				inherited.add(new ConfigElzaInheritedType(ElzaTypes.ZP2015_OTHER_ID,k));
			});
			ElzaTypes.roleSpecMap.forEach((k,v)->{
				inherited.add(new ConfigElzaInheritedType(ElzaTypes.ZP2015_ENTITY_ROLE,k));
			});
			inherited.add(new ConfigElzaInheritedType(ElzaTypes.ZP2015_STORAGE_ID,null));
		}
	}

}
