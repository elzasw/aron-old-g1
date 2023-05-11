package cz.aron.transfagent.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.api.rest.TransfagentApi;
import cz.aron.api.rest.model.ApuSourceDef;
import cz.aron.api.rest.model.ApuSourceResponse;
import cz.aron.api.rest.model.DaoDef;
import cz.aron.api.rest.model.DaosResponse;
import cz.aron.api.rest.model.EntitiesResponse;
import cz.aron.api.rest.model.FindingAidsResponse;
import cz.aron.api.rest.model.FundsResponse;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.DaoImportService;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.FundService;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class TransformAgentController implements TransfagentApi {
    
    private final ApuSourceService apuSourceService;
    
    private final DaoImportService daoImportService;
    
    private final FundService fundService;
    
    private final FindingAidService findingAidService;
    
    private final ArchivalEntityService archivalEntityService;
    
    public TransformAgentController(ApuSourceService apuSourceService, DaoImportService daoImportService,
                                    FundService fundService,
                                    FindingAidService findingAidService, ArchivalEntityService archivalEntityService) {
        this.apuSourceService = apuSourceService;
        this.daoImportService = daoImportService;
        this.fundService = fundService;
        this.findingAidService = findingAidService;
        this.archivalEntityService = archivalEntityService;
    }

    @Override
    public ResponseEntity<ApuSourceResponse> getApus(@Valid String datadir, @Valid String uuid,
                                                     @Min(0) @Valid Integer page,
                                                     @Min(1) @Max(1000) @Valid Integer pageSize) {
        return ResponseEntity.ok(apuSourceService.getApuSources(datadir, uuid, page, pageSize));
    }

    @Override
    public ResponseEntity<String> getArchives(@Valid String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<ApuSourceDef> reimportApu(Long apuId) {
        return ResponseEntity.ok(apuSourceService.reimportApuSource(apuId.intValue()));
    }

    @Override
    public ResponseEntity<DaosResponse> getDaos(@Valid String uuid, @Min(0) @Valid Integer page,
                                                @Min(1) @Max(1000) @Valid Integer pageSize) {
        
        return ResponseEntity.ok(daoImportService.getDaos(uuid, page, pageSize));
    }

    @Override
    public ResponseEntity<DaoDef> reimportDao(Long daoId) {        
        return ResponseEntity.ok(daoImportService.reimportDao(daoId.intValue()));
    }

    @Override
    public ResponseEntity<FundsResponse> getFunds(@Valid String code, @Valid String uuid, @Min(0) @Valid Integer page,
                                                  @Min(1) @Max(1000) @Valid Integer pageSize) {
        return ResponseEntity.ok(fundService.getFunds(code, uuid, page, pageSize));
    }

    @Override
    public ResponseEntity<Void> reimportDaos(Resource body) {
        try (var is = body.getInputStream()) {
            IOUtils.readLines(is, StandardCharsets.UTF_8).stream().map(s->s.trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<EntitiesResponse> getEntities(@Valid String code, @Valid String uuid,
                                                        @Min(0) @Valid Integer page,
                                                        @Min(1) @Max(1000) @Valid Integer pageSize) {        
        return ResponseEntity.ok(archivalEntityService.getEntities(code, uuid, page, pageSize));
    }

    @Override
    public ResponseEntity<FindingAidsResponse> getFindingAids(@Valid String code, @Valid String uuid,
                                                              @Min(0) @Valid Integer page,
                                                              @Min(1) @Max(1000) @Valid Integer pageSize) {
        return ResponseEntity.ok(findingAidService.getFindingAids(code, uuid, page, pageSize));
    }

}
