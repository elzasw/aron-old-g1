package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaTypes.AmountTextPos;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemDecimal;

public class EdxAmountConvertor implements EdxItemConvertor {

	private final Map<String, AmountTextPos> amountSubtypes;
	
	public EdxAmountConvertor(Map<String, AmountTextPos> amountSubtypes) {
		this.amountSubtypes = amountSubtypes;
	}
	
	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		var dd = (DescriptionItemDecimal) item;
		var amountTextPos = amountSubtypes.get(dd.getS());
		if (amountTextPos != null) {
			var value = dd.getV();
			if (value != null) {
				if (amountTextPos.isPrefix()) {
					ApuSourceBuilder.addString(ctx.getActivePart(), CoreTypes.SIZE,
							amountTextPos.getText() + value.toString());
				} else {
					ApuSourceBuilder.addString(ctx.getActivePart(), CoreTypes.SIZE,
							value.toString() + amountTextPos.getText());
				}
			}
		}
	}

}
