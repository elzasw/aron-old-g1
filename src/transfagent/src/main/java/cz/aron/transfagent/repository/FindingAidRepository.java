package cz.aron.transfagent.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.dto.UuidNumAttachmentsNumDaos;

public interface FindingAidRepository extends JpaRepository<FindingAid, Integer> {

    FindingAid findByCode(String code);

    @EntityGraph(attributePaths = { "apuSource" })
    FindingAid findByApuSource(ApuSource apuSource);

    @EntityGraph(attributePaths = { "apuSource" })
	FindingAid findByUuid(UUID uuid);
	
	@Query("SELECT new cz.aron.transfagent.domain.dto.UuidNumAttachmentsNumDaos(fa.uuid, aps.numAttachments, aps.numDaos) FROM FindingAid fa INNER JOIN fa.funds f INNER JOIN fa.apuSource aps WHERE f.uuid = :fundUuid AND fa.apuSource.deleted = false")
	List<UuidNumAttachmentsNumDaos> findValidByFundUuid(UUID fundUuid);
	
	@EntityGraph(attributePaths = { "apuSource" })
	FindingAid findByInstitutionIdAndEvidenceNumberAndPartNumberIsNull(int institutionId, int evidenceNumber);
	
	@EntityGraph(attributePaths = { "apuSource" })
	FindingAid findByInstitutionIdAndEvidenceNumberAndPartNumber(int institutionId, int evidenceNumber, int partNumber);

}
