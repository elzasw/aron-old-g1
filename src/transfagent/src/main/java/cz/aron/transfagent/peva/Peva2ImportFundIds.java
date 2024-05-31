package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
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

public class Peva2ImportFundIds  extends Peva2Downloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2ImportFundIds.class);
	
	private LRUMap<String,String> fundUUIDToJaFa = null;
	
	public Peva2ImportFundIds(PEvA2Connection peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService, AttachmentSource attachmentSource, boolean active) {
		super("FUNDIDS", peva2, propertyRepository, config, tt, storageService, active);
		this.storeState = false;
		
		if (active) {
			Path fundIdsInputDir = storageService.getInputPath().resolve("fundids");
			try {
				Files.createDirectories(fundIdsInputDir);
			} catch (IOException e) {
				log.error("Fail to create directory {}",fundIdsInputDir );
				throw new UncheckedIOException(e);
			}
		}
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
			log.info("Updated {}/{} fund ids", numUpdated, lnsResp.getCount());
		}
		log.info("Fund ids synchronized");
		fundUUIDToJaFa = null;
		return numUpdated;
	}
	
	private void patchFundBatch(List<NadSheet> nadSheets, Peva2CodeLists codeLists) { 
		Path fundIdsInputDir = storageService.getInputPath().resolve("fundids");
		for (NadSheet nadSheetId : nadSheets) {			
			var gnsReqMain = new GetNadSheetRequest();
			gnsReqMain.setId(nadSheetId.getId());
			var gnsRespMain = peva2.getPeva().getNadSheet(gnsReqMain);
			if (gnsRespMain.getNadPrimarySheet()!=null) {
				NadPrimarySheet nps = gnsRespMain.getNadPrimarySheet();
				InstitutionReference ir = nps.getInstitution();
				String fundId = ArchiveFundId.createJaFaId(ir.getExternalId(),nps.getEvidenceNumber(),null);							
				try {
					Path name = fundIdsInputDir.resolve(fundId+"#"+ir.getId()+"#"+nps.getId()+".xml");					
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
				try {
					Path name = fundIdsInputDir.resolve(fundId+"#"+ir.getId()+"#"+nss.getId()+".xml");					
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
	
	@Configuration
    @ConditionalOnProperty(value = "peva2.url")
    public static class Peva2ImportFundIdsConfig {
        
        private final PropertyRepository propertyRepository;
        
        private final ConfigPeva2 config;
        
        private final TransactionTemplate tt;
        
        private final StorageService storageService;
        
        private final AttachmentSource attachmentSource;
        
        private final boolean active;
        
        public Peva2ImportFundIdsConfig(PropertyRepository propertyRepository, ConfigPeva2 config,
                                      TransactionTemplate tt, StorageService storageService,
                                      @Nullable AttachmentSource attachmentSource,
                                      @Value("${peva2.importFundIds:false}") boolean active) {
            this.propertyRepository = propertyRepository;
            this.config = config;
            this.tt = tt;
            this.storageService = storageService;
            this.attachmentSource = attachmentSource;
            this.active = active;
        }

        @Bean
        @Scope("prototype")
        public Peva2ImportFundIds importFundIds(PEvA2Connection peva2) {
            return new Peva2ImportFundIds(peva2, propertyRepository, config, tt, storageService, attachmentSource, active); 
        }

    }
	
	
	
	public static void main(String[] args) {
						
		Set<String> existing = new HashSet<>();
		Map<String,Path> jafa = new HashMap<>();		
		try {
			
			Files.readAllLines(Paths.get("C:\\projects\\aron\\data_archivy\\mza\\funds.csv")).forEach(l->{
				existing.add(l.split(",")[3]);				
			});;			
									
			Files.list(Paths.get("C:\\projects\\aron\\data\\transfagent\\input_folder\\input\\fundids")).forEach(p->{
				var fileName = p.getFileName().toString();				
				var tmp = fileName.split("#")[0];
				if (!existing.remove(tmp)) {
					jafa.put(tmp, p);
				}				
			});
			
			jafa.forEach((j,p)->{				
				try {
					GetNadSheetResponse gnsr = Peva2XmlReader.unmarshalGetNadSheetResponse(p);
					
					if (gnsr.getNadPrimarySheet()!=null) {
						var primary = gnsr.getNadPrimarySheet();
						System.out.println("pevafund-"+primary.getInstitution().getId()+"-"+primary.getId());
					} else {
						var sub = gnsr.getNadSubsheet();
						System.out.println("pevafund-"+sub.getInstitution().getId()+"-"+sub.getId());
					}
					
					
					
				} catch (IOException | JAXBException e) {
					throw new RuntimeException(e);
				}				
			});
			
			
			System.out.println("Nazdar");						
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		
		
	}

}
