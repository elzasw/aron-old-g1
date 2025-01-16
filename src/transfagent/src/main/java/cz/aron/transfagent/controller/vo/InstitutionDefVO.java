package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.InstitutionDef;
import cz.aron.transfagent.domain.Institution;

public class InstitutionDefVO extends InstitutionDef {

	public InstitutionDefVO(Institution inst) {
		setInstitutionId(inst.getId().longValue());
		setApusourceId(inst.getApuSource().getId().longValue());
		setCode(inst.getCode());
		setDeleted(inst.getApuSource().isDeleted());
		setUuid(inst.getUuid().toString());
	}
	
}
