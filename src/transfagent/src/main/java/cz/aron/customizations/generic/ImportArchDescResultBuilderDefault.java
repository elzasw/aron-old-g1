package cz.aron.customizations.generic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.customizations.soaplzen.ImportArchDescResultBuilderImpl;
import cz.aron.transfagent.repository.ArchivalEntityRepository;

@Service
@ConditionalOnProperty(value = "result.archdescelza", havingValue = "default")
public class ImportArchDescResultBuilderDefault extends ImportArchDescResultBuilderImpl {

	public ImportArchDescResultBuilderDefault(ObjectMapper objectMapper, ArchivalEntityRepository aeRepository,
			ThumbnailFileStoreI thumbnailFileStore) {
		super(objectMapper, aeRepository, thumbnailFileStore);
	}

}
