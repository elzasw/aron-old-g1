package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import cz.aron.peva2.wsdl.FieldSort;
import cz.aron.peva2.wsdl.GetNadSheetRequest;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.IdsFilter;
import cz.aron.peva2.wsdl.Include;
import cz.aron.peva2.wsdl.InstitutionReference;
import cz.aron.peva2.wsdl.ListExternalChangeRequest;
import cz.aron.peva2.wsdl.ListExternalChangeResponse;
import cz.aron.peva2.wsdl.ListInternalChangeRequest;
import cz.aron.peva2.wsdl.ListInternalChangeResponse;
import cz.aron.peva2.wsdl.ListNadSheetRequest;
import cz.aron.peva2.wsdl.ListNadSheetResponse;
import cz.aron.peva2.wsdl.ListRequest.Filters;
import cz.aron.peva2.wsdl.ListRequest.Includes;
import cz.aron.peva2.wsdl.ListRequest.Sorts;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.NadSubsheet;
import cz.aron.peva2.wsdl.SortOrder;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.AttachmentSource;
import cz.aron.transfagent.service.StorageService;

public class Peva2ImportFunds extends Peva2Downloader {
    
    private static final Logger log = LoggerFactory.getLogger(Peva2ImportFunds.class);
    
    private static final String PREFIX_DASH = "pevafund-";

    public static final String EXTERNAL_CHANGES_XML = "external_changes.xml";

    public static final String INTERNAL_CHANGES_XML = "internal_changes.xml";
    
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
			
			if (this.config.isNewStatechecking()) {
				var includes = new Includes();
				includes.getInclude().add(Include.DELETED);
				includes.getInclude().add(Include.INVALID);
				includes.getInclude().add(Include.DESTROYED);
				includes.getInclude().add(Include.MOVED);
				lnsr.setIncludes(includes);

				var sorts = new Sorts();
				var fieldSort = new FieldSort();
				fieldSort.setField("updated");
				fieldSort.setOrder(SortOrder.ASC);
				sorts.getSort().add(fieldSort);
				lnsr.setSorts(sorts);
			}
			
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
					// zmeny se ctou podle id uvedenych na listu NAD
					var externalChanges = downloadExternalChanges(
							nps.getExternalChanges() != null ? nps.getExternalChanges().getExternalChange() : null);
					var internalChanges = downloadInternalChanges(
							nps.getInternalChanges() != null ? nps.getInternalChanges().getInternalChange() : null);
					Files.createDirectories(fundDir);
					copyAttachments(ir.getExternalId(),nps.getEvidenceNumber(),null,fundDir);
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadPrimarySheet(nps);
					storeFundZip(fundDir, fundId, gnsr, externalChanges, internalChanges);
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
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadSubsheet(nss);
					// dilci list nema zmeny, do zipu se ulozi prazdne seznamy
					storeFundZip(fundDir, fundId, gnsr, downloadExternalChanges(null), downloadInternalChanges(null));
				} catch (Exception e) {
					log.error("Fail to store pevafund {} to input dir",gnsRespMain.getNadSubsheet().getId(),e);
					throw new IllegalStateException(e);
				}
			}
		}
    }

	private ListInternalChangeResponse downloadInternalChanges(List<String> ids) {
		return downloadInternalChanges(peva2, config, ids);
	}

	private ListExternalChangeResponse downloadExternalChanges(List<String> ids) {
		return downloadExternalChanges(peva2, config, ids);
	}

	/**
	 * Nacte vnitrni zmeny podle id.
	 *
	 * @param ids
	 *            id zmen, muze byt null nebo prazdny seznam
	 * @return odpoved obsahujici vsechny nalezene zmeny (vsechny stranky), pro
	 *         prazdny seznam id prazdna odpoved bez volani PEvA
	 */
	static ListInternalChangeResponse downloadInternalChanges(PEvA2Connection peva2, ConfigPeva2 config,
			List<String> ids) {
		var result = new ListInternalChangeResponse();
		var changes = new ListInternalChangeResponse.InternalChanges();
		result.setInternalChanges(changes);
		if (ids != null && !ids.isEmpty()) {
			String searchAfter = null;
			while (true) {
				var req = new ListInternalChangeRequest();
				req.setSize(config.getBatchSize());
				req.setSearchAfter(searchAfter);
				req.setFilters(createIdsFilter(ids));
				var resp = peva2.getPeva().listInternalChange(req);
				int pageSize = 0;
				if (resp.getInternalChanges() != null) {
					pageSize = resp.getInternalChanges().getInternalChange().size();
					changes.getInternalChange().addAll(resp.getInternalChanges().getInternalChange());
				}
				searchAfter = resp.getSearchAfter();
				if (isLastPage(config, pageSize, searchAfter, changes.getInternalChange().size(), ids.size())) {
					break;
				}
			}
			if (changes.getInternalChange().size() != ids.size()) {
				log.warn("Number of internal changes differs, expected {}, downloaded {}", ids.size(),
						changes.getInternalChange().size());
			}
		}
		result.setCount(changes.getInternalChange().size());
		return result;
	}

	/**
	 * Nacte vnejsi zmeny podle id.
	 *
	 * @param ids
	 *            id zmen, muze byt null nebo prazdny seznam
	 * @return odpoved obsahujici vsechny nalezene zmeny (vsechny stranky), pro
	 *         prazdny seznam id prazdna odpoved bez volani PEvA
	 */
	static ListExternalChangeResponse downloadExternalChanges(PEvA2Connection peva2, ConfigPeva2 config,
			List<String> ids) {
		var result = new ListExternalChangeResponse();
		var changes = new ListExternalChangeResponse.ExternalChanges();
		result.setExternalChanges(changes);
		if (ids != null && !ids.isEmpty()) {
			String searchAfter = null;
			while (true) {
				var req = new ListExternalChangeRequest();
				req.setSize(config.getBatchSize());
				req.setSearchAfter(searchAfter);
				req.setFilters(createIdsFilter(ids));
				var resp = peva2.getPeva().listExternalChange(req);
				int pageSize = 0;
				if (resp.getExternalChanges() != null) {
					pageSize = resp.getExternalChanges().getExternalChange().size();
					changes.getExternalChange().addAll(resp.getExternalChanges().getExternalChange());
				}
				searchAfter = resp.getSearchAfter();
				if (isLastPage(config, pageSize, searchAfter, changes.getExternalChange().size(), ids.size())) {
					break;
				}
			}
			if (changes.getExternalChange().size() != ids.size()) {
				log.warn("Number of external changes differs, expected {}, downloaded {}", ids.size(),
						changes.getExternalChange().size());
			}
		}
		result.setCount(changes.getExternalChange().size());
		return result;
	}

	private static Filters createIdsFilter(List<String> ids) {
		var idsFilter = new IdsFilter();
		idsFilter.getId().addAll(ids);
		var filters = new Filters();
		filters.getAndOrContainsOrEndWith().add(idsFilter);
		return filters;
	}

	private static boolean isLastPage(ConfigPeva2 config, int pageSize, String searchAfter, int numDownloaded,
			int numExpected) {
		return pageSize == 0 || pageSize < config.getBatchSize() || searchAfter == null
				|| numDownloaded >= numExpected;
	}

	/**
	 * Ulozi fond a jeho zmeny do pevafund-${jafa}.zip v adresari fondu.
	 *
	 * Zip obsahuje pevafund-${jafa}.xml, external_changes.xml a
	 * internal_changes.xml. Zapisuje se do docasneho souboru, ktery se po
	 * dokonceni prejmenuje.
	 */
	static void storeFundZip(Path fundDir, String fundId, GetNadSheetResponse gnsr,
			ListExternalChangeResponse externalChanges, ListInternalChangeResponse internalChanges)
			throws IOException, JAXBException {
		storeZip(fundDir, PREFIX_DASH + fundId, gnsr, externalChanges, internalChanges);
	}

	/**
	 * Ulozi list NAD a jeho zmeny do ${baseName}.zip v adresari dir.
	 *
	 * Zip obsahuje ${baseName}.xml, external_changes.xml a
	 * internal_changes.xml. Zapisuje se do docasneho souboru, ktery se po
	 * dokonceni prejmenuje.
	 */
	static void storeZip(Path dir, String baseName, GetNadSheetResponse gnsr,
			ListExternalChangeResponse externalChanges, ListInternalChangeResponse internalChanges)
			throws IOException, JAXBException {
		Path zipPath = dir.resolve(baseName + ".zip");
		Path tmpPath = dir.resolve(baseName + ".zip.tmp");
		try {
			try (var zos = new ZipOutputStream(Files.newOutputStream(tmpPath))) {
				zos.putNextEntry(new ZipEntry(baseName + ".xml"));
				Peva2XmlReader.marshalGetNadSheetResponse(gnsr, zos);
				zos.closeEntry();
				zos.putNextEntry(new ZipEntry(EXTERNAL_CHANGES_XML));
				Peva2XmlReader.marshalListExternalChangeResponse(externalChanges, zos);
				zos.closeEntry();
				zos.putNextEntry(new ZipEntry(INTERNAL_CHANGES_XML));
				Peva2XmlReader.marshalListInternalChangeResponse(internalChanges, zos);
				zos.closeEntry();
			}
			Files.move(tmpPath, zipPath, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(tmpPath);
		}
		// xml z predchoziho (nezpracovaneho) stazeni ve starem formatu
		Files.deleteIfExists(dir.resolve(baseName + ".xml"));
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
