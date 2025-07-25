package cz.aron.transfagent.elza.convertor;

import java.util.Map;
import java.util.Set;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;

/**
 * Convert ZP2015_UNIT_TYPE to UNIT_TYPE
 */
public class EdxUnitTypeConvertor implements EdxItemConvertor {

	private final String targetType;
	private final Map<String, String> enumMap;
	private final Set<String> spisSet;

	/**
	 * Constructor
	 * @param targetType target type (UNIT_TYPE)
	 * @param enumMap enum values mapping
	 * @param spisMap values mapped to spis
	 */
	public EdxUnitTypeConvertor(String targetType, Map<String, String> enumMap, Set<String> spisSet) {
		this.targetType = targetType;
		this.enumMap = enumMap;
		this.spisSet = spisSet;
	}

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		String specCode = null;
		if (item instanceof DescriptionItemEnum) {
			var itemEnum = (DescriptionItemEnum) item;
			specCode = itemEnum.getS();
		} else if (item instanceof DescriptionItemInteger) {
			var dii = (DescriptionItemInteger) item;
			specCode = dii.getS();
		}

		var value = enumMap.get(specCode);
		// skip values without mapping
		if (value != null) {
			ApuSourceBuilder.addEnum(ctx.getActivePart(), targetType, value, true);
		}
		if (spisSet.contains(specCode)) {
			ApuSourceBuilder.addEnum(ctx.getActivePart(), targetType, "spis", false);
		}		
	}

}
