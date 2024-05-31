package cz.aron.core.model;

import org.springframework.stereotype.Repository;

import cz.inqool.eas.common.domain.store.DomainStore;

@Repository
public class ApuEntitySimpleStore extends DomainStore<ApuEntitySimple, ApuEntitySimple, QApuEntitySimple> {
    public ApuEntitySimpleStore() {
        super(ApuEntitySimple.class);
    }
}
