package cz.aron.transfagent.transformation;

import java.util.List;
import java.util.UUID;

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

}
