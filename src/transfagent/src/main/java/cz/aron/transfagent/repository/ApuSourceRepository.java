package cz.aron.transfagent.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.SourceType;

@Repository
public interface ApuSourceRepository extends JpaRepository<ApuSource, Integer> {
    
    Optional<ApuSource> findByUuid(UUID uuid);

    List<ApuSource> findFirst1000ByReimport(boolean reimport);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE apu_source AS a "
            + "SET reimport = true WHERE a.apusource_id in :ids")
    void setReimportTrueByIds(@Param("ids") List<Integer> ids);
    
    @Modifying
	@Query(nativeQuery = true, value = "UPDATE apu_source AS a "
			+ "SET reimport = true FROM finding_aid fa "
			+ "WHERE fa.apusource_id = a.apusource_id AND fa.institution_id = :id")
	void reimportFindingAidByInstitution(@Param("id") Integer id);
	
    @Modifying
	@Query(nativeQuery = true, value = "UPDATE apu_source AS a "
			+ "SET reimport = true FROM fund fu "
			+ "WHERE fu.apusource_id = a.apusource_id AND fu.institution_id = :id")
	void reimportFundAidByInstitution(@Param("id") Integer id);
    
    @Modifying
    @Query(nativeQuery=true, value="UPDATE apu_source AS a "
    		+ "SET last_sent=:lastSent "
    		+ "WHERE a.apusource_id=:apuSourceId")
    void setLastSent(@Param("apuSourceId") int apuSourceId, @Param("lastSent") long lastSent);
    
    @Modifying
    @Query("UPDATE ApuSource a SET a.reimport = true WHERE a.sourceType = :type")
    void reimportAllByType(SourceType type);
    
	@Query("SELECT a.id FROM Attachment att JOIN att.apuSource a WHERE a.sourceType = :sourceType")
    Stream<Integer> findIdsBySourceTypeAndAttachment(@Param("sourceType") SourceType sourceType);

	/**
	 * Get ids of apu sources that are related to fund from given institution
	 * @param institutionIds ids of institutions
	 * @return {@code Stream<Integer>}
	 */
	@Query("SELECT a.id FROM Attachment att JOIN att.apuSource a JOIN Fund f ON f.apuSource = a WHERE f.institution.id IN :institutionIds")
    Stream<Integer> findIdsByFundAndAttachmentAndInstitution(@Param("institutionIds") Collection<Integer> institutionIds);

	/**
	 * Get ids of apu sources that are related to finding aid from given institution
	 * @param institutionIds ids of institutions
	 * @return {@code Stream<Integer>}
	 */
	@Query("SELECT a.id FROM Attachment att JOIN att.apuSource a JOIN FindingAid fa ON fa.apuSource = a WHERE fa.institution.id IN :institutionIds")
    Stream<Integer> findIdsByFindingAidAndAttachmentAndInstitution(@Param("institutionIds") Collection<Integer> institutionIds);
	
}
