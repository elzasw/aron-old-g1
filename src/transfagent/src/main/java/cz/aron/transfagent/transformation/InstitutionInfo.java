package cz.aron.transfagent.transformation;

import java.util.UUID;

public class InstitutionInfo {

    private final UUID uuid;

    private final String name;
    
    private final String shortName;

    public InstitutionInfo(UUID uuid, String name, String shortName) {
        this.uuid = uuid;
        this.name = name;
        this.shortName = shortName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

	public String getShortName() {
		return shortName;
	}    

}
