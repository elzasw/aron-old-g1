package cz.aron.core.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.aron.apux._2020.*;
import cz.aron.core.model.ApuSource;
import cz.aron.core.model.ApuType;
import cz.aron.core.model.*;
import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.ItemType;
import cz.aron.core.relation.Relation;
import cz.aron.core.relation.RelationRepository;
import cz.aron.core.relation.RelationStore;
import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
@Slf4j
public class ApuProcessor {
    @Inject private ApuSourceRepository apuSourceRepository;
    @Inject private ApuRepository apuRepository;
    @Inject private ApuStore apuStore;
    @Inject private DigitalObjectStore daoStore;
    @Inject private FileInputProcessor fileInputProcessor;
    @Inject private ObjectMapper objectMapper;
    @Inject private RelationRepository relationRepository;
    @Inject private TypesHolder typesHolder;
    @Inject private ApuRequestQueue apuRequestQueue;
    @Inject private RelationStore relationStore;

    private Map<String, ApuEntity> saveCache = new LinkedHashMap<>();   //maintain order so that parent always comes before child
    private Set<String> relationsDeleteCache = new HashSet<>();
    private Set<Relation> relationsAddCache = new HashSet<>();
    private Set<String> apusToHaveIncomingRelsUpdated = new HashSet<>();
    private Map<String, LevelStats> apuIdsStates = new HashMap<>();
    private Map<String, DigitalObject> existingDaos = new HashMap<>();

    private static final int CACHE_SIZE = 100;

    private int apuOrderCounter;

    public void processApuAndFiles(Path apuSrcPath, Map<String, Path> filesMap) {
    	
    	preprocess(apuSrcPath);
    	    	
        try(ApuSourceBatchReader reader = new ApuSourceBatchReader(apuSrcPath);) {
            log.debug("Processing apu source {}", reader.getUuid());

            // read apusrc.xml as String to be stored to database
            String metadata = Files.readString(apuSrcPath, StandardCharsets.UTF_8);
            
            ApuSource ourApuSource = apuSourceRepository.find(reader.getUuid());
            boolean create = false;
            if (ourApuSource == null) {
                create = true;
                ourApuSource = new cz.aron.core.model.ApuSource();
                ourApuSource.setId(reader.getUuid());
            }
            else {
                long numDisconnected = daoStore.disconnectDaosByApuSourceId(reader.getUuid());
                log.debug("Processing apu source {}, disconnect {} existing daos ", reader.getUuid(), numDisconnected);
                List<String> apusToDelete = apuStore.findIdsBySourceIdBottomUp(reader.getUuid());
                apuRepository.delete(apusToDelete);
                log.debug("Processing apu source {}, original data deleted", reader.getUuid());
            }
            ourApuSource.setData(metadata);
            ourApuSource.setPublished(LocalDateTime.now());

            if (create) {
                apuSourceRepository.create(ourApuSource);
            }
            else {
                apuSourceRepository.update(ourApuSource);
            }
            // hold only reference, ApuSource.data are potentialy large
            var apuSourceRef = apuSourceRepository.getRef(ourApuSource.getId());

            apuOrderCounter = 0;
            reader.process(apus->{                               
                fillDaoCache(apus.stream().filter(apu -> apu.getDaos() != null).flatMap(apu -> apu.getDaos()
                        .getUuid().stream())
                        .collect(Collectors.toList()));
                for (Apu apu : apus) {
                    processApu(apu, apuSourceRef, filesMap);
                }
                flush();
                log.debug("Processing apu source {}, process chunk of size {}", reader.getUuid(), apus.size());                
            }, CACHE_SIZE);
        } catch (Exception e) {
            log.error("Fail to import apusource ", e);
            throw new RuntimeException(e);
        } finally {
            clearInternalState();
        }
        
    }

    private void preprocess(Path apuSrcPath) {
    	var fictiveRoot = new LevelStats();
    	fictiveRoot.depth = 0;
    	fictiveRoot.pos = 0;
    	boolean clearState = true;	
    	try(ApuSourceBatchReader reader = new ApuSourceBatchReader(apuSrcPath);) {
    		log.debug("Processing first phase apu source {}", reader.getUuid());    		
    		reader.process(apus->{
    			for(var apu:apus) {
    				var levelStats = new LevelStats();
    				var parentId = apu.getPrnt();
    				LevelStats parent;
    				if (parentId!=null) {
    					parent = apuIdsStates.get(parentId);
    					if (parent==null) {
    						log.error("Parent {} not exist", parentId);
    						throw new RuntimeException("parent not exist"+parentId);
    					}    					    				
    				} else {
    					parent = fictiveRoot;
    				}
    				parent.childCnt++;
					levelStats.depth = parent.depth + 1;
					levelStats.pos = parent.childCnt;    				
    				apuIdsStates.put(apu.getUuid(), levelStats);
    			}
    		}, CACHE_SIZE);
    		clearState = false;
    	 } catch (Exception e) {
             log.error("Fail to import apusource, preprocess phase ", e);
             throw new RuntimeException(e);
    	} finally {
    		if (clearState) {
    			clearInternalState();
    		}
    	}
    }
    
    private void clearInternalState() {
        saveCache.clear();
        relationsDeleteCache.clear();
        relationsAddCache.clear();
        apusToHaveIncomingRelsUpdated.clear();
        apuIdsStates.clear();
        existingDaos.clear();
    }
    
    @Scheduled(fixedDelay = 60000)
    public void sendRequests() {        
        try {
            while(apuRequestQueue.sendRequestsBatch());
        } catch (Exception e) {
            log.error("Fail to send apu requests batch", e);
        }        
    }

    public void processTestingInputStream(Path path) throws IOException {        
        processApuAndFiles(path, null);
    }

    public void processApu(Apu apu, cz.aron.core.model.ApuSource apuSource, Map<String, Path> filesMap) {    	
    	var levelState = apuIdsStates.get(apu.getUuid());    	
        ApuEntity apuEntity = new ApuEntity();
        apuEntity.setId(apu.getUuid());
        apuEntity.setName(apu.getName());
        apuEntity.setOrder(++apuOrderCounter);
        apuEntity.setDescription(apu.getDesc());
        apuEntity.setPermalink(apu.getPrmLnk());
        apuEntity.setType(ApuType.valueOf(apu.getType().name().toUpperCase())); //fixme names don't match
        apuEntity.setChildCnt(levelState.childCnt);
        apuEntity.setPos(levelState.pos);
        apuEntity.setDepth(levelState.depth);
        if (apu.getPrnt() != null) {
            ApuEntity parentApu = saveCache.get(apu.getPrnt());
            if (parentApu == null) {
            	var parentLevelStats =  apuIdsStates.get(apu.getPrnt());
                if (parentLevelStats==null||!parentLevelStats.processed) {
                    throw new RuntimeException("parent apu not found yet");
                }
                parentApu = apuRepository.getRef(apu.getPrnt());
            }
            apuEntity.setParent(parentApu);
        }
        apuEntity.setSource(apuSource);
        if (apu.getPrts() != null) {
            processParts(apu.getPrts().getPart(), apuEntity);
        }
        processAttachments(apu.getAttchs(), apuEntity, filesMap);
        if (apu.getDaos() != null) {
            int i = 0;
            for (String daoUuid : apu.getDaos().getUuid()) {
                DigitalObject insertedDao = existingDaos.get(daoUuid);
                if (insertedDao == null) {
                    insertedDao = new DigitalObject();
                    insertedDao.setId(daoUuid);
                }
                insertedDao.setApu(apuEntity);
                insertedDao.setOrder(++i);
                apuEntity.getDigitalObjects().add(insertedDao);
            }
        }
        levelState.processed = true;
        saveCache.put(apuEntity.getId(), apuEntity);
        apusToHaveIncomingRelsUpdated.add(apuEntity.getId());
        /*
        if (saveCache.size() > CACHE_SIZE) {
            flush();
        }
        */
        recordRelations(apuEntity);
        apuRequestQueue.removeForApuId(apuEntity.getId());        
    }

    private void processParts(List<Part> parts, ApuEntity apuEntity) {
        Map<String, ApuPart> processedPartCache = new HashMap<>();
        for (Part part : parts) {
            ApuPart apuPart = new ApuPart();
            if (part.getId() != null) {
                apuPart.setId(part.getId());
            }
            apuPart.setValue(part.getValue());
            apuPart.setType(part.getType().replace("_", "~"));
            Object prnt = part.getPrnt();
            if (prnt != null) {
                String parentPartId = ((Part) prnt).getId();
                ApuPart parentPart = processedPartCache.get(parentPartId);
                if (parentPart == null) {
                    throw new RuntimeException("parent part not processed yet, move the connecting at the end");
                }
                apuPart.setParentPart(parentPart);
                parentPart.getChildParts().add(apuPart);
            }
            else {
                apuPart.setApu(apuEntity);
                apuEntity.getParts().add(apuPart);
                processedPartCache.put(apuPart.getId(), apuPart);
            }
            processPartItems(part.getItms(), apuPart);
        }
    }

    private void processPartItems(DescItems itms, ApuPart apuPart) {
        for (Object o : itms.getStrOrLnkOrEnm()) {
            ApuPartItem item = new ApuPartItem();
            if (o instanceof ItemString) {
                ItemString itemString = (ItemString) o;
                item.setType(itemString.getType().replace("_", "~"));
                item.setValue(itemString.getValue());
                item.setVisible(itemString.isVisible() == null || itemString.isVisible());
            }
            else if(o instanceof ItemLink) {
                ItemLink itemLink = (ItemLink) o;
                item.setType(itemLink.getType().replace("_", "~"));
                item.setValue(itemLink.getName());
                item.setHref(itemLink.getLink());
                item.setVisible(itemLink.isVisible() == null || itemLink.isVisible());
            }
            else if(o instanceof ItemEnum) {
                ItemEnum itemEnum = (ItemEnum) o;
                item.setType(itemEnum.getType().replace("_", "~"));
                item.setValue(itemEnum.getValue());
                item.setVisible(itemEnum.isVisible() == null || itemEnum.isVisible());
            }
            else if(o instanceof ItemRef) {
                ItemRef itemRef = (ItemRef) o;
                item.setType(itemRef.getType().replace("_", "~"));
                item.setValue(itemRef.getValue());
                item.setVisible(itemRef.isVisible() == null || itemRef.isVisible());
                if (itemRef.getType().equals("ORIGINATOR_REF") || itemRef.getType().equals("AP_REF")) { //only archival entities
                    apuRequestQueue.add(itemRef.getValue(), apuPart.findRootApuEntity().getId());
                }
            }
            else if(o instanceof ItemDateRange) {
                ItemDateRange itemDateRange = (ItemDateRange) o;
                item.setType(itemDateRange.getType().replace("_", "~"));
                UniversalDate universalDate = new UniversalDate();
                universalDate.setFormat(itemDateRange.getFmt());
                universalDate.setFrom(itemDateRange.getF());
                universalDate.setTo(itemDateRange.getTo());
                universalDate.setValueFromEstimated(itemDateRange.isFe() != null && itemDateRange.isFe());
                universalDate.setValueToEstimated(itemDateRange.isToe() != null && itemDateRange.isToe());
                try {
                    item.setValue(objectMapper.writeValueAsString(universalDate));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                item.setVisible(itemDateRange.isVisible() == null || itemDateRange.isVisible());
            }
            else if (o instanceof ItemJson) {
                ItemJson itemJson = (ItemJson) o;
                item.setType(itemJson.getType().replace("_", "~"));
                item.setValue(itemJson.getValue());
                item.setVisible(itemJson.isVisible() == null || itemJson.isVisible());
            }
            item.setApuPart(apuPart);
            apuPart.getItems().add(item);
        }
    }

    private void processAttachments(List<Attachment> attchs, ApuEntity apuEntity, Map<String, Path> filesMap) {
        if (attchs != null) {
            int i = 0;
            for (Attachment attch : attchs) {
                ApuAttachment apuAttachment = new ApuAttachment();
                apuAttachment.setName(attch.getName());
                apuAttachment.setOrder(++i);
                fileInputProcessor.processFile(
                        attch.getFile(),
                        DigitalObjectType.PUBLISHED,
                        apuAttachment,
                        null,
                        filesMap);
                apuAttachment.setApu(apuEntity);
                apuEntity.getAttachments().add(apuAttachment);
            }
        }
    }

    private void recordRelations(ApuEntity apuEntity) {
        relationsDeleteCache.add(apuEntity.getId());
        for (ApuPart part : apuEntity.getParts()) {
            for (ApuPartItem item : part.getItems()) {
                ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                if (itemType != null && itemType.getType() == DataType.APU_REF) {
                    Relation relation = new Relation();
                    relation.setSource(apuEntity.getId());
                    relation.setRelation(item.getType());
                    relation.setTarget(item.getValue());
                    relationsAddCache.add(relation);
                    apusToHaveIncomingRelsUpdated.add(item.getValue());
                }
            }
        }
        /*
        if (relationsDeleteCache.size() > CACHE_SIZE || relationsAddCache.size() > CACHE_SIZE) {
            flush();
        }*/
    }

    private void flush() {
        //save outgoing relations
        Set<String> relationIdsToDelete = new HashSet<>();
        for (String apuId : relationsDeleteCache) {
            relationIdsToDelete.addAll(relationStore.findIdsBySource(apuId));
        }
        relationRepository.delete(relationIdsToDelete);
        relationsDeleteCache.clear();
        relationRepository.create(relationsAddCache);
        relationsAddCache.clear();

        //save apus (indexing uses relations table to index incoming relation type groups)
        apuRepository.create(saveCache.values());
        //Now to reindex all apus that reference these apus, to update labels in them
        List<String> updatedApusIds = saveCache.values().stream().map(DomainObject::getId).collect(Collectors.toList());
        List<String> apuIdsTargetingUpdatedIds = relationStore.findIdsByTarget(updatedApusIds);
        apuRepository.massIndex(apuIdsTargetingUpdatedIds);
        //clear for next batch
        saveCache.clear();

        //reindex incoming relations on target apus
        apuRepository.reindex(new ArrayList<>(apusToHaveIncomingRelsUpdated));
        apusToHaveIncomingRelsUpdated.clear();
    }

    private void fillDaoCache(List<String> daoIds) {
        existingDaos.clear();
        if (!daoIds.isEmpty()) {
            daoStore.listByIds(daoIds).forEach(dao -> existingDaos.put(dao.getId(), dao));
        }
    }

    private class LevelStats {
    	private int depth;
    	private int pos;
    	private int childCnt = 0;
    	private boolean processed = false;
        	
    }

}
