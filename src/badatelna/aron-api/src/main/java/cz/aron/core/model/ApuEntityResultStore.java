package cz.aron.core.model;

import org.springframework.stereotype.Repository;

import cz.inqool.eas.common.domain.store.DomainStore;

@Repository
public class ApuEntityResultStore extends DomainStore<ApuEntityResult, ApuEntityResult, QApuEntityResult> {
    public ApuEntityResultStore() {
        super(ApuEntityResult.class);
    }
}
