package cz.aron.core.model;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import cz.inqool.eas.common.domain.store.DomainStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Iterables;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Repository
public class ApuStore extends DomainStore<ApuEntity, ApuEntity, QApuEntity> {

    private static final int BATCH_SIZE = 1000;
    
    @Value("${tree.levelSize:100}")
    private Integer levelSize;

    public ApuStore() {
        super(ApuEntity.class);
    }

    public List<String> findIdsBySourceIdBottomUp(String id) {  //ordered by parent field dependencies so that they can be deleted in order
        List<Tuple> fetch = query().select(metaModel.id, metaModel.parent.id).from(metaModel).where(metaModel.source.id.eq(id)).fetch();
        Map<String, String> idToParentIdMap = new HashMap<>();
        for (Tuple tuple : fetch) {
            idToParentIdMap.put(tuple.get(metaModel.id), tuple.get(metaModel.parent.id));
        }
        //Pick ids from top of tree
        Set<String> topToBottomIds = new LinkedHashSet<>();
        while (!idToParentIdMap.isEmpty()) {
            Iterator<Map.Entry<String, String>> iterator = idToParentIdMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> next = iterator.next();
                if (next.getValue() == null || topToBottomIds.contains(next.getValue())) {
                    topToBottomIds.add(next.getKey());
                    iterator.remove();
                }
            }
        }
        //Reverse
        ArrayList<String> resultList = new ArrayList<>(topToBottomIds);
        Collections.reverse(resultList);
        return resultList;
    }

    public List<IdLabelDto> mapNames(Collection<String> ids) {
        var ret = new ArrayList<IdLabelDto>(ids.size());
        Iterables.partition(ids, BATCH_SIZE).forEach(partition -> {
            ret.addAll(query().select(Projections.constructor(IdLabelDto.class, metaModel.id, metaModel.name)).from(metaModel).where(metaModel.id.in(ids)).fetch());
        });
        return ret;
    }
    
    public LocalDateTime findApuSourcePublished(String id) {
        return query().select(metaModel.source.published).from(metaModel).innerJoin(metaModel.source,QApuSource.apuSource).where(metaModel.id.eq(id)).fetchOne();
    }
    
	public List<ApuEntityTreeViewDto> getEntitiesBefore(String apuId) {
		var entity = query().select(metaModel).from(metaModel).where(metaModel.id.eq(apuId)).fetchOne();
		if (entity.getParent() != null) {			
			var result = query()
					.select(Projections.constructor(ApuEntityTreeViewDto.class, metaModel.id, metaModel.name,
							metaModel.description, metaModel.depth, metaModel.pos, metaModel.childCnt))
					.from(metaModel)
					.where(metaModel.parent.id.eq(entity.getParent().getId()).and(metaModel.pos.lt(entity.getPos())))
					.orderBy(metaModel.pos.desc())
					.limit(levelSize)
					.fetch();
			Collections.reverse(result);
			return result;
		} else {
			var result = query()
					.select(Projections.constructor(ApuEntityTreeViewDto.class, metaModel.id, metaModel.name,
							metaModel.description, metaModel.depth, metaModel.pos, metaModel.childCnt))
					.from(metaModel)
					.where(metaModel.source.id.eq(entity.getSource().getId()).and(metaModel.parent.isNull()).and(metaModel.pos.lt(entity.getPos())))
					.orderBy(metaModel.pos.desc())
					.limit(levelSize)
					.fetch();
			Collections.reverse(result);
			return result;
		}
	}
    
	public List<ApuEntityTreeViewDto> getEntitiesAfter(String apuId) {
		var entity = query().select(metaModel).from(metaModel).where(metaModel.id.eq(apuId)).fetchOne();
		if (entity.getParent() != null) {			
			var result = query()
					.select(Projections.constructor(ApuEntityTreeViewDto.class, metaModel.id, metaModel.name,
							metaModel.description, metaModel.depth, metaModel.pos, metaModel.childCnt))
					.from(metaModel)
					.where(metaModel.parent.id.eq(entity.getParent().getId()).and(metaModel.pos.gt(entity.getPos())))
					.orderBy(metaModel.pos.asc())
					.limit(levelSize)
					.fetch();
			return result;
		} else {
			var result = query()
					.select(Projections.constructor(ApuEntityTreeViewDto.class, metaModel.id, metaModel.name,
							metaModel.description, metaModel.depth, metaModel.pos, metaModel.childCnt))
					.from(metaModel)
					.where(metaModel.source.id.eq(entity.getSource().getId()).and(metaModel.parent.isNull()).and(metaModel.pos.gt(entity.getPos())))
					.orderBy(metaModel.pos.asc())
					.limit(levelSize)
					.fetch();
			return result;
		}
	}

	public List<ApuEntityTreeViewDto> getEntitiesUnder(String apuId) {
		var result = query()
				.select(Projections.constructor(ApuEntityTreeViewDto.class, metaModel.id, metaModel.name,
						metaModel.description, metaModel.depth, metaModel.pos, metaModel.childCnt))
				.from(metaModel)
				.where(metaModel.parent.id.eq(apuId))
				.orderBy(metaModel.pos.asc())
				.limit(levelSize)
				.fetch();
		return result;
	}
}
