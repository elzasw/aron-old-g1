package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.GetNadSheetRequest;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.InstitutionReference;
import cz.aron.peva2.wsdl.ListNadSheetRequest;
import cz.aron.peva2.wsdl.ListNadSheetResponse;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.NadSubsheet;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.AttachmentSource;
import cz.aron.transfagent.service.StorageService;

public class Peva2ImportFunds extends Peva2Downloader {
    
    private static final Logger log = LoggerFactory.getLogger(Peva2ImportFunds.class);
    
    private static final String PREFIX_DASH = "pevafund-";
    
    private final String institutionPrefix;
    
    private final AttachmentSource attachmentSource;
	
	private LRUMap<String,String> fundUUIDToJaFa = null;

	public Peva2ImportFunds(PEvA2Connection peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService, AttachmentSource attachmentSource, boolean active) {
		super("FUND", peva2, propertyRepository, config, tt, storageService, active);
		this.attachmentSource = attachmentSource;
		institutionPrefix = PREFIX_DASH + peva2.getInstitutionId() + "-";
	}
   
	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial,
			Peva2CodeListProvider codeListProvider) {
		String searchAfter = searchAfterInitial;
		int numUpdated = 0;
		while (true) {
			ListNadSheetRequest lnsr = new ListNadSheetRequest();
			lnsr.setSize(config.getBatchSize());
			lnsr.setUpdatedAfter(updateAfter);
			lnsr.setSearchAfter(searchAfter);
			ListNadSheetResponse lnsResp = peva2.getPeva().listNadSheet(lnsr);
			log.info("Downloaded {} funds to update after {}",
					lnsResp.getItems().getNadPrimarySheetOrNadSubsheet().size(), updateAfter);
			searchAfter = lnsResp.getSearchAfter();
			long count = lnsResp.getItems().getNadPrimarySheetOrNadSubsheet().size();
			if (count == 0) {
				break;
			}

			fundUUIDToJaFa = new LRUMap<String, String>(10000);
			int numUpdatedFromBatch = 0;
			try {
				patchFundBatch(lnsResp.getItems().getNadPrimarySheetOrNadSubsheet(), codeListProvider.getCodeLists());
				numUpdatedFromBatch += lnsResp.getItems().getNadPrimarySheetOrNadSubsheet().size();
			} catch (Exception e) {
				log.error("Fail to update fund batch", e);
				// zkusim to po jednom
				for (NadSheet nadSheet : lnsResp.getItems().getNadPrimarySheetOrNadSubsheet()) {
					try {
						patchFundBatch(Collections.singletonList(nadSheet), codeListProvider.getCodeLists());
						numUpdatedFromBatch++;
					} catch (Exception e1) {
						log.error("Fail to update fund {}", nadSheet.getId(), e1);
					}
				}
			}

			storeSearchAfter(searchAfter);
			numUpdated += numUpdatedFromBatch;
			log.info("Updated {}/{} funds", numUpdated, lnsResp.getCount());
		}
		log.info("Funds synchronized");
		fundUUIDToJaFa = null;
		return numUpdated;
	}
    
    private void patchFundBatch(List<NadSheet> nadSheets, Peva2CodeLists codeLists) {    	
    	Path fundsInputDir = storageService.getInputPath().resolve("fund");
		for (NadSheet nadSheetId : nadSheets) {			
			var gnsReqMain = new GetNadSheetRequest();
			gnsReqMain.setId(nadSheetId.getId());
			var gnsRespMain = peva2.getPeva().getNadSheet(gnsReqMain);
			if (gnsRespMain.getNadPrimarySheet()!=null) {
				NadPrimarySheet nps = gnsRespMain.getNadPrimarySheet();
				InstitutionReference ir = nps.getInstitution();
				String fundId = ArchiveFundId.createJaFaId(ir.getExternalId(),nps.getEvidenceNumber(),null);				
				Path fundDir = fundsInputDir.resolve(PREFIX_DASH+fundId);			
				try {
					Files.createDirectories(fundDir);
					copyAttachments(ir.getExternalId(),nps.getEvidenceNumber(),null,fundDir);
					Path name = fundDir.resolve(PREFIX_DASH+fundId+".xml");					
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadPrimarySheet(nps);					
					Peva2XmlReader.marshalGetNadSheetResponse(gnsr, name);
				} catch (Exception e) {
					log.error("Fail to store pevafund {} to input dir",gnsRespMain.getNadPrimarySheet().getId(),e);
					throw new IllegalStateException(e);
				}
				fundUUIDToJaFa.put(nps.getId(), nps.getEvidenceNumber());
			} else if (gnsRespMain.getNadSubsheet()!=null) {
				NadSubsheet nss = gnsRespMain.getNadSubsheet();
				var parentUUID = nss.getParent();				
				var evidenceNumber = fundUUIDToJaFa.get(parentUUID);
				if (evidenceNumber==null) {
					var gnsReqParent = new GetNadSheetRequest();
					gnsReqParent.setId(parentUUID);
					var gnsRespParent = peva2.getPeva().getNadSheet(gnsReqParent);
					evidenceNumber = gnsRespParent.getNadPrimarySheet().getEvidenceNumber();
					fundUUIDToJaFa.put(gnsRespParent.getNadPrimarySheet().getId(), evidenceNumber);
				}												
				InstitutionReference ir = nss.getInstitution();
				var fundId = ArchiveFundId.createJaFaId(ir.getExternalId(),evidenceNumber,""+nss.getNumber());				
				Path fundDir = fundsInputDir.resolve(PREFIX_DASH+fundId);			
				try {
					Files.createDirectories(fundDir);
					copyAttachments(ir.getExternalId(),evidenceNumber,nss.getNumber(),fundDir);
					Path name = fundDir.resolve(PREFIX_DASH+fundId+".xml");					
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadSubsheet(nss);					
					Peva2XmlReader.marshalGetNadSheetResponse(gnsr, name);
				} catch (Exception e) {
					log.error("Fail to store pevafund {} to input dir",gnsRespMain.getNadSubsheet().getId(),e);
					throw new IllegalStateException(e);
				}
			}
		}
    }
    
    private void copyAttachments(String institutionCode, String fundCode, Integer subFundCode, Path fundDir)
            throws IOException {
        if (attachmentSource == null) {
            return;
        }
        var attachments = attachmentSource.getFundAttachments(institutionCode, fundCode, subFundCode);
        for (var attachment : attachments) {
            Files.copy(attachment.getPath(), fundDir.resolve(attachment.getName()));
        }
    }

    @Override
    protected boolean processCommand(Path path, Peva2CodeListProvider codeListProvider) {
        var fileName = path.getFileName().toString();
        String id;
		if (fileName.startsWith(institutionPrefix)) {
			id = fileName.substring(institutionPrefix.length());
		} else if (peva2.isMainConnection() && fileName.startsWith(PREFIX_DASH)
				&& fileName.length() == (PREFIX_DASH.length() + 36)) {
			id = fileName.substring(PREFIX_DASH.length());
		} else {
			// not my command
			return false;
		}

        var gnsReq = new GetNadSheetRequest();
        gnsReq.setId(id);
        var gnsResp = peva2.getPeva().getNadSheet(gnsReq);
        NadSheet nadSheet = gnsResp.getNadPrimarySheet();
        if (nadSheet == null) {
            nadSheet = gnsResp.getNadSubsheet();
        }
        fundUUIDToJaFa = new LRUMap<String, String>(10000);
        try {
            patchFundBatch(Collections.singletonList(nadSheet), codeListProvider.getCodeLists());
        } finally {
            fundUUIDToJaFa = null;
        }
        return true;
    }

    @Override
    protected String getName() {
        return super.getName() + "-" + peva2.getInstitutionId();
    }
    
    protected static final String createCommand(String institutionId, String fundId) {
        return PREFIX_DASH + institutionId + "-" + fundId;
    }
    
    @Configuration
    @ConditionalOnProperty(value = "peva2.url")
    public static class Peva2ImportFundsConfig {
        
        private final PropertyRepository propertyRepository;
        
        private final ConfigPeva2 config;
        
        private final TransactionTemplate tt;
        
        private final StorageService storageService;
        
        private final AttachmentSource attachmentSource;
        
        private final boolean active;
        
        public Peva2ImportFundsConfig(PropertyRepository propertyRepository, ConfigPeva2 config,
                                      TransactionTemplate tt, StorageService storageService,
                                      @Nullable AttachmentSource attachmentSource,
                                      @Value("${peva2.importFund:false}") boolean active) {
            this.propertyRepository = propertyRepository;
            this.config = config;
            this.tt = tt;
            this.storageService = storageService;
            this.attachmentSource = attachmentSource;
            this.active = active;
        }

        @Bean
        @Scope("prototype")
        public Peva2ImportFunds importFunds(PEvA2Connection peva2) {
            return new Peva2ImportFunds(peva2, propertyRepository, config, tt, storageService, attachmentSource, active); 
        }

    }
        
    
}
