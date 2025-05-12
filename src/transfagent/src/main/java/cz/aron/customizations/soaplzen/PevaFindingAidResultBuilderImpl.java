package cz.aron.customizations.soaplzen;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.api.rest.model.Result;
import cz.aron.core.api.rest.model.ResultRowItem;
import cz.aron.core.api.rest.model.ResultRowItemValue;
import cz.aron.peva2.wsdl.FindingAid;
import cz.aron.transfagent.peva.findingaid.Peva2FindingAidResultBuilder;
import cz.aron.transfagent.transformation.InstitutionInfo;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class PevaFindingAidResultBuilderImpl implements Peva2FindingAidResultBuilder {

	private final ObjectMapper objectMapper;

	public PevaFindingAidResultBuilderImpl(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String buildResult(FindingAid findingAid, InstitutionInfo instInfo) {
		var result = new Result();
		result.setId(findingAid.getId());
		result.setT("FA");

		var rowItem = new ResultRowItem();
		rowItem.setT("name");
		result.addLItem(Arrays.asList(rowItem));

		var fundName = findingAid.getName();
		var itemValue = new ResultRowItemValue();
		itemValue.setV(fundName);
		rowItem.addVItem(itemValue);

		var instName = instInfo.getShortName();
		rowItem = new ResultRowItem();
		rowItem.setT("INST_REF");
		result.addLItem(Arrays.asList(rowItem));
		itemValue = new ResultRowItemValue();
		itemValue.setV(instName);
		itemValue.id(instInfo.getUuid().toString());
		rowItem.addVItem(itemValue);				
		
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}
	
}
