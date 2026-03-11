package cz.aron.customizations.soahk;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.transfagent.elza.dao.ArchDescLevelDaoImporter;
import cz.aron.transfagent.elza.dao.DaoRefRegistration;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.Level;

@ConditionalOnProperty(value = "filestore.path")
@Service
public class LevelDaoImporterFileStore implements ArchDescLevelDaoImporter {
	
	private final DaoFileStoreService daoFileStoreService;
	
	public LevelDaoImporterFileStore(DaoFileStoreService daoFileStoreService) {
		this.daoFileStoreService = daoFileStoreService;
	}

	@Override
	public int importDaos(String institutionCode, int fundCode, Level lvl, Apu apu, ContextDataProvider dataProvider,
			DaoRefRegistration daoRefReg) {		
		if (daoFileStoreService.getDaoDir(lvl.getUuid()) != null) {
			ApuSourceBuilder.addDao(apu, UUID.fromString(lvl.getUuid()));
			daoRefReg.registerDao(DaoFileStoreService.SERVICE_NAME, lvl.getUuid(), UUID.fromString(lvl.getUuid()));			
			return 1;			
		} else {
			return 0;	
		}		
	}

}
