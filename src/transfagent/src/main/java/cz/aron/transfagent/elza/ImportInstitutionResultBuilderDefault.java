package cz.aron.transfagent.elza;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.api.rest.model.Result;
import cz.aron.core.api.rest.model.ResultRowItem;
import cz.aron.core.api.rest.model.ResultRowItemValue;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;

@Service
@ConditionalOnProperty(value = "result.institution", havingValue = "default")
public class ImportInstitutionResultBuilderDefault implements ImportInstitutionResultBuilder {
	
	private final ObjectMapper objectMapper;
	
	private final ApTypeService apTypeService;
	
	public ImportInstitutionResultBuilderDefault(ObjectMapper objectMapper, ApTypeService apTypeService) {
		this.objectMapper = objectMapper;
		this.apTypeService = apTypeService;
	}

	@Override
	public String buildResult(AccessPoint ap, ElzaNameBuilder nameBuilder) {
		
		Fragment prefName = null;
		Fragments frgs = ap.getFrgs();		
		for(Fragment frg: frgs.getFrg()) {
			if(frg.getT().equals("PT_NAME")) {
				if(prefName==null) {
					prefName = frg;
				}				
			}
		}

		var result = new Result();
		result.setId(ap.getApe().getUuid());
		result.setT("I");
		
		var fullName = ElzaXmlReader.getFullName(prefName);
		
		var nameItem = new ResultRowItem();
		nameItem.setT("N");
		var nameItemValue = new ResultRowItemValue();
		nameItemValue.setV(fullName);
		nameItem.addVItem(nameItemValue);
		nameItem.setV(Arrays.asList(nameItemValue));
		result.addLItem(Arrays.asList(nameItem));
		
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}

}
