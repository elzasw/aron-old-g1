package cz.aron.transfagent.elza;

import cz.aron.transfagent.ead3.Ead3XmlReader;
import cz.aron.transfagent.transformation.InstitutionInfo;

public interface ImportFindingAidResultBuilder {
	
	String buildResult(Ead3XmlReader ead3XmlReader, InstitutionInfo institutionInfo, String uuid);

}
