package cz.aron.transfagent.elza.convertor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
			List<OtherSize> otherSizes = new ArrayList<>();			
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
				case ElzaTypes.ZP2015_SIZE_OTHER:
					if (descItem.getS() != null) {
						switch(descItem.getS()) {
						case ElzaTypes.ZP2015_SIZE_DIAMETER:
							otherSizes.add(new OtherSize("průměr",((DescriptionItemDecimal) descItem).getV()));
							break;
						case ElzaTypes.ZP2015_SIZE_PLICA:
							otherSizes.add(new OtherSize("plika (výška)",((DescriptionItemDecimal) descItem).getV()));
							break;
						case ElzaTypes.ZP2015_SIZE_MAP_WIDTH:
							otherSizes.add(new OtherSize("zrcadlo (šířka)",((DescriptionItemDecimal) descItem).getV()));
							break;
						case ElzaTypes.ZP2015_SIZE_MAP_HEIGHT:
							otherSizes.add(new OtherSize("zrcadlo (výška)",((DescriptionItemDecimal) descItem).getV()));
							break;							
						case ElzaTypes.ZP2015_SIZE_IMAGE_HEIGHT:
							otherSizes.add(new OtherSize("obrazové pole (výška)",((DescriptionItemDecimal) descItem).getV()));
							break;
						case ElzaTypes.ZP2015_SIZE_IMAGE_WIDTH:
							otherSizes.add(new OtherSize("obrazové pole (šířka)",((DescriptionItemDecimal) descItem).getV()));
							break;
						case ElzaTypes.ZP2015_SIZE_IMAGE_DIAMETER:
							otherSizes.add(new OtherSize("obrazové pole (průměr)",((DescriptionItemDecimal) descItem).getV()));
							break;
						case ElzaTypes.ZP2015_SIZE_METRAGE:
							otherSizes.add(new OtherSize("metráž",((DescriptionItemDecimal) descItem).getV()));
							break;
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

			StringJoiner sj2 = new StringJoiner(", ");
			if (sj.length() > 0) {
				sj2.add(sj.toString());												
			}
			for(var otherSize:otherSizes) {
				sj2.add(otherSize.getValue(units));
			}
			if (sj2.length() > 0) {
				ApuSourceBuilder.addString(ctx.getActivePart(), CoreTypes.SIZE, sj2.toString());	
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

	private static class OtherSize {
		
		private final String text;
		
		private final BigDecimal value;
		
		public OtherSize(String text, BigDecimal value) {
			this.text = text;
			this.value = value;
		}
		
		public String getValue(String unit) {
			if (unit == null) {
				return text + ": " + value.toString();
			} else {
				return text + ": " + value.toString() + unit;
			}
		}
		
	}
	
}
