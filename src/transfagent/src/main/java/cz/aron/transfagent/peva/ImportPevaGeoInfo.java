package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.GeoObject;
import cz.aron.peva2.wsdl.GeoObjectName;
import cz.aron.peva2.wsdl.GetGeoObjectResponse;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;

public class ImportPevaGeoInfo {

	private final ApTypeService apTypeService;

	private final Peva2CodeListProvider codeLists;

	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	private String uuid;
	
	private String camUuid;

	public ImportPevaGeoInfo(ApTypeService apTypeService, Peva2CodeListProvider codeLists) {
		this.apTypeService = apTypeService;
		this.codeLists = codeLists;
	}
	
	public ApuSourceBuilder importGeo(Path inputFile, ContextDataProvider cdp)
			throws IOException, JAXBException {
		var getOriginatorResp = Peva2XmlReader.unmarshalGetGeoObjectResponse(inputFile);
		return importGeo(getOriginatorResp, cdp);
	}

	public ApuSourceBuilder importGeo(GetGeoObjectResponse getGeoObjectResp, ContextDataProvider cdp)
			throws IOException, JAXBException {
		var geoObject = getGeoObjectResp.getGeoObject();
		uuid = geoObject.getId();
		camUuid = geoObject.getCamUuid();
		var fullName = getFullName(geoObject.getPreferredName());
		var apu = apusBuilder.createApu(fullName, ApuType.ENTITY, UUID.fromString(geoObject.getId()));
		apu.setDesc(geoObject.getDescription());
		var part = ApuSourceBuilder.addPart(apu, CoreTypes.PT_AE_INFO);
		processClassSubclass(part, geoObject);
		return apusBuilder;
	}

	private void processClassSubclass(Part part, GeoObject geoObject) {		
		var tmp = codeLists.getCodeLists().getOriginatorSubClass(geoObject.getSubClass());
		if (tmp == null) {
			throw new IllegalStateException("Unknown subclass " + geoObject.getSubClass());
		}
		ApuSourceBuilder.addEnum(part, CoreTypes.AE_SUBCLASS, tmp.getName(), true);			
		var className = apTypeService.getTypeName(tmp.getCamCode());
		if (className!=null) {
			ApuSourceBuilder.addEnum(part, CoreTypes.AE_CLASS, className, true);
		}
	}

	private String getFullName(GeoObjectName geoObjectName) {
		StringBuilder sb = new StringBuilder();
		sb.append(geoObjectName.getPrimaryPart());

		var supplements = new StringJoiner(" : ", "(", ")");
		if (StringUtils.isNotBlank(geoObjectName.getGeoSupplement())) {
			supplements.add(geoObjectName.getGeoSupplement());
		}		
		if (StringUtils.isNotBlank(geoObjectName.getGeneralSupplement())) {
			supplements.add(geoObjectName.getGeneralSupplement());
		}
		if (StringUtils.isNotBlank(geoObjectName.getChronologicalSupplement())) {
			supplements.add(geoObjectName.getChronologicalSupplement());
		}
		if (supplements.length() > 2) {
			sb.append(" ").append(supplements.toString());
		}
		return sb.toString();
	}

	public String getUuid() {
		return uuid;
	}

	public String getCamUUID() {
		return camUuid;
	}

}
