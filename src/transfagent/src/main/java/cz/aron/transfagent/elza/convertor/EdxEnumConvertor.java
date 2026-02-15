package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;

public class EdxEnumConvertor implements EdxItemConvertor {

	private final String targetType;
	private final Map<String, String> enumMap;
	private final boolean visible;

	public EdxEnumConvertor(final String targetType,
			final Map<String, String> enumMap) {
		this.targetType = targetType;
		this.enumMap = enumMap;
		this.visible = true;
	}
	
	public EdxEnumConvertor(String targetType,
			Map<String, String> enumMap, boolean visible) {
		this.targetType = targetType;
		this.enumMap = enumMap;
		this.visible = visible;
	}


	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
	    
	    String specCode = null;
	    if(item instanceof DescriptionItemEnum) {
	        var itemEnum = (DescriptionItemEnum)item;
	        specCode = itemEnum.getS();
	    } else
	    if(item instanceof DescriptionItemInteger) {
	        var dii = (DescriptionItemInteger)item;
	        specCode = dii.getS();
	    }
		
		var value = enumMap.get(specCode);
		// skip values without mapping
		if(value==null) {
			return;
		}
		ApuSourceBuilder.addEnum(ctx.getActivePart(), targetType, value, visible);
	}

}
