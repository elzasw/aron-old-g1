package cz.aron.core.model;

import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.ItemType;
import cz.aron.core.relation.RelationStore;
import cz.inqool.eas.common.domain.Domain;
import cz.inqool.eas.common.domain.DomainRepository;
import cz.inqool.eas.common.projection.Projection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@Repository
@Slf4j
public class ApuRepository extends DomainRepository<
        ApuEntity,
        ApuEntity,
        IndexedApu,
        ApuStore,
        ApuIndex> {

    @Inject private TypesHolder typesHolder;
    @Inject private RelationStore relationStore;

    public List<IdLabelDto> mapNames(Collection<String> ids) {
        return store.mapNames(ids);
    }

    public void massIndex(List<String> ids) {
        int batchSize = getReindexBatchSize();
        int fromIndex = 0;
        do {
            List<String> idsInBatch = ids.subList(fromIndex, Math.min(fromIndex + batchSize, ids.size()));
            List<ApuEntity> entitesToIndex = store.listByIds(idsInBatch);
            index.index(projectedToIndexable(indexProjectedType, entitesToIndex));
            fromIndex += batchSize;
        } while (fromIndex < ids.size());
    }

    @Override
    protected <PROJECTED extends Domain<ApuEntity>> IndexedApu projectedToIndexable(Class<PROJECTED> type, PROJECTED projected) {
        Projection<ApuEntity, ApuEntity, PROJECTED> entityProjection = projectionFactory.get(rootType, type);
        Projection<ApuEntity, ApuEntity, ApuEntity> indexProjection = projectionFactory.get(rootType, indexProjectedType);
        Projection<ApuEntity, ApuEntity, IndexedApu> indexedProjection = projectionFactory.get(indexProjectedType, indexableType);

        ApuEntity entity = entityProjection.toBase(projected);
        ApuEntity indexProjected = indexProjection.toProjected(entity);
        fillTargetLabelsToApuRefs(List.of(indexProjected));
        return indexedProjection.toProjected(indexProjected);
    }

    @Override
    protected <PROJECTED extends Domain<ApuEntity>> List<IndexedApu> projectedToIndexable(Class<PROJECTED> type, Collection<? extends PROJECTED> projected) {
        Projection<ApuEntity, ApuEntity, PROJECTED> entityProjection = projectionFactory.get(rootType, type);
        Projection<ApuEntity, ApuEntity, ApuEntity> indexProjection = projectionFactory.get(rootType, indexProjectedType);
        Projection<ApuEntity, ApuEntity, IndexedApu> indexedProjection = projectionFactory.get(indexProjectedType, indexableType);
        List<ApuEntity> indexProjected = projected.
                stream().
                map(entityProjection::toBase).
                map(indexProjection::toProjected).
                collect(Collectors.toList());
        for (ApuEntity apuEntity : indexProjected) {
            fillIncomingRels(apuEntity);
        }
        fillTargetLabelsToApuRefs(indexProjected);
        return indexProjected.stream().
                map(indexedProjection::toProjected).
                collect(Collectors.toList());
    }

    private void fillIncomingRels(ApuEntity targetApu) {
        Set<String> relationTypesByTarget = new HashSet(relationStore.findRelationTypesByTarget(targetApu.getId()));
        Set<String> relationGroupsByTarget = new HashSet<>();
        for (String relationType : relationTypesByTarget) {
            relationGroupsByTarget.addAll(typesHolder.getItemGroupsForItemType(relationType));
        }
        targetApu.getIncomingRelTypeGroups().addAll(relationGroupsByTarget);
        targetApu.getIncomingRelTypes().addAll(relationTypesByTarget);
    }

    private void fillTargetLabelsToApuRefs(Collection<ApuEntity> apus) {
        //Find all referred ids
        Set<String> idsToFind = new HashSet<>();
        for (ApuEntity apuEntity : apus) {
            for (ApuPart part : apuEntity.getParts()) {
                for (ApuPartItem item : part.getItems()) {
                    ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                    if (itemType == null) {
                        log.warn("unrecognized item type: " + item.getType());
                        continue;
                    }
                    if (itemType.getType() == DataType.APU_REF) {
                        idsToFind.add(item.getValue());
                    }
                }
            }
        }
        //Fetch their labels and put them to a map
        Map<String, String> idToLabelLookupMap = new HashMap<>();
        for (IdLabelDto idLabelDto : mapNames(idsToFind)) {
            idToLabelLookupMap.put(idLabelDto.getId(), idLabelDto.getLabel());
        }
        //Use the map to fill labels to items
        for (ApuEntity apuEntity : apus) {
            for (ApuPart part : apuEntity.getParts()) {
                for (ApuPartItem item : part.getItems()) {
                    ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                    if (itemType == null) {
                        log.warn("unrecognized item type: " + item.getType());
                        continue;
                    }
                    if (itemType.getType() == DataType.APU_REF) {
                        item.setTargetLabel(idToLabelLookupMap.get(item.getValue()));
                    }
                }
            }
        }
    }

    @Override
    public int getReindexBatchSize() {
        return 1000;
    }

}