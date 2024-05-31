package cz.aron.transfagent.transformation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class PropertiesDataProvider implements ContextDataProvider {

	Properties props = new Properties();

	public void load(Path propPath) throws IOException {
		try(InputStream is = Files.newInputStream(propPath);) {
			props.load(is);
		}
	}

	@Override
	public InstitutionInfo getInstitutionApu(String instCode) {
		String propName = "institution." + instCode;
		return new InstitutionInfo(UUID.fromString(getProperty(propName)), getProperty(propName), getProperty(propName));
	}

	private String getProperty(String propName) {
		String apuUuid = props.getProperty(propName);
		if(apuUuid==null) {
			throw new RuntimeException("Mssing property: " + propName);
		}
		return apuUuid;
	}

	@Override
	public List<ArchEntityInfo> getArchivalEntityWithParentsByElzaId(Integer elzaId) {
		String propName = "entity." + elzaId;
		UUID uuid = UUID.fromString(getProperty(propName));
		return List.of(new ArchEntityInfo(uuid, propName + ".entityClass"));
	}

    @Override
    public List<ArchEntityInfo> getArchivalEntityWithParentsByUuid(UUID apUuid) {
        String propName = "parent.GEO";
        UUID uuid = UUID.fromString(getProperty(propName));
        return List.of(new ArchEntityInfo(uuid, "GEO_UNIT"));
    }

    @Override
    public UUID getFundApu(String institutionCode, String fundCode) {
        String propName = String.format("fund.%s.%s", institutionCode, fundCode);
        return UUID.fromString(getProperty(propName));
    }

    @Override
    public UUID getDao(String daoHandle) {
        return null;
    }

	@Override
	public UUID getFundApuByUUID(String institutionCode, UUID fundUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ArchEntitySourceInfo> getArchEntityApuSources(List<UUID> uuids, String entityClass) {
		return Collections.emptyList();
	}
}
