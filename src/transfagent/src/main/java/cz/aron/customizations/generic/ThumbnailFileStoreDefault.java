package cz.aron.customizations.generic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.core.api.rest.model.Result;
import cz.aron.customizations.soaplzen.ImportArchDescResultBuilderImpl.ThumbnailFileStoreI;

@Service
@ConditionalOnProperty(value = "result.archdescelza", havingValue = "default")
public class ThumbnailFileStoreDefault implements ThumbnailFileStoreI {

	@Override
	public void fillThumbnailLink(String daoLink, Result result) {
		// do nothing		
	}

}
