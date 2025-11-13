package cz.aron.customizations.generic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.customizations.soaplzen.PevaFundResultBuilderImpl;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "result.fundpeva", havingValue = "default")
public class Peva2FundResultBuilderDefault extends PevaFundResultBuilderImpl {

	public Peva2FundResultBuilderDefault(ObjectMapper objectMapper, ArchivalEntityRepository archivalEntityRepository,
			StorageService storageService) {
		super(objectMapper, archivalEntityRepository, storageService);
	}

}
