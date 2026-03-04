package cz.aron.transfagent.domain.dto;

import java.util.UUID;

public class UuidNumAttachmentsNumDaos {
	
	private final UUID uuid;
	
	private final int numAttachments;
	
	private final int numDaos;
	
	public UuidNumAttachmentsNumDaos(UUID uuid, int numAttachments, int numDaos) {
		this.uuid = uuid;
		this.numAttachments = numAttachments;
		this.numDaos = numDaos;
	}

	public UUID getUuid() {
		return uuid;
	}

	public int getNumAttachments() {
		return numAttachments;
	}

	public int getNumDaos() {
		return numDaos;
	}

}
