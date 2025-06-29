package cz.aron.transfagent.service.importfromdir;

import cz.aron.transfagent.domain.ApuSource;

public interface ReimportProcessor {
    
    enum Result {
        REIMPORTED,
        NOCHANGES,
        UNSUPPORTED,
        FAILED,
        DELETED
    };

	/**
	 * Reimport apu
	 * @param apuSource
	 * @return Return if apuSource was reimported
	 * 
	 * Insert into CoreQueue when REIMPORTED result is returned
	 */
	Result reimport(ApuSource apuSource);

}
