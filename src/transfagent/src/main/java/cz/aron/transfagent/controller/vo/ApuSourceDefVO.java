package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.ApuSourceDef;
import cz.aron.api.rest.model.ApuSourceType;
import cz.aron.transfagent.domain.ApuSource;

public class ApuSourceDefVO extends ApuSourceDef {

    public ApuSourceDefVO(ApuSource apuSource) {
        setApusourceId(apuSource.getId().longValue());
        setDataDir(apuSource.getDataDir());
        setDeleted(apuSource.isDeleted());
        setImported(apuSource.getDateImported().toOffsetDateTime());
        setReimport(apuSource.isReimport());
        switch(apuSource.getSourceType()) {
        case ARCH_DESCS:
            setSourceType(ApuSourceType.ARCH_DESCS);
            break;
        case ARCH_ENTITY:
            setSourceType(ApuSourceType.ARCH_ENTITY);
            break;
        case COLLECTION:
            setSourceType(ApuSourceType.COLLECTION);
            break;
        case DIRECT:
            setSourceType(ApuSourceType.DIRECT);
            break;
        case FINDING_AID:
            setSourceType(ApuSourceType.FINDING_AID);
            break;
        case FUND:
            setSourceType(ApuSourceType.FUND);
            break;
        case INSTITUTION:
            setSourceType(ApuSourceType.INSTITUTION);
            break;
        }
        setUuid(apuSource.getUuid().toString());
    }

}
