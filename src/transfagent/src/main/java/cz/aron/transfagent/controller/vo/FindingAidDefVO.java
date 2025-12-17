package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.FindingAidDef;
import cz.aron.transfagent.domain.FindingAid;

public class FindingAidDefVO extends FindingAidDef {

    public FindingAidDefVO(FindingAid findingAid) {
        setApusourceId(findingAid.getApuSource().getId().longValue());
        setCode(findingAid.getCode());
        setFindingAidId(findingAid.getId().longValue());
        setInstitutionId(findingAid.getInstitution().getId().longValue());
        setUuid(findingAid.getUuid().toString());
        setDeleted(findingAid.getApuSource().isDeleted());
        setNumAttachments(findingAid.getApuSource().getNumAttachments());
        setNumDaos(findingAid.getApuSource().getNumDaos());
        if (findingAid.getEvidenceNumber()!=null) {
	        if (findingAid.getPartNumber()!=null) {
		        setEvidenceNumber(""+findingAid.getEvidenceNumber()+"/"+findingAid.getPartNumber());
	        } else {
	        	setEvidenceNumber(""+findingAid.getEvidenceNumber());
	        }
        }
        if (!findingAid.getFunds().isEmpty()) {
        	var fund = findingAid.getFunds().iterator().next();
        	if (fund.getNad() != null) {
            	if (fund.getSubNad()!=null) {
            		setNad(fund.getNad().toString() + "/" + fund.getSubNad().toString());		
            	} else {
            		setNad(fund.getNad().toString());
            	}
            }
        }
        setName(findingAid.getName());
    }

}
