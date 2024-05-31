package cz.aron.transfagent.elza.convertor;

import java.math.BigDecimal;
import java.util.StringJoiner;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemDecimal;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;

public class EdxSizeConvertor implements EdxItemConvertor {

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {

		var lvl = ctx.getProcessedLevel();
		if (lvl != null) {
			BigDecimal depth = null;
			BigDecimal width = null;
			BigDecimal height = null;
			String units = null;
			for (var descItem : lvl.getDdOrDoOrDp()) {
				switch (descItem.getT()) {
				case ElzaTypes.ZP2015_SIZE_DEPTH:
					depth = ((DescriptionItemDecimal) descItem).getV();
					break;
				case ElzaTypes.ZP2015_SIZE_HEIGHT:
					height = ((DescriptionItemDecimal) descItem).getV();
					break;
				case ElzaTypes.ZP2015_SIZE_WIDTH:
					width = ((DescriptionItemDecimal) descItem).getV();
					break;
				case ElzaTypes.ZP2015_SIZE_UNITS:
					if (descItem instanceof DescriptionItemEnum) {
						DescriptionItemEnum itemEnum = (DescriptionItemEnum) descItem;
						if ("ZP2015_SIZE_MM".equals(itemEnum.getS())) {
							units = " mm";
						}
					}
					break;
				default:
					// ignore
				}
			}

			var sj = new StringJoiner(" x ");
			if (width != null) {
				sj.add(addUnits(width.toString(), units));
			}
			if (height != null) {
				sj.add(addUnits(height.toString(), units));
			}
			if (depth != null) {
				sj.add(addUnits(depth.toString(), units));
			}

			if (sj.length() > 0) {
				ApuSourceBuilder.addString(ctx.getActivePart(), CoreTypes.SIZE, sj.toString());
			}
		}

	}

	private String addUnits(String size, String units) {
		if (units != null) {
			return size + units;
		} else {
			return size;
		}
	}

}
