package cz.aron.customizations.generic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.customizations.soaplzen.PevaFindingAidResultBuilderImpl;
import cz.aron.transfagent.config.ConfigPeva2;

@Service
@ConditionalOnProperty(value = "result.findingaidpeva", havingValue = "default")
public class Peva2FindingAidResultBuilderDefault extends PevaFindingAidResultBuilderImpl {

	public Peva2FindingAidResultBuilderDefault(ObjectMapper objectMapper, ConfigPeva2 configPeva2) {
		super(objectMapper, configPeva2);
	}

}
