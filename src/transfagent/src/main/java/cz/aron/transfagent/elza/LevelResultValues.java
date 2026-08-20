package cz.aron.transfagent.elza;

import java.util.List;
import java.util.UUID;

import cz.tacr.elza.schema.v2.DescriptionItemEnum;

/**
 * Hodnoty urovne popisu potrebne pro sestaveni Result
 */
public interface LevelResultValues {

	DescriptionItemEnum getLevelType();

	DescriptionItemEnum getUnitType();

	String getUnitContent();

	String getSpisZnak();

	String getSignatura();

	String getPuvodniSignatura();

	String getUnitDateText();

	String getUnitDate();

	List<UUID> getUuids();

	String getRefOzn();

	String getInvCislo();

	String getUkladaciCislo();

	String getName();

	String getParentName();

	boolean isMatrika();

	String getUkladaciJednotka();

	Integer getUkladaciJednotkaPoradi();

	String getTypZaznamu();

	String getDaoLink();

	String getDaoId();

}
