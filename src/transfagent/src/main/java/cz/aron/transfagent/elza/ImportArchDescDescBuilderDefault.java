package cz.aron.transfagent.elza;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;

/**
 * Default implementation of {@link ImportArchdescDescNameBuilder}.
 */
public class ImportArchDescDescBuilderDefault implements ImportArchDescDescBuilder {
	
	private final ConfigElzaArchDesc configArchDesc;
	
	public ImportArchDescDescBuilderDefault(ConfigElzaArchDesc configArchDesc) {
		this.configArchDesc = configArchDesc;
	}

	@Override
	public String getDesc(LevelContext levelContext, Set<String> levelDuplicities, ElzaXmlReader elzaXmlReader) {
		if (levelContext.getLevel().getPid() == null) {
            // for root no description
            return null;
        }

        String titleStr = null;
        String datace = null;
        StringBuilder sb = new StringBuilder();
        for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
            if (titleStr==null&&item.getT().equals("ZP2015_TITLE")) {
                DescriptionItemString title = (DescriptionItemString) item;
                titleStr = title.getV();
            }
            if (titleStr==null&&item.getT().equals("ZP2015_CONTENT")) {
                DescriptionItemString title = (DescriptionItemString) item;
                titleStr = title.getV();
            }
            if (item.getT().equals(ElzaTypes.ZP2015_UNIT_DATE) && (item instanceof DescriptionItemUnitDate)) {
                DescriptionItemUnitDate dataceUnitDate = (DescriptionItemUnitDate) item;
                datace = UnitDateConvertor.convertToString(dataceUnitDate);
            }
        }

        boolean titleInherited = false;
        boolean dateInherited = false;
        // inherit title and name when null
        if (StringUtils.isBlank(titleStr)) {        	
            titleStr = ImportArchDesc.inheritTitle(levelContext.getParentContext());
            titleInherited = true;
        }
        if (StringUtils.isBlank(datace) && configArchDesc.isInheritNameDate()) {
            datace = ImportArchDesc.inheritDatace(levelContext.getParentContext());
            dateInherited = true;
        }

        if (StringUtils.isNotBlank(titleStr)) {
            sb.append(titleStr);
        }
		if (StringUtils.isNotBlank(datace)
				&& (configArchDesc.isAddDateToName() || (configArchDesc.isInheritNameDate() && titleInherited))) {
			sb.append(", ").append(datace);
		}
		
		if (levelDuplicities != null) {
			var rawDesc = sb.toString();
			if (levelDuplicities.contains(rawDesc) && StringUtils.isNotBlank(datace) && dateInherited == false
					&& !configArchDesc.isAddDateToName()) {
				sb.append(", ").append(datace);
			}
		}

        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();		
	}

}
