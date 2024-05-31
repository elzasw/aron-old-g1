package cz.aron.transfagent.elza.convertor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Part;
import cz.aron.common.itemtypes.TypesConfiguration;
import cz.aron.transfagent.config.ConfigElzaArchDescApMapping;
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

public class EdxApRefWithRole implements EdxItemConvertor {

    private final String partType;
    private final ContextDataProvider dataProvider;
    private final Map<String, String> specMap;    
    private final List<ConfigElzaArchDescApMapping> apMappings;
    private final boolean processPrivSupplement;
    private final ElzaNameBuilder nameBuilder;
    private final TypesConfiguration typesConfig;

    public EdxApRefWithRole(String partType,
            final ContextDataProvider dataProvider, 
            final Map<String, String> specMap,
            final List<ConfigElzaArchDescApMapping> apMappings,
            final boolean processPrivSupplement,
            final ElzaNameBuilder nameBuilder,
            final TypesConfiguration typesConfig) {
        this.partType = partType;
        this.dataProvider = dataProvider;
        this.specMap = specMap;
        if (apMappings==null) {
        	this.apMappings = Collections.emptyList();
        } else {
        	this.apMappings = apMappings;
        }
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
        if(ap == null) {
            throw new RuntimeException("Failed to convert AP: " + apRef.getApid() + ", ap not found");
        }
        UUID apUuid = UUID.fromString(ap.getApe().getUuid());
        if (ctx.isArchEntityReferenced(apUuid)) {
        	// ap already referenced
        	return;
        }

        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        
        String t = this.specMap.get(apRef.getS());
        if(t == null) {
            throw new RuntimeException("Missing mapping for type: " + apRef.getT() + ", spec: " + apRef.getS() + ", ap not found");
        }
        
		if (processPrivSupplement) {
			if (ap.getFrgs().getFrg().stream().flatMap(frg -> frg.getDdOrDoOrDp().stream())
					.anyMatch(d -> ElzaTypes.NM_SUP_PRIV.equals(d.getT()))) {
				Fragment fragment = ap.getFrgs().getFrg().stream().filter(fr -> CoreTypes.PT_NAME.equals(fr.getT()))
						.findFirst().orElse(null);
				if (fragment != null) {
					String prefix = "";
					var itc = typesConfig.getItemTypes().stream().filter(itemType->itemType.getCode().equals(t)).findFirst().orElse(null);
					if (itc!=null) {
						prefix = "" + itc.getName() + ": ";
					}
					ApuSourceBuilder.addString(ctx.getActivePart(), "ENTITY_NOTE",
							prefix + nameBuilder.createFullName(fragment, ap.getApe().getT(), true));
				}
				return;
			}
		}
        
        Part part = ApuSourceBuilder.getFirstPart(ctx.getActiveApu(), partType);
        if(part == null) {
            part = ApuSourceBuilder.addPart(ctx.getActiveApu(), partType);
        }
        
        var archEntityInfo = dataProvider.getArchivalEntityWithParentsByUuid(apUuid);
                
        for(var apMapping:apMappings) {
        	if (Objects.equals(apMapping.getSpec(), apRef.getS())&&Objects.equals(apMapping.getUuid(), ap.getApe().getUuid()) ) {
        		ApuSourceBuilder.addEnum(part, apMapping.getCode(), apMapping.getName(), true);
        	}
        }

        //apusBuilder.addEnum(part, roleType, s, true);
        if (CollectionUtils.isEmpty(archEntityInfo)) {
            ArchEntityInfo aei = new ArchEntityInfo(apUuid, ap.getApe().getT()); 
            ctx.addArchEntityRef(aei);
            apusBuilder.addApuRef(part, t, apUuid);
        } else {
            List<UUID> uuids = new ArrayList<>(archEntityInfo.size());
            for(ArchEntityInfo aei : archEntityInfo) {
                var uuid = aei.getUuid();
                if (!ctx.isArchEntityReferenced(apUuid)) {
                	uuids.add(uuid);
                	ctx.addArchEntityRef(aei);
                }
            }
            apusBuilder.addApuRefsFirstVisible(part, t, uuids);
        }
    }

}
