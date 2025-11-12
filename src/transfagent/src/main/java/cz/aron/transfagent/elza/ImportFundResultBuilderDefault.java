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
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.tacr.elza.schema.v2.FundInfo;

@Service
@ConditionalOnProperty(value = "result.fundelza", havingValue = "default")
public class ImportFundResultBuilderDefault implements ImportFundResultBuilder {
	
	private final ObjectMapper objectMapper;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final StorageService storageService;

	public ImportFundResultBuilderDefault(ObjectMapper objectMapper, ArchivalEntityRepository archivalEntityRepository,
			StorageService storageService) {
		this.objectMapper = objectMapper;
		this.archivalEntityRepository = archivalEntityRepository;
		this.storageService = storageService;
	}
	
	public String buildResult(FundInfo fi,InstitutionInfo institutionInfo, String uuid) {
		var result = new Result();
		result.setId(uuid);
		result.setT("F");
		generateFundName(fi, result);
		generateDate(fi, result);
		generateInstitution(fi, result, institutionInfo);
		
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void generateFundName(FundInfo fi, Result result) {		
		var rowItem = new ResultRowItem();
		rowItem.setT("N");
		result.addLItem(Arrays.asList(rowItem));
		
		var fundName = fi.getN();
		var itemValue = new ResultRowItemValue();
		itemValue.setV(StringUtils.abbreviate(fundName, 200));
		rowItem.addVItem(itemValue);
    }
	
	public void generateDate(FundInfo fi, Result result) {
		var timeRange = fi.getTr();
		if (timeRange!=null) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(timeRange);			
			var rowItem = new ResultRowItem();
			rowItem.setT("D");
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));			
		}
	}
	
	public void generateInstitution(FundInfo fi, Result result, InstitutionInfo institutionInfo) {
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
