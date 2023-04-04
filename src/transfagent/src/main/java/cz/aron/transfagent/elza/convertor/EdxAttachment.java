package cz.aron.transfagent.elza.convertor;

import java.util.List;

import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemFileRef;

public class EdxAttachment implements EdxItemConvertor {
    
    private final List<String> attachmentIds;
    
    public EdxAttachment(List<String> attachmentIds) {
        this.attachmentIds = attachmentIds;
    }

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        DescriptionItemFileRef fileRef = (DescriptionItemFileRef) item;
        if ("ZP2015_ATTACHMENT".equals(fileRef.getT())) {
            attachmentIds.add(fileRef.getFid());
        }
        return;
    }

}
