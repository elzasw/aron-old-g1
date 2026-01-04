package cz.aron.customizations.generic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.customizations.soaplzen.ImportArchDescDescBuilderImpl;
import cz.aron.transfagent.config.ConfigElzaArchDesc;

@Service
@ConditionalOnProperty(value = "result.archdescdescbuilderelza", havingValue = "default")
public class ImportArchDescDescBuilderDefault extends ImportArchDescDescBuilderImpl {
	
	public ImportArchDescDescBuilderDefault(ConfigElzaArchDesc configArchDesc) {
		super(configArchDesc);
	}

}
