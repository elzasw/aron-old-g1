package cz.aron.transfagent.elza.convertor;

import java.util.UUID;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;

public class EdxLinkConvertor implements EdxItemConvertor {
	
	private static final String ELZA_NODE = "elza-node";
    
    private final String targetTypeRef;
    private final String targetTypeLink;

    public EdxLinkConvertor(final String targetTypeRef,
                            final String targetTypeLink) {
        this.targetTypeRef = targetTypeRef;
        this.targetTypeLink = targetTypeLink;
    }

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        if(item instanceof DescriptionItemUndefined) {
            return;
        }
        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        
        DescriptionItemUriRef uriRef = (DescriptionItemUriRef)item;
        if(ELZA_NODE.equals(uriRef.getSchm())) {
            // link to another apu
            String uuid = uriRef.getUri().substring(ELZA_NODE.length());
            if(uuid.startsWith("://")) {
                uuid = uuid.substring(3);
            }
            apusBuilder.addApuRef(ctx.getActivePart(), targetTypeRef, UUID.fromString(uuid));
        } else {
            // prepare external link
            ApuSourceBuilder.addLink(ctx.getActivePart(), targetTypeLink, uriRef.getUri(), uriRef.getLbl());
        }
    }

}
