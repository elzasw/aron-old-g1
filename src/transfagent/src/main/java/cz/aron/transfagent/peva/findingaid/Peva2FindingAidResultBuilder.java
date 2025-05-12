package cz.aron.transfagent.peva.findingaid;

import cz.aron.peva2.wsdl.FindingAid;
import cz.aron.transfagent.transformation.InstitutionInfo;

public interface Peva2FindingAidResultBuilder {

	String buildResult(FindingAid findingAid, InstitutionInfo instInfo);
	
}
