package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.FindingAid;
import cz.aron.peva2.wsdl.UniversalTimeRange;
import cz.aron.transfagent.config.ConfigPeva2FindingAidProperties;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.InstitutionInfo;

public class ImportPevaFindingAidInfo {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaFindingAidInfo.class);

	private ContextDataProvider dataProvider;

	private final ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	private final Peva2CodeLists codeLists;
	
	private final ConfigPeva2FindingAidProperties findingAidProperties;

	private String institutionCode;
	
	private String findingAidCode;

	private List<UUID> fundUUIDs = new ArrayList<>();
	
	private List<String> authorUUIDs = new ArrayList<>();
	
	private UUID findingAidUUID;
	
	public ImportPevaFindingAidInfo(Peva2CodeLists codeLists, ConfigPeva2FindingAidProperties findingAidProperties) {
		this.codeLists = codeLists;
		this.findingAidProperties = findingAidProperties;
	}

	public ApuSourceBuilder importFindingAidInfo(final Path inputFile, final ContextDataProvider cdp)
			throws IOException, JAXBException {
		dataProvider = cdp;
		var gfar = Peva2XmlReader.unmarshalGetFindingAidResponse(inputFile);
		var findingAid = gfar.getFindingAid();
		importFindingAidInfo(findingAid);
		return apusBuilder;
	}

	private void importFindingAidInfo(FindingAid findingAid) {
		
		var instInfo = getInstitutionInfo(findingAid);
		findingAidUUID = UUID.fromString(findingAid.getId());

		var findingAidName = findingAid.getName();
		var fullFindingAidName = createFindingAidName(instInfo, findingAid);
		
		Apu apu = apusBuilder.createApu(fullFindingAidName, ApuType.FINDING_AID, UUID.fromString(findingAid.getId()));

		var partTitle = ApuSourceBuilder.addPart(apu, CoreTypes.PT_TITLE);
		partTitle.setValue(findingAidName);
		ApuSourceBuilder.addString(partTitle, CoreTypes.TITLE, findingAidName);
		
		for(var fundCode:findingAid.getNadSheets().getNadSheet()) {
			var fundUUID = dataProvider.getFundApuByUUID(institutionCode, UUID.fromString(fundCode));
			if (fundUUID!=null) {
				fundUUIDs.add(fundUUID);	
			} else {
				log.error("FindingAid uuid={}, missing fund {}",findingAid.getId(),fundCode);
			}						
		}
		
		if (fundUUIDs.isEmpty()) {
			log.error("Fainding aid code: {}, institution:{} not related to any fund", findingAid.getEvidenceNumber(),
					institutionCode);
			throw new PevaFundNotExist(findingAid.getInstitution().getId(), findingAid.getNadSheets().getNadSheet().stream().collect(Collectors.toList()));
		}
		
		Validate.isTrue(!fundUUIDs.isEmpty(), "Fainding aid code: %s, institution:%s not related to any fund", findingAid.getEvidenceNumber(),
				institutionCode);

		// add info part
		Part partInfo = ApuSourceBuilder.addPart(apu, CoreTypes.PT_FINDINGAID_INFO);
		ApuSourceBuilder.addString(partInfo, CoreTypes.FINDINGAID_ID, findingAid.getEvidenceNumber());
		findingAidCode = findingAid.getEvidenceNumber();
		
		/**
		var releaseDatePlace = ead3XmlReader.getReleaseDatePlace();
		if (StringUtils.isNotEmpty(releaseDatePlace)) {
			apusBuilder.addString(partInfo, CoreTypes.FINDINGAID_RELEASE_DATE_PLACE, releaseDatePlace);
		}*/
						
		var findingAidType = codeLists.getFindingAidType(findingAid.getType());
		if (StringUtils.isNotEmpty(findingAidType)) {
			ApuSourceBuilder.addEnum(partInfo, CoreTypes.FINDINGAID_TYPE, findingAidType);
		}
		var formTypes = getFormTypes(findingAid);
		if (StringUtils.isNotBlank(formTypes)) {
			ApuSourceBuilder.addEnum(partInfo, "FINDINGAID_FORM_TYPE", formTypes);
		}
		var signature = findingAid.getSignature();
		if (StringUtils.isNotBlank(signature)) {
			ApuSourceBuilder.addString(partInfo, "FINDINGAID_SIG", signature);
		}

		processAddenums(findingAid, partInfo);
		processPlacesOfOrigin(findingAid, partInfo);
		processAuthors(findingAid, partInfo);
				
		var dateRange = findingAid.getTimeRange();
		if (dateRange != null) {
			fillFindingAidDateRange(dateRange, partInfo);
		}
		var yearOfOrigin = findingAid.getYearOfOrigin();
		if (yearOfOrigin!=null) {
			ApuSourceBuilder.addString(partInfo, "FINDINGAID_YEAR_OF_ORIGIN", yearOfOrigin.toString());
		}
		/**
		var unitsAmount = ead3XmlReader.getLocalionControlByType("UNITS_AMOUNT");
		if (StringUtils.isNotEmpty(unitsAmount)) {
			apusBuilder.addString(partInfo, CoreTypes.FINDINGAID_UNITS_AMOUNT, unitsAmount);
		}*/

		if (findingAidProperties.isReferencesPart()) {
			// add references part		
			var partRef = ApuSourceBuilder.addPart(apu, CoreTypes.PT_ARCH_DESC_FUND);
			processRefs(partRef, fundUUIDs, instInfo.getUuid());
		} else {
			processRefs(partInfo, fundUUIDs, instInfo.getUuid());
		}
	}
	
	private void processRefs(Part part, List<UUID> fundUUIDs, UUID instUUID) {
		for(var fundUUID:fundUUIDs) {
			apusBuilder.addApuRef(part, "FUND_REF", fundUUID);
		}
		apusBuilder.addApuRef(part, "FUND_INST_REF", instUUID);
	}
	
	private void processAddenums(FindingAid findingAid, Part partInfo) {
		StringJoiner sj = new StringJoiner("\r\n");
		for (var addenum : findingAid.getAddendums().getAddendum()) {
			StringJoiner sjInt = new StringJoiner(", ");
			if (StringUtils.isNotBlank(addenum.getNote())) {
				sjInt.add(addenum.getNote());
			}
			if (addenum.getCountOfPages() != null) {
				sjInt.add("Počet stran: " + addenum.getCountOfPages());
			}
			if (addenum.getYearOfOrigin() != null) {
				sjInt.add("Rok vzniku: " + addenum.getYearOfOrigin());
			}
		}
		if (sj.length() > 0) {
			ApuSourceBuilder.addString(partInfo, "FINDINGAID_ADDENUM", sj.toString());
		}
	}
	
	private void processPlacesOfOrigin(FindingAid findingAid, Part partInfo) {
		//var places = new TreeSet<String>();
		var placesOfOrigin = findingAid.getPlacesOfOrigin();
		for(var placeOfOrigin:placesOfOrigin.getPlaceOfOrigin()) {
			var placeId = placeOfOrigin.getPlace();
			//var geo = entityDownloader.getGeoObject(placeId);
			//var name = geo.getPreferredName().getPrimaryPart();
			//places.add(name);								
			apusBuilder.addApuRef(partInfo, "ORIG_PLACES_REF", UUID.fromString(placeId));			
		}
	}
	
	private void processAuthors(FindingAid findingAid, Part partInfo) {		
		if (findingAidProperties.isAuhorRef()) {
			for(var authorRecord:findingAid.getAuthorRecords().getAuthorRecord()) {
				var author = authorRecord.getAuthor();			
				apusBuilder.addApuRef(partInfo, "FINDINGAID_AUTHOR_REF", UUID.fromString(author));
			}	
		} else {
			for(var authorRecord:findingAid.getAuthorRecords().getAuthorRecord()) {							
				authorUUIDs.add(authorRecord.getAuthor());
			}
		}
	}

	private void fillFindingAidDateRange(UniversalTimeRange timeRange, Part partInfo) {									
		Peva2Utils.fillDateRange(timeRange, partInfo, apusBuilder);
	}
	
	private String createFindingAidName(InstitutionInfo institutionInfo, FindingAid findingAid) {
		if (findingAidProperties.isComposedFindingAidName()) {
			var findingAidType = codeLists.getFindingAidType(findingAid.getType());
			if (StringUtils.isEmpty(findingAidType)) {
				findingAidType = "";
			}
			var formTypes = getFormTypes(findingAid); 			
			return institutionInfo.getName() + ": " + findingAid.getName() + ". " + findingAidType + ", "
					+ formTypes + ", " + findingAid.getYearOfOrigin();
		} else if  (findingAidProperties.isComposedShortFindingAidName() ) {
			var findingAidType = codeLists.getFindingAidType(findingAid.getType());
			if (StringUtils.isEmpty(findingAidType)) {
				findingAidType = "";
			}
			var formTypes = getFormTypes(findingAid); 			
			return institutionInfo.getShortName() + ": " + findingAid.getName() + ". " + findingAidType + ", "
					+ formTypes + ", " + findingAid.getYearOfOrigin();
		} else {
			return findingAid.getName();
		}
	}
	
	private String getFormTypes(FindingAid findingAid) {
		StringJoiner sj = new StringJoiner(", ");
		for (var form : findingAid.getForms().getForm()) {
			var type = codeLists.getFindingAidFormType(form.getFormType());
			if (type != null) {
				sj.add(type);
			}
		}
		return sj.toString();
	}
	
	private InstitutionInfo getInstitutionInfo(FindingAid findingAid) {
		var instInfo = dataProvider.getInstitutionApu(findingAid.getInstitution().getExternalId());
		if (instInfo == null) {
			throw new RuntimeException("Missing institution: " + institutionCode);
		}
		institutionCode = findingAid.getInstitution().getExternalId();
		return instInfo;
	}

	public String getInstitutionCode() {
		return institutionCode;
	}
	
	public String getFindingAidCode() {
	    return findingAidCode;
	}
	
	public List<UUID> getFundUUIDs() {
		return fundUUIDs;
	}

	public UUID getFindingAidUUID() {
		return findingAidUUID;
	}
	
	public List<String> getAuthorUUIDs() {
		return authorUUIDs;
	}

	public static class PevaFundNotExist extends RuntimeException {
	    
	    private final String institutionUUID;
		
		private final List<String> fundIds;
		
		public PevaFundNotExist(String institutionUUID, List<String> fundIds) {
		    this.institutionUUID = institutionUUID;
			this.fundIds = fundIds;
		}				

		public String getInstitutionUUID() {
            return institutionUUID;
        }

        public List<String> getFundIds() {
			return fundIds;
		}

	}
	
	public static class FindingAidRemoved extends RuntimeException {
		
	}
	
}
