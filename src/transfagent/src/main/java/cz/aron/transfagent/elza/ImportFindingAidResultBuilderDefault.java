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
import cz.aron.transfagent.ead3.Ead3XmlReader;
import cz.aron.transfagent.transformation.InstitutionInfo;

@Service
@ConditionalOnProperty(value = "result.findingaidelza", havingValue = "default")
public class ImportFindingAidResultBuilderDefault implements ImportFindingAidResultBuilder {
	
	private final ObjectMapper objectMapper;
	
	public ImportFindingAidResultBuilderDefault(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String buildResult(Ead3XmlReader ead3XmlReader, InstitutionInfo institutionInfo, String uuid) {
		var result = new Result();
		result.setId(uuid);
		result.setT("FI");
		generateFindingAidName(ead3XmlReader, result);
		generateDate(ead3XmlReader, result);
		generateInstitution(institutionInfo, result);
		
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void generateFindingAidName(Ead3XmlReader ead3XmlReader, Result result) {		
		var rowItem = new ResultRowItem();
		rowItem.setT("N");
		result.addLItem(Arrays.asList(rowItem));
		
		var fundName = ead3XmlReader.getSubtitle();
		var itemValue = new ResultRowItemValue();
		itemValue.setV(StringUtils.abbreviate(fundName, 200));
		rowItem.addVItem(itemValue);
    }

	public void generateDate(Ead3XmlReader ead3XmlReader, Result result) {
		var dateRange = ead3XmlReader.getLocalionControlByType("DATE_RANGE");
		if (dateRange!=null) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(dateRange);			
			var rowItem = new ResultRowItem();
			rowItem.setT("D");
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));			
		}
	}
	
	public void generateInstitution(InstitutionInfo institutionInfo, Result result) {
		var instName = institutionInfo.getShortName();
		var rowItem = new ResultRowItem();
		rowItem.setT("IR");
		result.addLItem(Arrays.asList(rowItem));
		var itemValue = new ResultRowItemValue();
		itemValue.setV(instName);
		itemValue.id(institutionInfo.getUuid().toString());
		rowItem.addVItem(itemValue);						
	}

}
