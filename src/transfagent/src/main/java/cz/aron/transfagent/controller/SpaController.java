package cz.aron.transfagent.controller;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.helpers.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    private static final Logger logger = LoggerFactory.getLogger(SpaController.class);

    @GetMapping("/")
    public void index(HttpServletRequest request, HttpServletResponse response) throws IOException {    	
        returnIndexHtml(response);
        logger.debug("Single page application rendered for '/'");
    }

    @GetMapping("/sources/**")
    public void sources(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	returnIndexHtml(response);
    }
    
    @GetMapping("/daos/**")
    public void daos(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	returnIndexHtml(response);
    }
    
    @GetMapping("/funds/**")
    public void funds(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	returnIndexHtml(response);
    }
    
    @GetMapping("/aids/**")
    public void aids(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	returnIndexHtml(response);
    }
    
    @GetMapping("/entities/**")
    public void entities(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	returnIndexHtml(response);
    }
    
    @GetMapping("/archdescs/**")
    public void archdescs(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	returnIndexHtml(response);
    }
    
    @GetMapping("/institutions/**")
    public void institutions(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	returnIndexHtml(response);
    }
        
    private void returnIndexHtml(HttpServletResponse response) throws IOException {
    	response.setContentType(MediaType.TEXT_HTML_VALUE);
        try (OutputStream os = response.getOutputStream()) {        	
        	IOUtils.copy(SpaController.class.getResourceAsStream("/static/index.html"), os);        	
        }
    }
	
}
