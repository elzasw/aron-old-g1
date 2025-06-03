package cz.aron.customizations.soaplzen;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.api.rest.model.Result;
import cz.aron.core.api.rest.model.ResultRowItem;
import cz.aron.core.api.rest.model.ResultRowItemValue;
import cz.aron.transfagent.elza.ElzaNameBuilder;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.elza.ImportApResultBuilder;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportApResultBuilderImp implements ImportApResultBuilder {

	private final ObjectMapper objectMapper;

	public ImportApResultBuilderImp(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String buildResult(AccessPoint ap, ElzaNameBuilder nameBuilder) {

		var entityClass = ap.getApe().getT();
		var result = new Result();
		result.setId(ap.getApe().getUuid());
		result.setT(entityClass);

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

}
