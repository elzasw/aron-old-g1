package cz.aron.transfagent.elza.dao;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.transfagent.service.DaoFileStore4Service;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.Level;

@ConditionalOnProperty(value = "filestore4.path")
@Service
public class LevelDaoImporterFileStore4 implements ArchDescLevelDaoImporter {

    private final DaoFileStore4Service fileStore4;

    public LevelDaoImporterFileStore4(DaoFileStore4Service fileStore4) {
        this.fileStore4 = fileStore4;
    }

    @Override
    public int importDaos(String institutionCode, int fundCode, Level lvl, Apu apu, ContextDataProvider dataProvider, DaoRefRegistration daoRefReg) {
        String handle = fileStore4.getDaoHandle(institutionCode, fundCode, lvl.getUuid());
        if (handle!=null) {
            UUID uuid = UUID.fromString(lvl.getUuid());
            ApuSourceBuilder.addDao(apu, uuid);
            daoRefReg.registerDao(fileStore4.getName(), handle, uuid);
            return 1;
        }
        return 0;
    }

}
