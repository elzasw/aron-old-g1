package cz.aron.transfagent.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;

@Service
public class ReimportService implements ImportProcessor {
	
	private final static Logger log = LoggerFactory.getLogger(ReimportService.class);  
	
	@Autowired
	FileImportService importService;
	
	@Autowired
	ApuSourceRepository apuSourceRepository;
	
	@Autowired
	private TransactionTemplate transactionTemplate;
	
	@Autowired
	private CoreQueueRepository coreQueueRepository;
	
	List<ReimportProcessor> processors = new ArrayList<>();
	
	@PostConstruct
	void init() {
		importService.registerImportProcessor(this);
	}
	
	public void registerReimportProcessor(ReimportProcessor p) {
		processors.add(p);
	}

	@Override
	public void importData(final ImportContext ic) {
		
		// check from DB reimport request
		while(true) {
			List<ApuSource> reimportRequests = apuSourceRepository.findFirst1000ByReimport(true);
			if (reimportRequests.isEmpty()) {
				// nic k reimportu
				break;
			}
			apuSourceLabel: for (var apuSource : reimportRequests) {
				log.debug("Reimporting apuSource: {}", apuSource.getId());
				Result result = null;
				for (var proc : processors) {
					result = proc.reimport(apuSource);
					if (result == Result.REIMPORTED || result == Result.NOCHANGES || result == Result.REIMPORTED_NO_SEND) {
						log.info("Reimported apuSource: id={}, uuid={}, status={}", apuSource.getId(), apuSource.getUuid(), result);
						boolean send = result == Result.REIMPORTED;
						transactionTemplate.executeWithoutResult(t->{
							apuSource.setReimport(false);
							apuSource.setForce(false);
							apuSource.setDateImported(ZonedDateTime.now());
							apuSourceRepository.save(apuSource);
							if (send) {
								// send to Core
								CoreQueue cq = new CoreQueue();
								cq.setApuSource(apuSource);
								coreQueueRepository.save(cq);
							}							
						});
						continue apuSourceLabel;
					} else if (result == Result.FAILED) {
						log.error("Reimport failed, id={}, uuid={}",apuSource.getId(), apuSource.getUuid());
						break;
					} else if (result==Result.UNSUPPORTED) {
					    // ignore
					} else if (result==Result.DELETED) {
						transactionTemplate.executeWithoutResult(t->{							
							// pokud neni oznaceno deleted, poslu do aroncore
							boolean send = !apuSource.isDeleted();							
							apuSource.setReimport(false);
							apuSource.setForce(false);
							apuSource.setDeleted(true);
							apuSource.setDateImported(ZonedDateTime.now());
							apuSourceRepository.save(apuSource);							
							// send to Core
							if (send) {
								CoreQueue cq = new CoreQueue();
								cq.setApuSource(apuSource);
								coreQueueRepository.save(cq);
							}
						});							
					}
				}

				if (result==Result.UNSUPPORTED) {
				    // some processor exist and all processors return UNSUPPORTED
				    transactionTemplate.executeWithoutResult(t->{
				        apuSource.setReimport(false);
				        apuSource.setDateImported(ZonedDateTime.now());
				        apuSourceRepository.save(apuSource);                    
				    });
				}				
				log.error("Item cannot be reimported: id={}, uuid={}", apuSource.getId(), apuSource.getUuid());
				ic.setFailed(true);
				break;
			}	
		}				
	}
	
	@Override
	public int getPriority() { return -100; }

}
