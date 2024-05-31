package cz.aron.transfagent.transformation;

import java.util.UUID;

import cz.aron.transfagent.domain.EntityStatus;

public class ArchEntitySourceInfo {
	
    private final UUID uuid;

    private final String dataDir;
    
    private final UUID apuSourceUUID;
    
    private final EntityStatus status;
    
    private final Long lastSent;
    
    private final Long lastScheduled;
    
	public ArchEntitySourceInfo(UUID uuid, String dataDir, UUID apuSourceUUID, EntityStatus status, Long lastSent) {
		this.uuid = uuid;
		this.dataDir = dataDir;
		this.apuSourceUUID = apuSourceUUID;
		this.status = status;
		this.lastSent = lastSent;
		this.lastScheduled = null;
	}
	
	public ArchEntitySourceInfo(UUID uuid, String dataDir, UUID apuSourceUUID, EntityStatus status, Long lastSent, Long lastScheduled) {
		this.uuid = uuid;
		this.dataDir = dataDir;
		this.apuSourceUUID = apuSourceUUID;
		this.status = status;
		this.lastSent = lastSent;
		this.lastScheduled = lastScheduled;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getDataDir() {
		return dataDir;
	}

	public UUID getApuSourceUUID() {
		return apuSourceUUID;
	}

	public EntityStatus getStatus() {
		return status;
	}

	public Long getLastSent() {
		return lastSent;
	}

	public Long getLastScheduled() {
		return lastScheduled;
	}

}
