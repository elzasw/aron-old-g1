package cz.aron.transfagent.elza;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.api.rest.model.Result;
import cz.aron.core.api.rest.model.ResultRowItem;
import cz.aron.core.api.rest.model.ResultRowItemValue;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;

@Service
@ConditionalOnProperty(value = "result.apelza", havingValue = "default")
public class ImportApResultBuilderDefault implements ImportApResultBuilder {
	
	private final ObjectMapper objectMapper;
	
	private final ApTypeService apTypeService;

	public ImportApResultBuilderDefault(ObjectMapper objectMapper, ApTypeService apTypeService) {
		this.objectMapper = objectMapper;
		this.apTypeService = apTypeService;
	}

	@Override
	public String buildResult(AccessPoint ap, ElzaNameBuilder nameBuilder) {

		var entityClass = ap.getApe().getT();
		var result = new Result();
		result.setId(ap.getApe().getUuid());
		result.setT(getType(ap));

		boolean nameFilled = false;
		boolean descriptionFilled = false;

		for (Fragment frg : ap.getFrgs().getFrg()) {
			switch (frg.getT()) {
			case "PT_NAME":
				if (!nameFilled) {
					var name = nameBuilder.createFullName(frg, entityClass, false);
					var nameItem = new ResultRowItem();
					nameItem.setT("N");
					var nameItemValue = new ResultRowItemValue();
					nameItemValue.setV(name);
					nameItem.addVItem(nameItemValue);
					nameItem.setV(Arrays.asList(nameItemValue));
					result.addLItem(Arrays.asList(nameItem));
					nameFilled = true;
				}
				break;
			case "PT_BODY":
				if (!descriptionFilled) {
					String desc = ElzaXmlReader.getStringType(frg, ElzaTypes.BRIEF_DESC);
					if (StringUtils.isNotEmpty(desc)) {
						var descItem = new ResultRowItem();
						descItem.setT("DSC");
						var descItemValue = new ResultRowItemValue();
						descItemValue.setV(desc);
						descItem.addVItem(descItemValue);
						descItem.setV(Arrays.asList(descItemValue));
						result.addLItem(Arrays.asList(descItem));
						descriptionFilled = true;
					}
				}
				break;
			default:
				// ignore
			}
		}
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	
	private String getType(AccessPoint ap) {		
		var type = ap.getApe().getT();
		if (type==null) {
			return "AP";
		}		
		var parentCode = apTypeService.getParentCode(type);
		if (parentCode==null) {
			return "AP";
		}
		switch(parentCode) {
		case "PARTY_GROUP":
			return "AP_C";
		case "PERSON":
			return "AP_P";
		case "DYNASTY":
			return "AP_F";
		case "GEO":
			return "AP_G";
		case "EVENT":
			return "AP_E";
		case "ARTWORK":
			return "AP_W";
		case "TERM":
			return "AP_T";
		default:
			return "AP";
		}
	}

}
