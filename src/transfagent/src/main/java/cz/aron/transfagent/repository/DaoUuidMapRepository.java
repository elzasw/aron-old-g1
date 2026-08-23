package cz.aron.transfagent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.DaoUuidMap;

public interface DaoUuidMapRepository extends JpaRepository<DaoUuidMap, Integer> {

    Optional<DaoUuidMap> findByRepositoryAndHandle(String repository, String handle);

}
