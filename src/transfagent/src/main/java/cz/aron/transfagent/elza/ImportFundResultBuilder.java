package cz.aron.transfagent.elza;

import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.tacr.elza.schema.v2.FundInfo;

public interface ImportFundResultBuilder {
	
	String buildResult(FundInfo fi,InstitutionInfo institutionInfo, String uuid);

}
