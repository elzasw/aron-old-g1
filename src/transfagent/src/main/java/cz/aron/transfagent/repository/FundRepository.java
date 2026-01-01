package cz.aron.transfagent.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;

public interface FundRepository extends JpaRepository<Fund, Integer> {

    @EntityGraph(attributePaths = { "apuSource" })
    Fund findByCode(String code);

    @EntityGraph(attributePaths = { "apuSource" })
    Fund findByCodeAndInstitution(String code, Institution institution);
    
    Fund findByUuidAndInstitution(UUID uuid, Institution institution);

    Fund findByApuSource(ApuSource apuSource);
    
    Fund findByUuid(UUID uuid);
    
    @EntityGraph(attributePaths = { "apuSource" })
	Fund findByInstitutionIdAndNadAndSubNadIsNull(int institutionId, int nad);

    @EntityGraph(attributePaths = { "apuSource" })
	Fund findByInstitutionIdAndNadAndSubNad(int institutionId, int nad, int subNad);

}
