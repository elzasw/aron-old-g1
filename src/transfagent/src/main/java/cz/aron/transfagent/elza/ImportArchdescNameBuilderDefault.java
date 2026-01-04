package cz.aron.transfagent.elza;

import org.apache.commons.lang3.StringUtils;

import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.Section;

/**
 * Default implementation of {@link ImportArchDescNameBuilder}.
 */
public class ImportArchdescNameBuilderDefault implements ImportArchDescNameBuilder {

	private final ConfigElzaArchDesc configArchDesc;

	public ImportArchdescNameBuilderDefault(ConfigElzaArchDesc configArchDesc) {
		this.configArchDesc = configArchDesc;
	}

	@Override
	public String getName(Section sect, LevelContext levelContext, String institutionName,
			String institutionShortName, ElzaXmlReader elzaXmlReader) {
		var lvl = levelContext.getLevel();
		String parentId = lvl.getPid();

		StringBuilder sb = new StringBuilder();

		if (configArchDesc.isComposedShortName()) {
			sb.append(institutionShortName).append(": ");
		} else {
			sb.append(institutionName).append(": ");
		}
		sb.append(sect.getFi().getN());

		if (parentId == null) {
			// Koren AS je bez dalsich identifikatoru
			return sb.toString();
		}

		String refOzn = null;
		String invCislo = null;
		String poradoveCislo = null;
		String otherIdent = null;
		String otherIdentType = null;
		int otherIdentPriority = -1;
		String datace = null;
		for (DescriptionItem item : lvl.getDdOrDoOrDp()) {
			if (item.getT().equals("ZP2015_UNIT_ID") && (item instanceof DescriptionItemString)) {
				DescriptionItemString title = (DescriptionItemString) item;
				refOzn = title.getV();
			} else if (item.getT().equals("ZP2015_INV_CISLO") && (item instanceof DescriptionItemString)) {
				DescriptionItemString title = (DescriptionItemString) item;
				invCislo = title.getV();
			} else if (item.getT().equals("ZP2015_SERIAL_NUMBER") && (item instanceof DescriptionItemInteger)) {
				DescriptionItemInteger serNum = (DescriptionItemInteger) item;
				poradoveCislo = serNum.getV().toString();
			} else if (item.getT().equals(ElzaTypes.ZP2015_OTHER_ID) && (item instanceof DescriptionItemString)) {
				DescriptionItemString otherId = (DescriptionItemString) item;
				if (ImportArchDesc.otherIdPriorityMap.get(otherId.getS()) > otherIdentPriority) {
					otherIdentPriority = ImportArchDesc.otherIdPriorityMap.get(otherId.getS());
					otherIdentType = ElzaTypes.otherIdNameMap.get(otherId.getS());
					otherIdent = otherId.getV();
				}
			}
			if (item.getT().equals(ElzaTypes.ZP2015_UNIT_DATE) && (item instanceof DescriptionItemUnitDate)) {
				DescriptionItemUnitDate dataceUnitDate = (DescriptionItemUnitDate) item;
				datace = UnitDateConvertor.convertToString(dataceUnitDate);
			}
		}

		if (StringUtils.isBlank(datace)) {
			datace = ImportArchDesc.inheritDatace(levelContext.getParentContext());
		}

		if (StringUtils.isNotEmpty(refOzn)) {
			sb.append(", ").append(refOzn);
		} else if (StringUtils.isNotEmpty(invCislo)) {
			sb.append(", inv. č.: ").append(invCislo);
		} else if (StringUtils.isNotEmpty(poradoveCislo)) {
			sb.append(", poř. č.: ").append(poradoveCislo);
		} else if (StringUtils.isNotEmpty(otherIdent)) {
			if (otherIdentType != null) {
				sb.append(", ").append(otherIdentType);
			}
			sb.append(": ").append(otherIdent);
		} else {
			// TODO: stanoveni cesty pomoci hierarchie
			// var parentApu = this.apuMap.get(lvl.getPid());
			// while(parentApu!=null) {
			// apuPosMap.get()
			// }

			// Variantne lze pridat UUID jako ident
			// sb.append(lvl.getUuid().toString());
		}
		if (StringUtils.isNotBlank(datace)) {
			sb.append(", ").append(datace);
		}
		return sb.toString();
	}

}
