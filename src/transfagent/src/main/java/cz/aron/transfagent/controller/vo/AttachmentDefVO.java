package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.AttachmentDef;
import cz.aron.transfagent.domain.Attachment;

public class AttachmentDefVO extends AttachmentDef {
	
	public AttachmentDefVO(Attachment attachment) {
		setApusourceId(attachment.getApuSource().getId().longValue());
		setAttachmentId(attachment.getId().longValue());
		setFileName(attachment.getFileName());
		setLastModified(attachment.getLastModified());
		setName(attachment.getFileName());
		setUuid(attachment.getUuid().toString());
	}

}
