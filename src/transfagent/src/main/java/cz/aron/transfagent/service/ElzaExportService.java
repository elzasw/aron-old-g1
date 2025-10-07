package cz.aron.transfagent.service;

import javax.annotation.PostConstruct;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.InsecureTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigElza;
import cz.tacr.elza.ws.core.v1.ExportService;

@Service
public class ElzaExportService {

    private static final Logger log = LoggerFactory.getLogger(ElzaExportService.class);

    private final ConfigElza config;

    private ExportService exportService;

    public ElzaExportService(ConfigElza config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setServiceClass(ExportService.class);
        factoryBean.setAddress(config.getUrl());
        factoryBean.setUsername(config.getUsername());
        factoryBean.setPassword(config.getPassword());
        if (config.isSoapLogging()) {
            LoggingFeature lf = new LoggingFeature();
            lf.setPrettyLogging(true);
            factoryBean.getFeatures().add(lf);
        }

        exportService = factoryBean.create(ExportService.class);

        // set timeout by default on 5 minutes
        Client client = ClientProxy.getClient(exportService);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.getClient().setConnectionTimeout(5 * 60000);
        http.getClient().setReceiveTimeout(5 * 60000);
        if (config.getMaxChildElements() > 0) {
            client.getEndpoint().put(StaxUtils.MAX_CHILD_ELEMENTS, config.getMaxChildElements());
            log.info("Elza maxChildElements set to {}", config.getMaxChildElements());
        }
        
        if (config.isTrustAll()) {
        	TLSClientParameters params = http.getTlsClientParameters();
    		if (params == null) {
    			params = new TLSClientParameters();
    			params.setDisableCNCheck(true);
    			http.setTlsClientParameters(params);
    		}
        	params.setTrustManagers(InsecureTrustManager.getNoOpX509TrustManagers());
        	log.info("Set to trust all server certificates when communicating with Elza !!!");
		}

    }

    public ExportService get() {
        return exportService;
    }

}
