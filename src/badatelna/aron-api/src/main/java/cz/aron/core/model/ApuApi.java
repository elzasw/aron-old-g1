package cz.aron.core.model;

import cz.inqool.eas.common.domain.DomainApi;
import cz.inqool.eas.common.domain.index.dto.Result;
import cz.inqool.eas.common.domain.index.dto.params.Params;
import cz.inqool.eas.common.exception.InvalidArgument;
import cz.inqool.eas.common.exception.MissingObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

import static cz.inqool.eas.common.utils.AssertionUtils.coalesce;
import static cz.inqool.eas.common.utils.AssertionUtils.lte;
import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@RestController
@RequestMapping("/apu")
@Slf4j
public class ApuApi extends DomainApi<
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuService> {

    @Inject private ApuTreeViewStore apuTreeViewStore;
    @Inject private ApuEntityViewStore apuEntityViewStore;
    @Inject private ApuEntitySimpleStore apuEntitySimpleStore;    
    @Inject private ApuRepository apuRepository;
    @Inject private ApuStore apuStore;
    @Inject private ObjectMapper objectMapper;
    @Value("${files.treeCache:}")
    private String treeCache;

    @GetMapping("/{id}/tree")
    public ResponseEntity<?> getSimpleTree(@PathVariable("id") String apuId) {                        
        if (treeCache!=null) {
            var published = apuStore.findApuSourcePublished(apuId);
            String timestamp = "";
            if (published!=null) {
                timestamp = "" + published.toEpochSecond(ZoneOffset.UTC);
            }
            var directory = Paths.get(treeCache, apuId.substring(0,2), apuId.substring(2,4));
            var path = directory.resolve(apuId + "_" + timestamp  + ".gz");
            if (Files.isRegularFile(path)) {
                return file(path);
            } else {
                var tmpPath = directory.resolve(apuId+".tmp");
                ApuEntityTreeView apuEntityTreeView = apuTreeViewStore.find(apuId);
                try {
                    Files.createDirectories(directory);
                    try (var fos = new FileOutputStream(tmpPath.toFile()); var gzos = new GZIPOutputStream(fos)) {
                        objectMapper.writeValue(gzos, apuEntityTreeView);
                        gzos.flush();
                    }
                    Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Tree cached {}", apuId);                    
                } catch (IOException ioEx) {
                    log.error("Fail to cache tree {}", apuId, ioEx);
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        log.warn("Fail to delete {}", path, e);
                    }
                    try {
                        Files.deleteIfExists(tmpPath);
                    } catch (IOException e) {
                        log.warn("Fail to delete {}", path, e);
                    }
                    return ResponseEntity.ok(apuEntityTreeView);
                }
                return file(path);             
            }
        } else {
            ApuEntityTreeView apuEntityTreeView = apuTreeViewStore.find(apuId);
            return ResponseEntity.ok(apuEntityTreeView);    
        }
    }

    private ResponseEntity<FileSystemResource> file(Path path) {        
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_ENCODING, 
          "gzip");        
        return ResponseEntity.ok()
                .headers(responseHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new FileSystemResource(path));          
    }
    

    @GetMapping("/labels")
    public List<IdLabelDto> mapNames(@RequestBody List<String> ids) {
        return service.mapNames(ids);
    }
    
    @PostMapping("/listview")
    public Result<ApuEntityView> listView(@Valid @RequestBody(required = false) Params params) {
        
        params = coalesce(params, Params::new);
        
        Result<String> idsResult = apuRepository.getIndex().listIdsByParams(params);
        List<ApuEntityView> items = apuEntityViewStore.listByIds(idsResult.getItems());
        return new Result<>(items, idsResult.getCount(), idsResult.getSearchAfter(), idsResult.getAggregations());
    }
    
    @PostMapping("/listsimple")
    public Result<ApuEntitySimple> listSimple(@Valid @RequestBody(required = false) Params params) {
        
        params = coalesce(params, Params::new);
        
        Result<String> idsResult = apuRepository.getIndex().listIdsByParams(params);
        List<ApuEntitySimple> items = apuEntitySimpleStore.listByIds(idsResult.getItems());
        return new Result<>(items, idsResult.getCount(), idsResult.getSearchAfter(), idsResult.getAggregations());
    }
    
    @GetMapping(value = "/{id}/view")
    public ApuEntityView getView(@PathVariable("id") String id) {                        
        ApuEntityView view = apuEntityViewStore.find(id);        
        notNull(view, () -> new MissingObject(ApuEntityView.class, id));
        return view;
    }
    
    @GetMapping(value = "/views")
    public ResponseEntity<List<ApuEntityView>> getViews(@RequestParam List<String> ids) {
        lte(ids.size(),100, ()-> new InvalidArgument("ids", InvalidArgument.ErrorCode.SIZE_TOO_BIG));        
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=1800")
                .contentType(MediaType.APPLICATION_JSON)
		.body(apuEntityViewStore.listByIds(ids));
    }

    @GetMapping(value = "/{id}/related/{direction}")
    public ResponseEntity<List<ApuEntityTreeViewDto>> getRelatedNodes(@PathVariable("id") String apuId, @PathVariable("direction") String direction) {
	List<ApuEntityTreeViewDto> body = null;
    	switch(direction) {
    	case "before":
    		body = apuStore.getEntitiesBefore(apuId);
		break;
    	case "after":
    		body = apuStore.getEntitiesAfter(apuId);
		break;
    	case "under":
    		body = apuStore.getEntitiesUnder(apuId);
		break;
    	default:
    		throw new RuntimeException();
    	}
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=1800")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping(value = "/{id}/entity")
    public ResponseEntity<ApuEntity> getEntity(@PathVariable("id") String id) {
        var body = service.get(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=1800")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
