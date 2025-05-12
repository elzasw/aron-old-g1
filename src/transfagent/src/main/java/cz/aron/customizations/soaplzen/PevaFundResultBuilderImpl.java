package cz.aron.customizations.soaplzen;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.api.rest.model.Result;
import cz.aron.core.api.rest.model.ResultRowItem;
import cz.aron.core.api.rest.model.ResultRowItemValue;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.transfagent.peva.fund.Peva2FundResultBuilder;
import cz.aron.transfagent.transformation.InstitutionInfo;

@ConditionalOnProperty(value = "filestore5.path")
@Service
public class PevaFundResultBuilderImpl implements Peva2FundResultBuilder {

	private final ObjectMapper objectMapper;

	public PevaFundResultBuilderImpl(ObjectMapper objectMapper) {
		super();
		this.objectMapper = objectMapper;
	}

	@Override
	public String buildResult(NadSheet nadSheet, InstitutionInfo institutionInfo) {

		var result = new Result();
		result.setId(nadSheet.getId());
		result.setT("F");

		var rowItem = new ResultRowItem();
		rowItem.setT("name");
		result.addLItem(Arrays.asList(rowItem));

		var fundName = nadSheet.getHeader().getName();
		var itemValue = new ResultRowItemValue();
		itemValue.setV(fundName);
		rowItem.addVItem(itemValue);

		var instName = institutionInfo.getShortName();
		rowItem = new ResultRowItem();
		rowItem.setT("INST_REF");
		result.addLItem(Arrays.asList(rowItem));
		itemValue = new ResultRowItemValue();
		itemValue.setV(instName);
		itemValue.id(institutionInfo.getUuid().toString());
		rowItem.addVItem(itemValue);				
		
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
