package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.EntityDef;
import cz.aron.api.rest.model.EntityStateDef;
import cz.aron.transfagent.domain.ArchivalEntity;

public class EntityDefVO extends EntityDef {

    public EntityDefVO(ArchivalEntity archivalEntity) {
        if (archivalEntity.getApuSource()!=null) {
            setApusourceId(archivalEntity.getApuSource().getId().longValue());
        }
        if (archivalEntity.getElzaId()!=null) {
            setElzaId(archivalEntity.getElzaId().toString());
        }
        setEntityClass(archivalEntity.getEntityClass());
        setEntityId(archivalEntity.getId().longValue());
        if (archivalEntity.getParentEntity()!=null) {
            setParentId(archivalEntity.getParentEntity().getId().longValue());
        }
        switch(archivalEntity.getStatus()) {
        case ACCESSIBLE:
            setState(EntityStateDef.ACCESSIBLE);
            break;
        case AVAILABLE:
            setState(EntityStateDef.AVAILABLE);
            break;
        case NOT_ACCESSIBLE:
            setState(EntityStateDef.NOT_ACCESSIBLE);
            break;
        case NOT_AVAILABLE:
            setState(EntityStateDef.NOT_AVAILABLE);
            break;
        default:
            // ignore
        }
        if (archivalEntity.getUuid()!=null) {
        	setUuid(archivalEntity.getUuid().toString());
        }
    }
    
}
