package cz.aron.transfagent.service;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;
import com.lightcomp.ft.core.send.items.BaseDir;
import com.lightcomp.ft.core.send.items.ListReader;
import com.lightcomp.ft.core.send.items.SimpleFile;
import com.lightcomp.ft.core.send.items.SourceItem;

import cz.aron.apux._2020.UuidList;
import cz.aron.management.v1.ApuManagementPort;
import cz.aron.transfagent.config.ConfigAronCore;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.service.client.CoreAronClient;

@Service
public class CoreQueueService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CoreQueueService.class);

    private final CoreQueueRepository coreQueueRepository;

    private final AttachmentRepository attachmentRepository;
    
    private final ApuSourceRepository apuSourceRepository;

    private final ConfigAronCore configAronCore;

    private final CoreAronClient coreAronClient;

    private final StorageService storageService;
    
    private final TransactionTemplate tt;

    private ThreadStatus status;
    
    private Client client = null;

	public CoreQueueService(CoreQueueRepository coreQueueRepository, AttachmentRepository attachmentRepository,
			ApuSourceRepository apuSourceRepository, ConfigAronCore configAronCore, CoreAronClient coreAronClient,
			StorageService storageService, TransactionTemplate tt) {
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentRepository = attachmentRepository;
		this.apuSourceRepository = apuSourceRepository;
		this.configAronCore = configAronCore;
		this.coreAronClient = coreAronClient;
		this.storageService = storageService;
		this.tt = tt;
    }

    /**
     * Odeslání dat do jádra
     */
	private void sendData() {

		while (true) {
			var events = coreQueueRepository.findFirst1000ByOrderById();
			if (events.isEmpty()) {
				// nothing to send
				return;
			}

			try {
				createClient();
				for (var item : events) {
					uploadOrDeleteData(item);
					tt.executeWithoutResult(t->{
						coreQueueRepository.deleteById(item.getId());
						apuSourceRepository.setLastSent(item.getApuSource().getId(), item.getId());
					});					
					if (status != ThreadStatus.RUNNING) {
						return;
					}
				}
			} finally {				
				if (client!=null) {
					try {
						client.stop();
					} catch (Exception e) {
						log.error("Fail to stop filetransfer client",e);
					}
				}
				client = null;
			}
		}
	}
	
	/**
	 * Create filetransfer client
	 */
	private void createClient() {
        ClientConfig clientConfig = new ClientConfig(configAronCore.getFt().getUrl());
        clientConfig.setSoapLogging(configAronCore.getFt().getSoapLogging());
        clientConfig.setRecoveryDelay(1);
        client = FileTransfer.createClient(clientConfig);
	}

    /**
     * Přenos dat pomocí FileTransfer nebo mazání přes WSDL dotaz
     * 
     * @param item
     */
    private void uploadOrDeleteData(CoreQueue item) {
        if (item.getApuSource().isDeleted()) { // dotaz na vymazání záznamu
            deleteItemData(item);
        } else {
            uploadItemData(item);
        }
    }

    /**
     * Mazání dat přes WSDL dotaz
     * 
     * @param item
     */
    private void deleteItemData(CoreQueue item) {
        UuidList uuidList = new UuidList();
        uuidList.getUuid().add(item.getApuSource().getUuid().toString());
        ApuManagementPort apuManagementPort = coreAronClient.get();
        try {
            apuManagementPort.deleteApuSources(uuidList);
        } catch (Exception e) {
            item.setErrorMessage(e.getMessage());
            coreQueueRepository.save(item);
            log.error("Error deleting apusrc.", e);
            throw new IllegalStateException(e);
        }
        log.info("Deleted apusrc={}, type={}", item.getApuSource().getUuid(), item.getApuSource().getSourceType());
    }

    /**
     * Přenos dat přes FileTransfer
     * 
     * @param item
     */
    private void uploadItemData(CoreQueue item) {
        List<SourceItem> sourceItems = createSourceItems(item);
        UploadRequestImpl request = UploadRequestImpl.buildRequest(new ListReader(sourceItems),""+item.getId());
        try {
            client.uploadSync(request);
        } finally {
            client.stop();
        }

        String errorMsg = null;
        if (request.isCanceled()) {
            errorMsg = "Transfer to server canceled.";  
        }
        if (request.isFailed()) {
            errorMsg = "Transfer to server failed.";  
        }

        if (!StringUtils.isBlank(errorMsg)) {
            item.setErrorMessage(errorMsg);
            coreQueueRepository.save(item);
            log.error(errorMsg);
            // diky vyhozeni exception bude zastaven a zrusen client
            throw new IllegalStateException(errorMsg);
        }
        log.info("Uploaded apusrc={}, type={}", item.getApuSource().getUuid(), item.getApuSource().getSourceType());
    }

    private List<SourceItem> createSourceItems(CoreQueue item) {
    	switch (item.getApuSource().getSourceType()) {
    	case DIRECT:
    		return createSourceItemsDirect(item);
    	case INSTITUTION:
    		return createSourceItemsSimple(item);
    	case ARCH_ENTITY:
    		return createSourceItemsSimple(item);
    	case FUND:
    		return createSourceItemsSimple(item);
    	case ARCH_DESCS:
    		return createSourceItemsSimple(item);
    	case FINDING_AID:
    	    return createSourceItemsSimple(item);
    	default:
    		throw new IllegalStateException("Unsupported source type: "+item.getApuSource().getSourceType());
    	}
    }

    private List<SourceItem> createSourceItemsDirect(CoreQueue item) {
        Path dataDir = storageService.getApuDataDir(item.getApuSource().getDataDir());
        File [] files = dataDir.toFile().listFiles((f)->f.isFile());

        Path file;
        if (files!=null&&files.length==1) {
        	file = files[0].toPath();
        } else {
        	throw new IllegalStateException();
        }

        List<SourceItem> sourceItems = new ArrayList<>();
        SimpleFile simpleFile = new SimpleFile(file, "apusrc-"+item.getApuSource().getUuid()+".xml");
        sourceItems.add(simpleFile);
        return sourceItems;
    }

    private List<SourceItem> createSourceItemsSimple(CoreQueue item) {
    	Path dataDir = storageService.getApuDataDir(item.getApuSource().getDataDir());
    	Path file = dataDir.resolve("apusrc.xml");
        List<SourceItem> sourceItems = new ArrayList<>();
        SimpleFile simpleFile = new SimpleFile(file, "apusrc-"+item.getApuSource().getUuid()+".xml");
        sourceItems.add(simpleFile);

        // add attachments
        List<Attachment> attachments = attachmentRepository.findByApuSource(item.getApuSource());
        if(!attachments.isEmpty()) {
            var listReader = new ListReader();
            for(Attachment attachment : attachments) {
                simpleFile = new SimpleFile(dataDir.resolve(attachment.getFileName()), attachment.getUuid().toString());
                listReader.addItem(simpleFile);
            }
            var dir = new BaseDir("files", listReader );
            sourceItems.add(dir);
        }
        return sourceItems;
    }

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                sendData();
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("Error in sending data. ", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        status = ThreadStatus.STOPPED;
    }

    @Override
    public void start() {
        if (configAronCore.isDisabled()) {
            status = ThreadStatus.STOPPED;
            return;
        }
        status = ThreadStatus.RUNNING;
        new Thread(() -> {
            run();
        }).start();
    }

    @Override
    public void stop() {
    	if(status==ThreadStatus.RUNNING) {
    		status = ThreadStatus.STOP_REQUEST;
    	}
    }

    @Override
    public boolean isRunning() {
        return status == ThreadStatus.RUNNING;
    }
}
