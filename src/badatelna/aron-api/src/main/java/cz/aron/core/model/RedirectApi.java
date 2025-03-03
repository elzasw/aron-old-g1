package cz.aron.core.model;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

@RestController
@Slf4j
public class RedirectApi {
	
	@Inject private ApuStore apuStore;
	@Inject private ApuAttachmentStore apuAttachmentStore;
	@Inject private DigitalObjectFileStore digitalObjectFileStore;

	@GetMapping("/redirect/**")
    public RedirectView redirect(HttpServletRequest request) {		
		String wildcardPath = request.getRequestURI().replace("/api/aron/redirect", "");
	    log.info("Redirecting {}", wildcardPath);
        var id = apuStore.findIdByPermalink(wildcardPath);
        if (id == null) {
            log.warn("Entity not found for permalink: {}", wildcardPath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
        }
        String redirectUrl = "/apu/" + id;
        log.info("Redirecting from /redirect{} to {}", wildcardPath, redirectUrl);

        return new RedirectView(redirectUrl);
    }
	
	@GetMapping("/attachment/**")
	public RedirectView redirectAttachment(HttpServletRequest request) {
		String wildcardPath = request.getRequestURI().replace("/api/aron/attachment/", "");
		log.info("Get attachment {}", wildcardPath);
		var ids = apuAttachmentStore.findFileIdsByName(wildcardPath);
		if (ids.isEmpty()) {
			log.warn("Attachment not found for permalink: {}", wildcardPath);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
		} else if (ids.size() > 1) {
			log.warn("Multiple attachment not found for permalink: {}", wildcardPath);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Multiple attachment not found");
		}				
		String redirectUrl = "/api/aron/file/" + ids.get(0);
		log.info("Redirecting from /attachment/{} to {}", wildcardPath, redirectUrl);		
		return new RedirectView(redirectUrl);		
	}

	@GetMapping("/redirectimage/**")
    public RedirectView redirectImage(HttpServletRequest request) {		
		String wildcardPath = request.getRequestURI().replace("/api/aron/redirectimage", "");
	    log.info("Redirecting {}", wildcardPath);

        var id = digitalObjectFileStore.findIdByPermalink(wildcardPath);
        if (id == null) {
            log.warn("Entity not found for permalink: {}", wildcardPath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
        }
        String redirectUrl = "/apu/" + id.getApuId() + "/dao/" + id.getDaoId() + "/file/" + id.getFileId();
        log.info("Redirecting from /redirectimage{} to {}", wildcardPath, redirectUrl);
        return new RedirectView(redirectUrl);
    }	

}