package cz.aron.transfagent.peva;

import cz.aron.transfagent.peva.codelist.AccessibilityProvider;
import cz.aron.transfagent.peva.codelist.DatingMethodProvider;
import cz.aron.transfagent.peva.codelist.DepartmentProvider;
import cz.aron.transfagent.peva.codelist.EvidenceUnitTypeProvider;
import cz.aron.transfagent.peva.codelist.FindingAidFormTypeProvider;
import cz.aron.transfagent.peva.codelist.FindingAidTypeProvider;
import cz.aron.transfagent.peva.codelist.GeoObjectTypeProvider;
import cz.aron.transfagent.peva.codelist.IntegrityProvider;
import cz.aron.transfagent.peva.codelist.LanguageProvider;
import cz.aron.transfagent.peva.codelist.OriginatorSubClassProvider;
import cz.aron.transfagent.peva.codelist.Peva2DatingMethod;
import cz.aron.transfagent.peva.codelist.Peva2Department;
import cz.aron.transfagent.peva.codelist.Peva2EvidenceUnitType;
import cz.aron.transfagent.peva.codelist.Peva2GeoObjectType;
import cz.aron.transfagent.peva.codelist.Peva2Language;
import cz.aron.transfagent.peva.codelist.Peva2OriginatorSubclass;
import cz.aron.transfagent.peva.codelist.Peva2ThematicEvidenceGroup;
import cz.aron.transfagent.peva.codelist.PhysicalStateProvider;
import cz.aron.transfagent.peva.codelist.ThematicEvidenceGroupProvider;

public class Peva2CodeLists {
    
    private final AccessibilityProvider accessibilities;
    private final PhysicalStateProvider physicalStates;
    private final IntegrityProvider integrity;    
    private final FindingAidTypeProvider findingAidTypes;
    private final LanguageProvider languages;
    private final EvidenceUnitTypeProvider evidenceUnitTypes;
    private final FindingAidFormTypeProvider findingAidFormTypes;
    private final OriginatorSubClassProvider originatorSubClasses;
    private final DatingMethodProvider datingMethods;
    private final ThematicEvidenceGroupProvider thematicGroups;
    private final GeoObjectTypeProvider geoTypes;
    private final DepartmentProvider departments;
    
	public Peva2CodeLists(AccessibilityProvider accessibilities, PhysicalStateProvider physicalStates,
			IntegrityProvider integrity, FindingAidTypeProvider findingAidTypes, LanguageProvider languages,
			EvidenceUnitTypeProvider evidenceUnitTypes, FindingAidFormTypeProvider findingAidFormTypes,
			OriginatorSubClassProvider originatorSubClasses, DatingMethodProvider datingMethods,
			ThematicEvidenceGroupProvider thematicGroups, GeoObjectTypeProvider geoTypes,
			DepartmentProvider departments) {
		this.accessibilities = accessibilities;
		this.physicalStates = physicalStates;
		this.integrity = integrity;
		this.findingAidTypes = findingAidTypes;
		this.languages = languages;
		this.evidenceUnitTypes = evidenceUnitTypes;
		this.findingAidFormTypes = findingAidFormTypes;
		this.originatorSubClasses = originatorSubClasses;
		this.datingMethods = datingMethods;
		this.thematicGroups = thematicGroups;
		this.geoTypes = geoTypes;
		this.departments = departments;
	}

    public String getAccessibilityName(String id) {
        return accessibilities.getItemById(id);
    }

    public String getPhysicalStateName(String id) {
        return physicalStates.getItemById(id);
    }

    public String getIntegrityName(String id) {
        return integrity.getItemById(id);
    }

    public String getFindingAidType(String id) {
    	return findingAidTypes.getItemById(id);
    }
    
    public String getFindingAidFormType(String id) {
    	return findingAidFormTypes.getItemById(id);
    }
    
	public Peva2Language getLanguage(String id) {
		return languages.getItemById(id);
	}
	
	public Peva2EvidenceUnitType getEvidenceUnitType(String id) {
		return evidenceUnitTypes.getItemById(id);
	}
	
	public Peva2OriginatorSubclass getOriginatorSubClass(String id) {
		return originatorSubClasses.getItemById(id);
	}
	
	public Peva2DatingMethod getDatingMethod(String id) {
		return datingMethods.getItemById(id);
	}
	
	public Peva2ThematicEvidenceGroup getThematicGroup(String id) {
		return thematicGroups.getItemById(id);
	}
	
	public ThematicEvidenceGroupProvider getThematicGroupProvider() {
		return thematicGroups;
	}
	
	public Peva2GeoObjectType getGeoType(String id) {
		return geoTypes.getItemById(id);
	}

	public Peva2Department getDepartment(String id) {
		return departments.getItemById(id);
	}
	
}
