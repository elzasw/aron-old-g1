package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.FindingAid;
import cz.aron.peva2.wsdl.GetFindingAidRequest;
import cz.aron.peva2.wsdl.GetFindingAidResponse;
import cz.aron.peva2.wsdl.ListFindingAidRequest;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.AttachmentSource;
import cz.aron.transfagent.service.StorageService;

public class Peva2ImportFindingAids extends Peva2Downloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2ImportFindingAids.class);
	
	private static final String PREFIX_DASH = "pevafa-";
	
	private final String institutionPrefix;
	
	private final AttachmentSource attachmentSource;

    public Peva2ImportFindingAids(PEvA2Connection peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
                                  TransactionTemplate tt, StorageService storageService, AttachmentSource attachmentSource,
                                  boolean active) {
        super("FINDINGAID", peva2, propertyRepository, config, tt, storageService, active);
        institutionPrefix = PREFIX_DASH + peva2.getInstitutionId() + "-";
        this.attachmentSource = attachmentSource;
    }

	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial, Peva2CodeListProvider codeListProvider) {		
        
        String searchAfter = searchAfterInitial;
        int numUpdated = 0;
        Map<String, List<Path>> attachments = null;
        while (true) {
            var lfar = new ListFindingAidRequest();
            lfar.setSize(config.getBatchSize());
            lfar.setUpdatedAfter(updateAfter);
            lfar.setSearchAfter(searchAfter);
            var lfaResp = peva2.getPeva().listFindingAid(lfar);            
            searchAfter = lfaResp.getSearchAfter();
            long count = lfaResp.getFindingAids().getFindingAid().size();
            log.info("Downloaded {} finding aids to update after {}", count, updateAfter);
            if (count == 0) {
                break;
            }
            
            if (attachments==null) {
            	attachments = initAttachments();
            }
            
            int numUpdatedFromBatch = 0;
            try {
                patchFindingAidsBatch(lfaResp.getFindingAids().getFindingAid(), codeListProvider.getCodeLists(),attachments);
                numUpdatedFromBatch+=lfaResp.getFindingAids().getFindingAid().size();
            } catch (Exception e) {
                log.error("Fail to update finding aids batch", e);
                // zkusim to po jednom
                for (var findingAid : lfaResp.getFindingAids().getFindingAid()) {
                    try {
                        patchFindingAidsBatch(Collections.singletonList(findingAid), codeListProvider.getCodeLists(),attachments);
                        numUpdatedFromBatch++;
                    } catch (Exception e1) {
                        log.error("Fail to update finding aid {}", findingAid.getId(), e1);
                    }
                }                
            }

            storeSearchAfter(searchAfter);            
            numUpdated += numUpdatedFromBatch;
            log.info("Updated {}/{} finding aids", numUpdated,
                     lfaResp.getCount());
        }
        log.info("Finding aids synchronized");
        return numUpdated;
	}

	private void patchFindingAidsBatch(List<FindingAid> findingAids, Peva2CodeLists codeLists,
			Map<String, List<Path>> attachments) {
		var faInputDir = storageService.getInputPath().resolve("findingaids");
		for (var findingAid : findingAids) {
			if (findingAid.getNadSheets() == null || findingAid.getNadSheets().getNadSheet() == null
					|| findingAid.getNadSheets().getNadSheet().isEmpty()) {
				log.warn("Finding aid without NadSheet institution:{}, finding aid:{}, id:{}",
						findingAid.getInstitution().getExternalId(), findingAid.getEvidenceNumber(),
						findingAid.getId());
				continue;
			}
			var gfar = new GetFindingAidResponse();
			gfar.setFindingAid(findingAid);
			String id = findingAid.getInstitution().getExternalId() + "_" + findingAid.getId();
			var faDir = faInputDir.resolve(PREFIX_DASH + id);
			try {
				Files.createDirectories(faDir);
				var attachmentFiles = attachments.get(findingAid.getEvidenceNumber());
				//TODO refactor as attachmentSource
				if (attachmentFiles != null) {
					for (var attachmentFile : attachmentFiles) {
						Files.copy(attachmentFile, faDir.resolve(attachmentFile.getFileName()));
					}
				}
				copyAttachments(findingAid.getInstitution().getExternalId(), findingAid.getEvidenceNumber(), faDir);
                Path name = faDir.resolve(PREFIX_DASH + id + ".xml");
                Peva2XmlReader.marshalGetFindingAidResponse(gfar, name);
			} catch (Exception e) {
				log.error("Fail to store peva finding aid {} to input dir", findingAid.getId(), e);
			}
		}
	}
	
	private Map<String, List<Path>> initAttachments() {
		if (config.getAttachmentDir() == null) {
			return Collections.emptyMap();
		} else {									
			Pattern pattern = Pattern.compile("^(\\d+)_?(.*$)");
			Map<String, List<Path>> ret = new HashMap<>();
			Path attachmentDir = Path.of(config.getAttachmentDir());			
			try (var stream = Files.list(attachmentDir)) {
				stream.forEach(p->{
					if (Files.isDirectory(p)) {
						var fileName = p.getFileName().toString();
						Matcher m = pattern.matcher(fileName);					
						if (m.matches()) {
							var code = m.group(1);
							// odstraneni pocatecnich nul
							code = ""+Integer.parseInt(code);
							var pathList = ret.get(code);
							if (pathList==null) {
								pathList = new ArrayList<Path>();
								ret.put(code, pathList);
							}
							
							final var pathListFinal = pathList;
							try (var files=Files.list(p)) {
								files.forEach(f->{
									if (Files.isRegularFile(f)) {
										pathListFinal.add(f);
									}									
								});
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
						}					
					}					
				});				
			} catch (IOException e) {
				log.error("Fail to init finding aid attachments dir {}",config.getAttachmentDir());
				throw new UncheckedIOException(e);
			}			
			return ret;
		}
	}
	
    private void copyAttachments(String institutionCode, String findingAidCode, Path findingAidDir)
            throws IOException {
        if (attachmentSource == null) {
            return;
        }
        var attachments = attachmentSource.getFindingAidAttachments(institutionCode, findingAidCode);
        for (var attachment : attachments) {
            Files.copy(attachment.getPath(), findingAidDir.resolve(attachment.getName()));
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
		var gfaReq = new GetFindingAidRequest();
		gfaReq.setId(id);
		var gfaResp = peva2.getPeva().getFindingAid(gfaReq);
		var attMap = initAttachments();
		patchFindingAidsBatch(Collections.singletonList(gfaResp.getFindingAid()), codeListProvider.getCodeLists(),
				attMap);
		return true;
	}

    @Override
    protected String getName() {
        return super.getName() + "-" + peva2.getInstitutionId();
    }
    
    @Configuration
    @ConditionalOnProperty(value = "peva2.url")
    public static class Peva2ImportFindingAidsConfig {

        private final PropertyRepository propertyRepository;

        private final ConfigPeva2 config;

        private final TransactionTemplate tt;

        private final StorageService storageService;
        
        private final AttachmentSource attachmentSource;

        private final boolean active;

        public Peva2ImportFindingAidsConfig(PropertyRepository propertyRepository, ConfigPeva2 config,
                                            TransactionTemplate tt, StorageService storageService,
                                            @Nullable AttachmentSource attachmentSource,
                                            @Value("${peva2.importFindingAid:false}") boolean active) {
            this.propertyRepository = propertyRepository;
            this.config = config;
            this.tt = tt;
            this.storageService = storageService;
            this.attachmentSource = attachmentSource;
            this.active = active;
        }

        @Bean
        @Scope("prototype")
        public Peva2ImportFindingAids importFindingAids(PEvA2Connection peva2) {
            return new Peva2ImportFindingAids(peva2, propertyRepository, config, tt, storageService, attachmentSource,
                    active);
        }

    }

}
