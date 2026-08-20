package cz.aron.transfagent.elza;

/**
 * Hodnoty urovne popisu potrebne pro sestaveni nazvu a popisu
 */
public interface LevelNameDescValues {

	ElzaXmlReader getElzaXmlReader();

	String getName();

	boolean isTitleInherited();

	String getUkladaciJednotka();

	boolean isUkladaciJednotkaInherited();

	String getDatace();

	boolean isDataceInherited();

	boolean isRoot();

	boolean isMatrika();

	String getTypZaznamu();

}
