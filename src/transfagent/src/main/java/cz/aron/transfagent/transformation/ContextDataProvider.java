package cz.aron.transfagent.transformation;

import java.util.List;
import java.util.UUID;

import cz.aron.transfagent.domain.ArchivalEntity;

/**
 * Interface for reading specific data 
 *
 */
public interface ContextDataProvider {

    InstitutionInfo getInstitutionApu(String instCode);

    List<ArchEntityInfo> getArchivalEntityWithParentsByElzaId(Integer elzaId);

    UUID getFundApu(String institutionCode, String fundCode);
    
    UUID getFundApuByUUID(String institutionCode, UUID fundUuid);

    List<ArchEntityInfo> getArchivalEntityWithParentsByUuid(UUID apUuid);

    UUID getDao(String daoHandle);
    
    List<ArchEntitySourceInfo> getArchEntityApuSources(List<UUID> uuids, String entityClass);
    
    /**
     * Get UUID of finding aids which exists and not in deleted state
     * @param fundUuid
     * @return
     */
    List<UUID> getValidFindingAids(UUID fundUuid);

    /**
     * Return basic information about finding aid
     * @param uuid of finding aid
     * @return FindingAidInfo
     */
    FindingAidInfo getFindingAidInfo(UUID uuid);
    
    /**
     * Get all archival entities with same UUID
     * @param uuid
     * @return list of entities
     */
    List<ArchivalEntity> getArchivalEntitiesByUuid(UUID uuid);
    
	/**
	 * Get all archival entities with same ccam UUID
	 * @param uuid
	 * @return list of entities
	 */
    List<ArchivalEntity> getArchivalEntitiesByCamUuid(UUID uuid);
    
    
}
