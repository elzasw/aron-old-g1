package cz.aron.transfagent.controller;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.api.rest.TransfagentApi;
import cz.aron.api.rest.model.ApuSourceResponse;

@RestController
public class TransformAgentController implements TransfagentApi {

    @Override
    public ResponseEntity<ApuSourceResponse> getApus(@Valid String datadir, @Valid String uuid,
                                                     @Min(0) @Valid Integer page,
                                                     @Min(1) @Max(1000) @Valid Integer pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<String> getArchives(@Valid String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
