package cz.aron.transfagent.transformation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.map.LRUMap;


/**
 * Cache pro nacitani archivnich entit s jejich predky
 */
public class CachedDataProvider implements ContextDataProvider {
	
	private final ContextDataProvider provider;
	
	private final Map<UUID, List<ArchEntityInfo>> archEntityWithParents = new LRUMap<>(1000);

	public CachedDataProvider(ContextDataProvider provider) {
		this.provider = provider;
	}

	@Override
	public InstitutionInfo getInstitutionApu(String instCode) {
		return provider.getInstitutionApu(instCode);
	}

	@Override
	public List<ArchEntityInfo> getArchivalEntityWithParentsByElzaId(Integer elzaId) {
		return provider.getArchivalEntityWithParentsByElzaId(elzaId);
	}

	@Override
	public UUID getFundApu(String institutionCode, String fundCode) {
		return provider.getFundApu(institutionCode, fundCode);
	}

	@Override
	public UUID getFundApuByUUID(String institutionCode, UUID fundUuid) {
		return provider.getFundApuByUUID(institutionCode, fundUuid);
	}

	@Override
	public List<ArchEntityInfo> getArchivalEntityWithParentsByUuid(UUID apUuid) {
		var ret = archEntityWithParents.get(apUuid);
		if (ret == null) {
			ret = provider.getArchivalEntityWithParentsByUuid(apUuid);
			archEntityWithParents.put(apUuid, ret);
		}
		return ret;
	}

	@Override
	public UUID getDao(String daoHandle) {
		return provider.getDao(daoHandle);
	}

	@Override
	public List<ArchEntitySourceInfo> getArchEntityApuSources(List<UUID> uuids, String entityClass) {
		return provider.getArchEntityApuSources(uuids, entityClass);
	}

}
