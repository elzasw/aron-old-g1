package cz.aron.transfagent.transformation;

import java.util.UUID;

public class ArchEntityInfo {

    private final UUID uuid;

    private final String entityClass;
    
    // pokud mam puvodce z pevy pri importu archdesc se sem doplni entityClass z archivniho popisu
    private final String srcEntityClass;

    public ArchEntityInfo(UUID uuid, String entityClass) {
        this.uuid = uuid;
        this.entityClass = entityClass;
        this.srcEntityClass = null;
    }
    
    public ArchEntityInfo(UUID uuid, String entityClass, String srcEntityClass) {
		this.uuid = uuid;
		this.entityClass = entityClass;
		this.srcEntityClass = srcEntityClass;
	}

    public UUID getUuid() {
        return uuid;
    }

    public String getEntityClass() {
        return entityClass;
    }
    
	public String getSrcEntityClass() {
		return srcEntityClass;
	}

}
