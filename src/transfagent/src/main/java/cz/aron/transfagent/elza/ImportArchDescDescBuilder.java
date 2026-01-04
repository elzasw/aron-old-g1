package cz.aron.transfagent.elza;

import java.util.Set;

import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;

/**
 * Interface for generating description
 */
public interface ImportArchDescDescBuilder {

	String getDesc(LevelContext levelContext, Set<String> levelDuplicities, ElzaXmlReader elzaXmlReader);
	
}
