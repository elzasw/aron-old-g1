package cz.aron.transfagent.elza;

import cz.tacr.elza.schema.v2.AccessPoint;

public interface ImportInstitutionResultBuilder {
	
	String buildResult(AccessPoint ap, ElzaNameBuilder nameBuilder);

}
