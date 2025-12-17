package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.ArchDescDef;
import cz.aron.transfagent.domain.ArchDesc;

public class ArchDescDefVO extends ArchDescDef {
	
	public ArchDescDefVO(ArchDesc archDesc) {
		setApusourceId(archDesc.getApuSource().getId().longValue());
		setArchDescId(archDesc.getId().longValue());
		setFundId(archDesc.getFund().getId().longValue());
		if (archDesc.getRootUuid() != null) {
			setUuid(archDesc.getRootUuid().toString());
		}
		setDeleted(archDesc.getApuSource().isDeleted());
		setFundName(archDesc.getFund().getName());
		setInstitutionId(archDesc.getFund().getInstitution().getId().longValue());
		var fund = archDesc.getFund();
		if (fund.getNad() != null) {
			if (fund.getSubNad()!=null) {
				setNad(fund.getNad().toString() + "/" + fund.getSubNad().toString());		
			} else {
				setNad(fund.getNad().toString());
			}
		}
		
	}	

}
