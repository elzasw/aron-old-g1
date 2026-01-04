package cz.aron.transfagent.elza;

import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.tacr.elza.schema.v2.Section;

/**
 * Interface for generating names for APUs.
 */
public interface ImportArchDescNameBuilder {

	/**
	 * Generate name for APU
	 */
	String getName(Section sect, LevelContext levelContext, String institutionName, String institutionShortName, ElzaXmlReader elzaXmlReader);
	
}
