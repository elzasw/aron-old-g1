package cz.aron.transfagent.elza;

import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Section;

public interface ImportArchDescResultBuilder {

	String buildResult(InstitutionInfo instInfo,Section sect, Level lvl, LevelContext parentContext);

}
