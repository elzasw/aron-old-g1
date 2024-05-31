package cz.aron.transfagent.elza.convertor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.common.itemtypes.TypesConfiguration;
import cz.aron.transfagent.elza.ElzaNameBuilder;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.Fragment;

public class EdxApRefConvertor implements EdxItemConvertor {

    private final String targetType;

    private final ContextDataProvider dataProvider;
    
    private final boolean processPrivSupplement;
    
    private final ElzaNameBuilder nameBuilder;
    
    private final TypesConfiguration typesConfig;

	public EdxApRefConvertor(final String targetType, ContextDataProvider dataProvider, boolean processPrivSupplement,
			ElzaNameBuilder nameBuilder, TypesConfiguration typesConfig) {
		this.targetType = targetType;
		this.dataProvider = dataProvider;
		this.processPrivSupplement = processPrivSupplement;
		this.nameBuilder = nameBuilder;
		this.typesConfig = typesConfig;
	}

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        if(item instanceof DescriptionItemUndefined) {
            return;
        }
        DescriptionItemAPRef apRef = (DescriptionItemAPRef)item;

        ElzaXmlReader elzaXmlReader = ctx.getElzaXmlReader();
        Map<String, AccessPoint> apMap = elzaXmlReader.getApMap();
        AccessPoint ap = apMap.get(apRef.getApid());
        if(ap==null) {
            throw new RuntimeException("Failed to convert AP: "+apRef.getApid() + ", ap not found");
        }
                
        UUID apUuid = UUID.fromString(ap.getApe().getUuid());
        if (ctx.isArchEntityReferenced(apUuid)) {
        	// ap already referenced
        	return;
        }
                
		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();

		if (processPrivSupplement) {
			if (ap.getFrgs().getFrg().stream().flatMap(frg -> frg.getDdOrDoOrDp().stream())
					.anyMatch(d -> ElzaTypes.NM_SUP_PRIV.equals(d.getT()))) {
				Fragment fragment = ap.getFrgs().getFrg().stream().filter(fr -> CoreTypes.PT_NAME.equals(fr.getT()))
						.findFirst().orElse(null);
				if (fragment != null) {
					String prefix = "";
					var itc = typesConfig.getItemTypes().stream().filter(itemType->itemType.getCode().equals(targetType)).findFirst().orElse(null);
					if (itc!=null) {
						prefix = ("" + itc.getName() + ": ").toLowerCase();
					}					
					ApuSourceBuilder.addString(ctx.getActivePart(), "ENTITY_NOTE",
							prefix + nameBuilder.createFullName(fragment, ap.getApe().getT(), true));
				}
				return;
			}
		}
        
        var archEntityInfo = dataProvider.getArchivalEntityWithParentsByUuid(apUuid);
        if (CollectionUtils.isEmpty(archEntityInfo)) {
            ctx.addArchEntityRef(new ArchEntityInfo(apUuid, ap.getApe().getT()) );
            apusBuilder.addApuRef(ctx.getActivePart(), targetType, apUuid);
        } else {
            List<UUID> uuids = new ArrayList<>(archEntityInfo.size());
            for(var aei: archEntityInfo) {
            	if (!ctx.isArchEntityReferenced(apUuid)) {
            		uuids.add(aei.getUuid());
            		ctx.addArchEntityRef(aei);
            	}
            }
            apusBuilder.addApuRefsFirstVisible(ctx.getActivePart(), targetType, uuids);
        }
    }

}
