package cz.aron.transfagent.ead3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ImportFindingAidResultBuilder;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;

public class ImportFindingAidInfo {

    private Ead3XmlReader ead3XmlReader;

    private ApuSourceBuilder builder = new ApuSourceBuilder();

    private ContextDataProvider dataProvider;

    private String institutionCode;

    private String fundCode;
    
    private String findingAidName;
    
    private final ImportFindingAidResultBuilder resultBuilder;

    public ImportFindingAidInfo(String fundCode, ImportFindingAidResultBuilder resultBuilder) {
        this.fundCode = fundCode;
        this.resultBuilder = resultBuilder;
    }

    public ApuSourceBuilder importFindingAidInfo(Path inputFile, String propFile) throws IOException, JAXBException {
        Validate.isTrue(propFile != null && propFile.length() > 0);

        PropertiesDataProvider pdp = new PropertiesDataProvider();
        Path propPath = Paths.get(propFile);
        pdp.load(propPath);

        return importFindingAidInfo(inputFile, null, pdp);
    }

    public ApuSourceBuilder importFindingAidInfo(final Path inputFile, UUID uuid, final ContextDataProvider cdp) throws IOException, JAXBException {
        this.dataProvider = cdp;

        try (InputStream is = Files.newInputStream(inputFile)) {
            ead3XmlReader = Ead3XmlReader.read(is);
            return importFindingAidInfo(uuid);
        }
    }

    public ApuSourceBuilder importFindingAidInfo(UUID uuid) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        findingAidName = ead3XmlReader.getSubtitle();
        Apu apu = builder.createApu(findingAidName, ApuType.FINDING_AID, uuid);

        // add title part
        Part partTitle = builder.addPart(apu, CoreTypes.PT_TITLE);
        partTitle.setValue(findingAidName);
        builder.addString(partTitle, CoreTypes.TITLE, findingAidName);

        institutionCode = ead3XmlReader.getInstitutionCode();
        var instInfo = dataProvider.getInstitutionApu(institutionCode);
        Validate.notNull(instInfo, "Missing institution, code: %s", institutionCode);

        var fundApuUuid = dataProvider.getFundApu(institutionCode, fundCode);
        Validate.notNull(fundApuUuid, "Missing fund, code: %s, institution: %s", fundCode, institutionCode);

        // add info part
        Part partInfo = builder.addPart(apu, CoreTypes.PT_FINDINGAID_INFO);
        builder.addString(partInfo, CoreTypes.FINDINGAID_ID, ead3XmlReader.getRecordId());
        var releaseDatePlace = ead3XmlReader.getReleaseDatePlace();
        if(StringUtils.isNotEmpty(releaseDatePlace)) {
            builder.addString(partInfo, CoreTypes.FINDINGAID_RELEASE_DATE_PLACE, releaseDatePlace);
        }
        var findingAidType = ead3XmlReader.getLocalionControlByType("FINDING_AID_TYPE");
        if(StringUtils.isNotEmpty(findingAidType)) {
            builder.addEnum(partInfo, CoreTypes.FINDINGAID_TYPE, findingAidType);
        }    
        var dateRange = ead3XmlReader.getLocalionControlByType("DATE_RANGE");
        if(StringUtils.isNotEmpty(dateRange)) {
            builder.addString(partInfo, CoreTypes.FINDINGAID_DATE_RANGE, dateRange);
        }
        var unitsAmount = ead3XmlReader.getLocalionControlByType("UNITS_AMOUNT");
        if(StringUtils.isNotEmpty(unitsAmount)) {
            builder.addString(partInfo, CoreTypes.FINDINGAID_UNITS_AMOUNT, unitsAmount);
        }

        // add references part
        Part partRef = builder.addPart(apu, CoreTypes.PT_ARCH_DESC_FUND);
        builder.addApuRef(partRef, "FUND_REF", fundApuUuid);
        builder.addApuRef(partRef, "FUND_INST_REF", instInfo.getUuid());

        if (resultBuilder!=null) {
        	var result = resultBuilder.buildResult(ead3XmlReader, instInfo, apu.getUuid());
			if (result!=null) {
				apu.setResult(result);
			}        	
        }
        
        return builder;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }
    
    public String getFindingAidName() {
    	return findingAidName;
    }
}
