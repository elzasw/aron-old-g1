package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;

public class EdxLevelTypeConvertor implements EdxItemConvertor {

	private static final String LEVEL_TYPE_GROUP_ARCHIVALIE = "archiválie";
	private static final String LEVEL_TYPE_GROUP_SERIE_ARCHIVALIE = "série archiválií";

	private static final String LEVEL_TYPE_ROOT = "kořen archivního souboru";
	private static final String LEVEL_TYPE_SERIES = "série archiválií";
	private static final String LEVEL_TYPE_FOLDER = "složka";
	private static final String LEVEL_TYPE_ITEM = "jednotlivost";
	private static final String LEVEL_TYPE_PART = "část jednotlivosti";

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		String levelType = null;
		String levelTypeGroup = null;
		var itemEnum = (DescriptionItemEnum) item;

		switch (itemEnum.getS()) {
		case ElzaTypes.ZP2015_LEVEL_ROOT:
			levelType = LEVEL_TYPE_ROOT;
			levelTypeGroup = LEVEL_TYPE_GROUP_SERIE_ARCHIVALIE;
			break;
		case ElzaTypes.ZP2015_LEVEL_SERIES:
			levelType = LEVEL_TYPE_SERIES;
			levelTypeGroup = LEVEL_TYPE_GROUP_SERIE_ARCHIVALIE;
			break;
		case ElzaTypes.ZP2015_LEVEL_FOLDER:
			levelType = LEVEL_TYPE_FOLDER;
			levelTypeGroup = LEVEL_TYPE_GROUP_ARCHIVALIE;
			break;
		case ElzaTypes.ZP2015_LEVEL_ITEM:
			levelType = LEVEL_TYPE_ITEM;
			levelTypeGroup = LEVEL_TYPE_GROUP_ARCHIVALIE;
			break;
		case ElzaTypes.ZP2015_LEVEL_PART:
			levelType = LEVEL_TYPE_PART;
			levelTypeGroup = LEVEL_TYPE_GROUP_ARCHIVALIE;
			break;
		}
		if (levelType != null) {			
			ApuSourceBuilder.addEnum(ctx.getActivePart(), CoreTypes.LEVEL_TYPE, levelType, false);
		}
		if (levelTypeGroup != null) {
			ApuSourceBuilder.addEnum(ctx.getActivePart(), CoreTypes.LEVEL_TYPE_GROUP, levelTypeGroup, false);
		}
	}

}
