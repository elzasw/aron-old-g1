package cz.aron.customizations.soaplzen;

import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.elza.ImportArchDescNameBuilder;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Section;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportArchDescNameBuilderImpl implements ImportArchDescNameBuilder {

	@Override
	public String getName(Section sect, Level lvl, LevelContext parentContext, String institutionName,
			String institutionShortName) {
		boolean root = false;
		boolean matrika = false;
		String title = null;
		for (var pp : lvl.getDdOrDoOrDp()) {
			if (ElzaTypes.ZP2015_LEVEL_TYPE.equals(pp.getT())) {
				DescriptionItemEnum levelType = (DescriptionItemEnum) pp;
				if (ElzaTypes.ZP2015_LEVEL_ROOT.equals(levelType.getS())) {
					root = true;
					break;
				}
			} else if ("ZP2015_TITLE".equals(pp.getT())) {
				title = ((DescriptionItemString) pp).getV();
			} else if (ElzaTypes.ZP2015_UNIT_SUBTYPE.equals(pp.getT()) && "ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
				matrika = true;
			}
		}
		if (title == null) {
			title = ImportArchDesc.inheritTitle(parentContext);
		}
		StringBuilder sb = new StringBuilder();
		if (root) {
			// for root use name of fund
			sb.append(institutionShortName).append(": ");
			sb.append(sect.getFi().getN());
		} else {
			sb.append(title);
		}
		return sb.toString();
	}

}
