package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.IdProjection;
import cz.aron.transfagent.transformation.ArchEntitySourceInfo;

@Repository
public interface ArchivalEntityRepository extends JpaRepository<ArchivalEntity, Integer> {
    
    List<ArchivalEntity> findByUuidIn(List<UUID> batch);

    List<ArchivalEntity> findByUuidInAndStatus(List<UUID> uuids, EntityStatus status);
    
    Optional<ArchivalEntity> findByUuid(UUID uuid);
    
    Optional<ArchivalEntity> findByElzaId(Integer elzaId);
    
    List<IdProjection> findTop1000ByStatusOrderById(EntityStatus status);

	List<ArchivalEntity> findAllByParentEntity(ArchivalEntity archivalEntity);

	@EntityGraph(attributePaths = {"parentEntity"})
	ArchivalEntity findByApuSource(ApuSource apuSource);

	@Modifying
	@Query(nativeQuery = true, value = "with recursive subtree(entity_id) as (\r\n"
			+ " select ae.entity_id from archival_entity ae where ae.entity_id = :entId\r\n"
			+ " union \r\n"
			+ " select ae2.entity_id from archival_entity ae2 \r\n"
			+ " inner join subtree st on ae2.parent_entity_id = st.entity_id \r\n"
			+ " )\r\n"
			+ "update apu_source set reimport = true where \r\n"
			+ "  apusource_id in (\r\n"
			+ "select es.apusource_id from entity_source es\r\n"
			+ "join subtree as st on st.entity_id = es.entity_id );\r\n"
			)
	void reimportConnected(@Param("entId") Integer entId);

	@Query("select ae from ArchivalEntity ae where ae.elzaId in (:ids)")
    List<ArchivalEntity> findByElzaIds(@Param("ids") List<Integer> ids);

    @Query(nativeQuery = true, value="WITH RECURSIVE cte(uuid, entity_class, parent_entity_id, depth) as "
            + "( "
            + "SELECT uuid, entity_class, parent_entity_id, 1 as depth "
            + "FROM archival_entity ae "
            + "WHERE ae.elza_id = ?1 "
            + "UNION ALL "
            + "SELECT ae2.uuid, ae2.entity_class, ae2.parent_entity_id, cte.depth+1 "
            + "FROM archival_entity ae2, cte "
            + "WHERE ae2.entity_id = cte.parent_entity_id "
            + ") "
            + "SELECT CAST(uuid as VARCHAR(50)), entity_class "
            + "FROM cte "
            + "ORDER BY depth")
    List<Object[]> findByElzaIdWithParents(Integer elzaId);

    @Query(nativeQuery = true, value="WITH RECURSIVE cte(uuid, entity_class, parent_entity_id, depth) as "
            + "( "
            + "SELECT uuid, entity_class, parent_entity_id, 1 as depth "
            + "FROM archival_entity ae "
            + "WHERE ae.uuid = ?1 "
            + "UNION ALL "
            + "SELECT ae2.uuid, ae2.entity_class, ae2.parent_entity_id, cte.depth+1 "
            + "FROM archival_entity ae2, cte "
            + "WHERE ae2.entity_id = cte.parent_entity_id "
            + ") "
            + "SELECT CAST(uuid as VARCHAR(50)), entity_class "
            + "FROM cte "
            + "ORDER BY depth")
    List<Object[]> findByUUIDWithParents(UUID uuid);

    @Modifying
	@Query("update ArchivalEntity ae set ae.download = true where ae.elzaId in :ids")
    void setDownloadTrueByIds(@Param("ids") List<Integer> ids);

    /**
     * Vrati seznam archivnich entit, u kterych je potreba nastavit stav NOT_ACCESSIBLE
     * @return List<ArchivalEntity>
     */
    @Query("SELECT ae FROM ArchivalEntity ae WHERE ae NOT IN (SELECT DISTINCT es.archivalEntity FROM EntitySource es) AND ae.status<>'NOT_ACCESSIBLE'")
    List<ArchivalEntity> findNewlyUnaccesibleEntities();

    @Query("SELECT new cz.aron.transfagent.transformation.ArchEntitySourceInfo(ae.uuid, apuS.dataDir, apuS.uuid, ae.status, apuS.lastSent) "
    		+ "FROM ArchivalEntity ae "
    		+ "JOIN ae.apuSource apuS "
    		+ "WHERE ae.uuid in (:uuids) AND ae.entityClass=:entityClass AND ae.status in ('ACCESSIBLE', 'AVAILABLE')")
    List<ArchEntitySourceInfo> getArchEntityApuSources(@Param("uuids") List<UUID> uuids, @Param("entityClass") String entityClass);

    @Query("SELECT new cz.aron.transfagent.transformation.ArchEntitySourceInfo(ae.uuid, apuS.dataDir, apuS.uuid, ae.status, apuS.lastSent, max(cq.id)) "
    		+ "FROM ArchivalEntity ae "
    		+ "JOIN ae.apuSource apuS "
    		+ "LEFT JOIN CoreQueue cq ON cq.apuSource=apuS "
    		+ "WHERE ae.uuid in (:uuids) AND ae.status in ('ACCESSIBLE', 'AVAILABLE') "
    		+ "GROUP BY ae.uuid, apuS.dataDir, apuS.uuid, ae.status, apuS.lastSent")
    List<ArchEntitySourceInfo> getArchEntityApuSourcesWithScheduled(@Param("uuids") List<UUID> uuids);
    
}
