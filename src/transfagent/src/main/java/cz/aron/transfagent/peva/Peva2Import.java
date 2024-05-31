package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigPeva2InstitutionCredentials;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.ThreadStatus;
import cz.aron.transfagent.service.importfromdir.ImportContext;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2Import implements  SmartLifecycle {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2Import.class);

	private final ConfigPeva2 config;

	private final List<Peva2Downloader> downloaders = new ArrayList<>();
	
	private final ApplicationContext applicationContext;
	
	private final Peva2CodeListProvider codeListProvider;
	
	private final StorageService storageService;

	private OffsetDateTime nextRun = null;
	
	private ThreadStatus status;
	
	private long importInterval = 5000;

    public Peva2Import(ConfigPeva2 config,
                       Peva2CodeListProvider codeListProvider, ApplicationContext applicationContext,
                       StorageService storageService) {
        this.config = config;
        this.codeListProvider = codeListProvider;
        this.applicationContext = applicationContext;
        this.storageService = storageService;
    }

	@PostConstruct
	void init() {
		downloaders.add(applicationContext.getBean(Peva2ImportOriginators.class));
		downloaders.add(applicationContext.getBean(Peva2ImportGeo.class));
		downloaders.add(applicationContext.getBean(Peva2ImportFindingAidAuthor.class));
		createInstitutionDownloaders(Peva2ImportFindingAidAuthor.class);
		downloaders.add(applicationContext.getBean(Peva2ImportFindingAidCopy.class));
						
		downloaders.add(applicationContext.getBean(Peva2ImportFunds.class));
		createInstitutionDownloaders(Peva2ImportFunds.class);		
		downloaders.add(applicationContext.getBean(Peva2ImportFindingAids.class));
		createInstitutionDownloaders(Peva2ImportFindingAids.class);
		
		/*
		downloaders.add(applicationContext.getBean(Peva2ImportFundIds.class));
		createInstitutionDownloaders(Peva2ImportFundIds.class);
		*/
				
		for (Peva2Downloader downloader : downloaders) {
			log.info("Downloader {}, status={}", downloader.getName(), downloader.isActive());
		}
		pevas.clear();
	}
	
	private void createInstitutionDownloaders(Class<? extends Peva2Downloader> cls) {
	    if (config.getInstitutions()!=null) {
            for(ConfigPeva2InstitutionCredentials credentials: config.getInstitutions()) {
                PEvA2Connection peva2 = getForInstitution(credentials);
                downloaders.add(applicationContext.getBean(cls,peva2));                
            }
        }
	}

	private Map<String,PEvA2Connection> pevas = new HashMap<>();
	
    private PEvA2Connection getForInstitution(ConfigPeva2InstitutionCredentials credentials) {
        PEvA2Connection peva2 = pevas.get(credentials.getInstitutionId());
        if (peva2 != null) {
            return peva2;
        }
        peva2 = (PEvA2Connection) applicationContext.getBean(PEvA2Client.PEVA2_INST_BEAN_NAME, config, credentials);
        pevas.put(credentials.getInstitutionId(), peva2);
        return peva2;
    }

	public void importData(ImportContext ic) {

		final OffsetDateTime nowTime = OffsetDateTime.now();

		processCommands();
		
		if (nextRun != null && nextRun.isAfter(OffsetDateTime.now())) {
			// TODO nejake lepsi planovani
			return;
		}
		
		codeListProvider.reset();
		for (var downloader : downloaders) {
			if (downloader.isActive()) {
				downloader.importDataInternal(ic, codeListProvider);
			}
		}

		// po uspesne synchronizaci nastavim dalsi cas synchronizace
		nextRun = nowTime.plusSeconds(config.getInterval());
	}
	
	private void processCommands() {
		Path commandsPath = storageService.getInputPath().resolve("commands");
		if (Files.isDirectory(commandsPath)) {
			try (Stream<Path> stream = Files.list(commandsPath);) {
				stream.forEach(f -> {
					if (Files.isRegularFile(f)) {
						for (var downloader : downloaders) {
							try {
								if (downloader.processCommand(f, codeListProvider)) {
									try {
										Files.deleteIfExists(f);
									} catch (IOException e) {
										log.error("Fail to delete command file {}", f);
										throw new UncheckedIOException(e);
									}
									break;
								}
							} catch (Exception e) {
								log.error("Fail to process command file {}", f, e);
								throw new IllegalStateException(e);
							}
						}
					}
				});
			} catch (IOException ioEx) {
				log.error("Fail to read commands directory {}", commandsPath, ioEx);
				throw new UncheckedIOException(ioEx);
			}
		}
	}

	public Peva2CodeLists getCodeLists() {
		return codeListProvider.getCodeLists();
	}

    public void run() {
        log.info("Import peva2 service is running.");
        
        while (status == ThreadStatus.RUNNING) {
            try {
            	ImportContext ic = new ImportContext();
                importData(ic);
                if(ic.isFailed()||ic.getNumProcessed()==0) {
                	Thread.sleep(importInterval);
                }
            } catch (Exception e) {
                log.error("Error in import file. ", e);
                try {
					Thread.sleep(importInterval);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					return;
				}
            }
        }
        log.info("Import peva2 service stopped.");
        status = ThreadStatus.STOPPED;
    }
	
	@Override
	public void start() {
		 status = ThreadStatus.RUNNING;
	        new Thread(() -> {
	            run();
	        }).start();
	}

	@Override
	public void stop() {
		status = ThreadStatus.STOP_REQUEST;		
	}

	@Override
	public boolean isRunning() {
		return status == ThreadStatus.RUNNING;
	}
	
}
