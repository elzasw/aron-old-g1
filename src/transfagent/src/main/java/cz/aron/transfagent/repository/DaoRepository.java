package cz.aron.transfagent.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;

public interface DaoRepository extends JpaRepository<Dao, Integer> {

    @Query("SELECT d.id FROM Dao d JOIN d.apuSource a " +
            "WHERE NOT EXISTS(SELECT q FROM CoreQueue q WHERE q.apuSource = a) " +
            "AND d.state = ?1 AND d.transferred = ?2 " +
            "ORDER BY d.id")
    List<Integer> findTopByStateAndTransferredOrderById(DaoState state, boolean transferred, Pageable limit);
    
    Integer countByStateAndTransferred(DaoState state, boolean transferred);

    @EntityGraph(attributePaths = { "apuSource" })
    Optional<Dao> findById(int id);

    @EntityGraph(attributePaths = { "apuSource" })
    Optional<Dao> findByUuid(UUID uuid);

    Optional<Dao> findByHandle(String handle);

    @EntityGraph(attributePaths = { "apuSource" })
    List<Dao> findAllByUuidIn(List<UUID> uuids);

    List<Dao> findByApuSource(ApuSource apuSource);
    
    Optional<Dao> findByHandleAndSource(String handle, String source);

    List<Dao> findTop1000ByStateOrderById(DaoState state);
    
    @Query("UPDATE Dao d SET d.state='ACCESSIBLE', d.transferred=false WHERE d.uuid IN (:uuids) ")
    @Modifying
    int markForReimport(@Param("uuids") Collection<String> uuids);

}
