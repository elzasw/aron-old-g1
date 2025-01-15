package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.ArchDescDef;
import cz.aron.transfagent.domain.ArchDesc;

public class ArchDescDefVO extends ArchDescDef {
	
	public ArchDescDefVO(ArchDesc archDesc) {
		setApusourceId(archDesc.getApuSource().getId().longValue());
		setArchDescId(archDesc.getId().longValue());
		setFundId(archDesc.getFund().getId().longValue());
		setUuid(archDesc.getUuid().toString());
		setDeleted(archDesc.getApuSource().isDeleted());
	}
	

}
