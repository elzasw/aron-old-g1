package cz.aron.core.model;

import com.querydsl.jpa.impl.JPAQuery;
import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;
import javax.persistence.EntityGraph;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Repository
public class DigitalObjectStore extends DomainStore<DigitalObject, DigitalObject, QDigitalObject> {

    public DigitalObjectStore() {
        super(DigitalObject.class);
    }

    public long disconnectDaosByApuSourceId(String id) {
        long numModified = entityManager.createQuery("UPDATE DigitalObject do SET do.apu=NULL WHERE do IN (SELECT do FROM DigitalObject do INNER JOIN ApuEntity ae ON do.apu=ae WHERE ae.source.id=?1)")
        .setParameter(1,id).executeUpdate();
        entityManager.flush();
        return numModified;
    }

    /**
     * Precte DigitalObject a vazby az k metadatum
     */
    public DigitalObject findFetchAll(String id) {    	
    	    	
        JPAQuery<DigitalObject> query = query().
                select(metaModel).
                from(metaModel)
                .where(domainMetaModel.id.eq(id));

        DigitalObject entity = query.fetchFirst();
        if (entity != null) {
        	/*
        	 * Docteni metadat v ramci transakce 
        	 */
        	for(var file:entity.getFiles()) {
        		file.getMetadata().size();
        	}
            detachAll();
        }
        return entity;
    }


}
