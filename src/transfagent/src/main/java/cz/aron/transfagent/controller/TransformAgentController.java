package cz.aron.transfagent.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.api.rest.TransfagentApi;
import cz.aron.api.rest.model.ApuSourceDef;
import cz.aron.api.rest.model.ApuSourceResponse;
import cz.aron.api.rest.model.ArchDescResponse;
import cz.aron.api.rest.model.DaoDef;
import cz.aron.api.rest.model.DaosResponse;
import cz.aron.api.rest.model.EntitiesResponse;
import cz.aron.api.rest.model.FindingAidsResponse;
import cz.aron.api.rest.model.FundsResponse;
import cz.aron.api.rest.model.InstitutionResponse;
import cz.aron.api.rest.model.ReimportType;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ArchDescService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.DaoImportService;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.FundService;
import cz.aron.transfagent.service.InstitutionsService;

@CrossOrigin
@RestController
@RequestMapping("/api")
@ConditionalOnProperty(value="ui.enabled", havingValue = "true")
public class TransformAgentController implements TransfagentApi {
	
	private static final Logger log = LoggerFactory.getLogger(TransformAgentController.class);

    private final ApuSourceService apuSourceService;

    private final DaoImportService daoImportService;

    private final FundService fundService;

    private final FindingAidService findingAidService;

    private final ArchivalEntityService archivalEntityService;
    
    private final ArchDescService archDescService;
    
    private final InstitutionsService institutionsService;

    public TransformAgentController(ApuSourceService apuSourceService, 
    							    DaoImportService daoImportService,
                                    FundService fundService,
                                    FindingAidService findingAidService, 
                                    ArchivalEntityService archivalEntityService,
                                    ArchDescService archDescService,
                                    InstitutionsService institutionsService) {
        this.apuSourceService = apuSourceService;
        this.daoImportService = daoImportService;
        this.fundService = fundService;
        this.findingAidService = findingAidService;
        this.archivalEntityService = archivalEntityService;
        this.archDescService = archDescService;
        this.institutionsService = institutionsService;
        log.info("UI controller started");
    }

    // get: /apus
    @Override
    public ResponseEntity<ApuSourceResponse> getApus(@Valid String datadir, 
    		                                         @Valid String uuid,
                                                     @Min(0) @Valid Integer page,
                                                     @Min(1) @Max(1000) @Valid Integer pageSize) {
        return ResponseEntity.ok(apuSourceService.getApuSources(datadir, uuid, page, pageSize));
    }

    // delete: /apus/{apuId}
	@Override
	public ResponseEntity<Void> deleteApu(Long apuId) {
		apuSourceService.delete(apuId.intValue());
		return ResponseEntity.ok().build();
	}

	// patch: /apus/{apuId}/reimport
    @Override
    public ResponseEntity<ApuSourceDef> reimportApu(Long apuId) {
        return ResponseEntity.ok(apuSourceService.reimportApuSource(apuId.intValue()));
    }

    // get: /archives
    @Override
    public ResponseEntity<String> getArchives(@Valid String name) {
        // TODO Auto-generated method stub
        return null;
    }

    // get: /daos
    @Override
    public ResponseEntity<DaosResponse> getDaos(@Valid String uuid, 
    		                                    @Min(0) @Valid Integer page,
                                                @Min(1) @Max(1000) @Valid Integer pageSize) {
        return ResponseEntity.ok(daoImportService.getDaos(uuid, page, pageSize));
    }

    // post: /daos/reimport
    @Override
    public ResponseEntity<Void> reimportDaos(Resource body) {
        try (var is = body.getInputStream()) {
            IOUtils.readLines(is, StandardCharsets.UTF_8).stream().map(s -> s.trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResponseEntity.ok().build();
    }

    // patch: /daos/{daoId}/reimport
    @Override
    public ResponseEntity<DaoDef> reimportDao(Long daoId) {        
        return ResponseEntity.ok(daoImportService.reimportDao(daoId.intValue()));
    }

    // get: /entities
    @Override
    public ResponseEntity<EntitiesResponse> getEntities(@Valid String code, 
    		                                            @Valid String uuid,
                                                        @Min(0) @Valid Integer page,
                                                        @Min(1) @Max(1000) @Valid Integer pageSize) {        
        return ResponseEntity.ok(archivalEntityService.getEntities(code, uuid, page, pageSize));
    }

    // get: /findingaids
    @Override
    public ResponseEntity<FindingAidsResponse> getFindingAids(@Valid String code, 
    		                                                  @Valid String uuid,
                                                              @Min(0) @Valid Integer page,
                                                              @Min(1) @Max(1000) @Valid Integer pageSize) {
        return ResponseEntity.ok(findingAidService.getFindingAids(code, uuid, page, pageSize));
    }

    // get: /funds
    @Override
    public ResponseEntity<FundsResponse> getFunds(@Valid String code, 
    		                                      @Valid String uuid, 
    		                                      @Min(0) @Valid Integer page,
                                                  @Min(1) @Max(1000) @Valid Integer pageSize) {
        return ResponseEntity.ok(fundService.getFunds(code, uuid, page, pageSize));
    }

	@Override
	public ResponseEntity<Void> reimportAllApusOfType(@NotNull @Valid ReimportType apuType) {
		apuSourceService.reimportAll(apuType);
		return ResponseEntity.ok().build();
	}

	@Override
	public ResponseEntity<ArchDescResponse> getArchDescs(@Valid String query, @Valid String uuid,
			@Min(0) @Valid Integer page, @Min(1) @Max(1000) @Valid Integer pageSize) {		
		return ResponseEntity.ok(archDescService.getArchDesc(query, page, pageSize));
	}

	@Override
	public ResponseEntity<InstitutionResponse> getInstitutions(@Valid String code, @Min(0) @Valid Integer page,
			@Min(1) @Max(1000) @Valid Integer pageSize) {
		// TODO Auto-generated method stub
		return ResponseEntity.ok(institutionsService.getInstitutions(code, page, pageSize));
	}	

	
}
