package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.DaoFileDef;
import cz.aron.transfagent.domain.Transform;

public class DaoFileDefVO extends DaoFileDef {

	public DaoFileDefVO(Transform transform) {
		setFileId(transform.getId().longValue());
		setFileName(transform.getFile());
		if (transform.getLastUpdate()!=null) {
			setLastModified(transform.getLastUpdate().toInstant().toEpochMilli());	
		} else {
			setLastModified(0L);
		}		
		setState(transform.getState().toString());
		setType(transform.getType());
		setUuid(transform.getFileUuid().toString());
	}
	
}
