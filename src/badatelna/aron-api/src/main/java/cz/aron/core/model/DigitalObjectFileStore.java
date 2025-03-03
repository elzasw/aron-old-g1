package cz.aron.core.model;

import com.querydsl.core.types.Projections;
import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Repository
public class DigitalObjectFileStore extends DomainStore<DigitalObjectFile, DigitalObjectFile, QDigitalObjectFile> {

    public DigitalObjectFileStore() {
        super(DigitalObjectFile.class);
    }

    public ApuDaoFileDto findIdByPermalink(String permalink) {
        return query().select(Projections.constructor(ApuDaoFileDto.class,metaModel.digitalObject.apu.id,metaModel.digitalObject.id,metaModel.id)).from(metaModel).where(metaModel.permalink.eq(permalink)).fetchOne();
    }

}
