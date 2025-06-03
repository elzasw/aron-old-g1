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
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.transfagent.peva.Peva2Utils;
import cz.aron.transfagent.peva.fund.Peva2FundResultBuilder;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.InstitutionInfo;

@ConditionalOnProperty(value = "filestore5.path")
@Service
public class PevaFundResultBuilderImpl implements Peva2FundResultBuilder {

	private final ObjectMapper objectMapper;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final StorageService storageService;

	public PevaFundResultBuilderImpl(ObjectMapper objectMapper, ArchivalEntityRepository archivalEntityRepository,
			StorageService storageService) {
		this.objectMapper = objectMapper;
		this.archivalEntityRepository = archivalEntityRepository;
		this.storageService = storageService;
	}

	@Override
	public String buildResult(NadSheet nadSheet, InstitutionInfo institutionInfo) {
		var result = new Result();
		result.setId(nadSheet.getId());
		result.setT("F");
		generateFundName(nadSheet, result);
		generateDate(nadSheet, result);
		generateInstitution(nadSheet, result, institutionInfo);
		
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateFundName(NadSheet nadSheet, Result result) {
		var rowItem = new ResultRowItem();
		rowItem.setT("N");
		result.addLItem(Arrays.asList(rowItem));
		
		var fundName = nadSheet.getHeader().getName();
		var itemValue = new ResultRowItemValue();
		itemValue.setV(StringUtils.abbreviate(fundName, 200));
		rowItem.addVItem(itemValue);
	}
	
	public void generateDate(NadSheet nadSheet, Result result) {
		var timeRange = nadSheet.getHeader().getTimeRange();
		if (timeRange!=null) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(Peva2Utils.getAsString(timeRange));			
			var rowItem = new ResultRowItem();
			rowItem.setT("D");
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));			
		}
	}
	
	public void generateInstitution(NadSheet nadSheet, Result result, InstitutionInfo institutionInfo) {
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
