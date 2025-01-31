package cz.aron.transfagent.transformation;

import java.util.UUID;

public class FindingAidInfo {
	
	private final UUID uuid;

    private final String name;

	public FindingAidInfo(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}    
	
}
