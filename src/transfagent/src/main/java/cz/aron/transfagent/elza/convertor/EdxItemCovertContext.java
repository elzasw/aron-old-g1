package cz.aron.transfagent.elza.convertor;

import java.util.UUID;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.tacr.elza.schema.v2.Level;

public interface EdxItemCovertContext {

	ApuSourceBuilder getApusBuilder();

	Part getActivePart();

	ElzaXmlReader getElzaXmlReader();

	/**
	 * Add reference to another APU
	 * @param aei
	 */
	void addArchEntityRef(ArchEntityInfo aei);

	Apu getActiveApu();
	
	Level getProcessedLevel();
	
	/**
	 * Check archival entity is referenced from current level
	 * @param uuid of entity
	 * @return true - referenced, false not referenced
	 */
	boolean isArchEntityReferenced(UUID uuid);
}
