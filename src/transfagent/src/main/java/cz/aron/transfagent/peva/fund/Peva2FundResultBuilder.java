package cz.aron.transfagent.peva.fund;

import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.transfagent.transformation.InstitutionInfo;

public interface Peva2FundResultBuilder {

	String buildResult(NadSheet nadSheet, InstitutionInfo institutionInfo);
	
}
