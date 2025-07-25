package cz.aron.transfagent.elza;

import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Section;

/**
 * Interface for generating names for APUs.
 */
public interface ImportArchDescNameBuilder {

	/**
	 * Generate name for APU
	 */
	String getName(Section sect, Level lvl, LevelContext parentContext, String institutionName, String institutionShortName);
	
}
