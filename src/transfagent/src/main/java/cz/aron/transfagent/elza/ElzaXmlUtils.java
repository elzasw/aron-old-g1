package cz.aron.transfagent.elza;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public final class ElzaXmlUtils {

	private ElzaXmlUtils() {
	}

	/**
	 * Popisny prvek nese pouzitelnou hodnotu, tj. je to string a neni prazdny.
	 * Prazdna hodnota se nesmi zapsat, jinak by zablokovala dedeni neprazdne
	 * hodnoty z nadrazene urovne.
	 */
	public static boolean hasContent(DescriptionItem pp) {
		return (pp instanceof DescriptionItemString) && StringUtils.isNotBlank(((DescriptionItemString) pp).getV());
	}
}
