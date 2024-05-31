package cz.aron.transfagent.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.common.BulkOperation;
import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.Property;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.tacr.elza.ws.types.v1.EntityUpdates;
import cz.tacr.elza.ws.types.v1.SearchEntityUpdates;

@Service
@ConditionalOnProperty(value = "elza.update", havingValue="1", matchIfMissing=true)
public class ElzaUpdateEntityProcessor implements ImportProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(ElzaUpdateEntityProcessor.class);

    private final ElzaExportService elzaExportService;

    private final FileImportService importService;

    private final ConfigElza configElza;

    private final ArchivalEntityRepository archivalEntityRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final PropertyRepository propertyRepository;
    
    private final TransactionTemplate transactionTemplate;
    
    private long lastUpdate = 0;
    
    public static final String ELZA_LAST_TRANSACTION = "ELZA_LAST_TRANSACTION"; 

    public ElzaUpdateEntityProcessor(final ElzaExportService elzaExportService, 
                                     final FileImportService importService,
                                     final ConfigElza configElza, 
                                     final ArchivalEntityRepository archivalEntityRepository, 
                                     final ApuSourceRepository apuSourceRepository,
                                     final PropertyRepository propertyRepository,
                                     final TransactionTemplate transactionTemplate) {
        this.elzaExportService = elzaExportService;
        this.importService = importService;
        this.configElza = configElza;
        this.archivalEntityRepository = archivalEntityRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.propertyRepository = propertyRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    void init() {
        importService.registerImportProcessor(this);
    }
    
    @Override
    public int getPriority() { 
        return -50; 
    }

    @Override
    public void importData(ImportContext ic) {        
        if (configElza.isDisabled()) {
            log.debug("Elza is disabled");
            return;
        }
        if (System.currentTimeMillis()-lastUpdate<60*60*1000l) {
            // update jednou za hodinu
            return;
        }
        log.info("Checking Elza updates");
        
        this.transactionTemplate.executeWithoutResult(ts -> importDataTrans(ts, ic));
        
        lastUpdate = System.currentTimeMillis();
        
        log.info("Elza updates check finished");
    }

    private void importDataTrans(TransactionStatus ts, ImportContext ic) {

        var property = propertyRepository.findByName(ELZA_LAST_TRANSACTION);
        var fromTrans = property == null? "0" : property.getValue();

        var exportService = elzaExportService.get();
        var request = new SearchEntityUpdates();
        request.setFromTrans(fromTrans);
        EntityUpdates entityUpdates;
        try {
            entityUpdates = exportService.searchEntityUpdates(request);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
        
        log.info("Received updates, transaction from: {} to: {}", entityUpdates.getFromTrans(), entityUpdates.getToTrans());

        if(property==null) {
            property = new Property();
            property.setName(ELZA_LAST_TRANSACTION);
        }
        property.setValue(entityUpdates.getToTrans());
        propertyRepository.save(property);

        if(entityUpdates.getEntityIds()!=null) {
            // List entity UUIDS
            var identifiers = entityUpdates.getEntityIds().getIdentifier();
            
            log.info("Updates count: {}", identifiers.size());
            
            BulkOperation.run(identifiers, 1000, sids -> {
                List<UUID> uuids = sids.stream()
                        .map(p -> UUID.fromString(p))
                        .collect(Collectors.toList());                
                var ents = archivalEntityRepository.findByUuidIn(uuids);
                for(var ent: ents) {                    
                    ApuSource apusrc = ent.getApuSource();
                    if(apusrc==null) {
                    	if (ent.getStatus()==EntityStatus.NOT_AVAILABLE) {
                    		ent.setStatus(EntityStatus.ACCESSIBLE);
                    		 archivalEntityRepository.save(ent);
                    		log.info("Found modified entity, id={}, uuid={}, elzaId={}, mark as ACCESSIBLE", ent.getId(),
                                    ent.getUuid(), ent.getElzaId());
                    	} else {
                    		log.info("Found modified entity, id={}, uuid={}, elzaId={}, state={}, ignored", ent.getId(),
                                ent.getUuid(), ent.getElzaId(), ent.getStatus());
                    	}
                    } else {
                    	// mark for fresh download and reimport
                        ent.setDownload(true);
                        archivalEntityRepository.save(ent);
                        apusrc.setReimport(true);
                        apuSourceRepository.save(apusrc);	
                    }                    
                }
            });
        }
    }

}
