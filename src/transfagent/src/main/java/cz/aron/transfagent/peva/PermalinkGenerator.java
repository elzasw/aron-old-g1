package cz.aron.transfagent.peva;

public interface PermalinkGenerator {

	/**
	 * Generate permalink for fund 
	 * @param archiveCode
	 * @param fundCode 
	 * @param subFundCode
	 * @return permalink or null
	 */
	String generateFundPermalink(String archiveCode, String fundCode, Integer subFundCode);

	/**
	 * Generate permalink for finding aid
	 * @param archiveCode
	 * @param findingAidCode
	 * @param findingAidPartNumber
	 * @return permalink or null
	 */
	String generateApPermalink(String archiveCode, String findingAidCode, String findingAidPartNumber);

}
