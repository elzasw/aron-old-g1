package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Part;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class EdxStringSpecConvertor implements EdxItemConvertor {
    
    private final Map<String, String> targetTypeMap;
    private Map<String, String> hiddenTargetTypeMap;
    
    private final String partType;

    /**
     * Constructor
     * @param targetTypeMap map from elza specification to aron type
     * 
     * partType is null so add string to active part
     */
	public EdxStringSpecConvertor(Map<String, String> targetTypeMap) {
		this(null, targetTypeMap);
	}

    /**
     * Constructor
     * @param partType partType to add string to, can be null add to active part in this case
     * @param targetTypeMap map from elza specification to aron type
     */
    public EdxStringSpecConvertor(String partType, Map<String, String> targetTypeMap) {
        this.targetTypeMap = targetTypeMap; 
        this.partType = partType;
    }
    
	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		DescriptionItemString itemString = (DescriptionItemString) item;
		var spec = itemString.getS();
		if (spec == null) {
			throw new RuntimeException(
					"Missing specification, type: " + itemString.getT() + ", value: " + itemString.getV());
		}

		String targetType = targetTypeMap.get(spec);
		if (targetType == null) {
			throw new RuntimeException("Missing mapping for specification: " + spec + ", type: " + itemString.getT()
					+ ", value: " + itemString.getV());
		}

		if (partType != null) {
			Part part = ApuSourceBuilder.getFirstPart(ctx.getActiveApu(), partType);
			if (part == null) {
				part = ApuSourceBuilder.addPart(ctx.getActiveApu(), partType);
			}
			ApuSourceBuilder.addString(part, targetType, itemString.getV());
		} else {
			ApuSourceBuilder.addString(ctx.getActivePart(), targetType, itemString.getV());
		}

		// store extra hidden indexes
		if (hiddenTargetTypeMap != null) {
			var htt = hiddenTargetTypeMap.get(spec);
			if (htt != null) {
				var result = ApuSourceBuilder.addString(ctx.getActivePart(), htt, itemString.getV());
				result.setVisible(false);
			}
		}
	}

    public EdxStringSpecConvertor addIndexedItem(Map<String, String> otherIdIndexMap) {
        this.hiddenTargetTypeMap = otherIdIndexMap;
        return this;
    }

}
