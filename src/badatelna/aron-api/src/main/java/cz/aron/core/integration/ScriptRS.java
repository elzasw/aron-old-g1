package cz.aron.core.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.model.ApuRepository;
import cz.inqool.eas.common.exception.dto.RestException;
import cz.inqool.eas.common.script.ScriptExecutor;
import cz.inqool.eas.common.script.ScriptType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/script")
public class ScriptRS {
	
	private final ScriptExecutor scriptExecutor;
	
	private final ObjectMapper objectMapper;
	
	private final ApuRepository apuRepository;
	
	public ScriptRS(ScriptExecutor scriptExecutor, ObjectMapper objectMapper, ApuRepository apuRepository) {
		this.scriptExecutor = scriptExecutor;
		this.objectMapper = objectMapper;
		this.apuRepository = apuRepository;
	}
	
	@Operation(summary = "Get the content of a upload with given ID.", description = "Returns content of an upload in input stream.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ResponseEntity.class)))
    @ApiResponse(responseCode = "404", description = "The file was not found.", content = @Content(schema = @Schema(implementation = RestException.class)))
    @GetMapping(value = "/{scriptName}/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object apuScript(@Parameter(description = "ID of APU", required = true) @PathVariable("scriptName") String scriptName,
			@Parameter(description = "ID of APU", required = true) @PathVariable("id") String id) {

		String code = null;
		try {
			code = new String(Files.readAllBytes(Path.of(scriptName+".groovy")),"utf-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		Map<String,Object> configuration = new HashMap<>();
		configuration.put("id", id);
		configuration.put("apuRepository", apuRepository);
		configuration.put("objectMapper", objectMapper);
		return scriptExecutor.executeScript(ScriptType.GROOVY, code, false, configuration, null);		
	}

}
