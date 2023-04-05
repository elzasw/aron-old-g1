package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.DaoDef;
import cz.aron.api.rest.model.DaoStateDef;
import cz.aron.transfagent.domain.Dao;

public class DaoDefVO extends DaoDef {

    public DaoDefVO(Dao dao) {
        setApusourceId(dao.getApuSource().getId().longValue());
        setDaoId(dao.getId().longValue());
        setDataDir(dao.getDataDir());
        setHandle(dao.getDataDir());
        setSource(dao.getSource());
        switch (dao.getState()) {
        case READY:
            setState(DaoStateDef.READY);
            break;
        case ACCESSIBLE:
            setState(DaoStateDef.ACCESSIBLE);
            break;
        case INACCESSIBLE:
            setState(DaoStateDef.INACCESSIBLE);
            break;
        }
        setTransferred(dao.isTransferred());
        setUuid(dao.getUuid().toString());
    }

}
