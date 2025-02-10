package cz.aron.core.model;

import cz.inqool.eas.common.domain.store.DomainStore;

import java.util.List;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class ApuAttachmentStore extends DomainStore<ApuAttachment, ApuAttachment, QApuAttachment> {

	public ApuAttachmentStore() {
        super(ApuAttachment.class);
    }

	public List<String> findFileIdsByName(String name) {
		return query().select(metaModel.file.file.id).from(metaModel).where(metaModel.name.eq(name)).fetch();
	}

}