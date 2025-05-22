package cz.aron.customizations.soaplzen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.api.rest.model.Result;
import cz.aron.core.api.rest.model.ResultRowItem;
import cz.aron.core.api.rest.model.ResultRowItemValue;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.ImportArchDescResultBuilder;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Section;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportArchDescResultBuilderImpl implements ImportArchDescResultBuilder {

	private final ObjectMapper objectMapper;
	
	private final ArchivalEntityRepository aeRepository;

	public ImportArchDescResultBuilderImpl(ObjectMapper objectMapper, ArchivalEntityRepository aeRepository) {
		this.objectMapper = objectMapper;
		this.aeRepository = aeRepository;
	}

	@Override
	public String buildResult(InstitutionInfo instInfo, Section sect, Level lvl, LevelContext levelContext,
			UUID fundApuUuid, ElzaXmlReader elzaXmlReader) {
		var result = new Result();
		result.setId(lvl.getUuid());
		result.setT("A");
		result.setTn("https://www.inventare.cz/fcgi-bin/iipsrv.fcgi?DeepZoom=archival-aid/soap-pn/soap-pn_ap0002_10005_ku-plzen2/soap-pn_ap0002_10005_ku-plzen2_0030.jp2");

		generateName(result, levelContext, sect);
		generateDate(result, levelContext);
		generateUnitContent(result, levelContext);
		generateGeoEntities(result, levelContext, elzaXmlReader);
		generateSignature(result, levelContext);
		generateNumber(result, levelContext);
		generateInstitutionAndFund(result, instInfo, sect, fundApuUuid, levelContext);
		generateParent(result, levelContext, sect);
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateName(Result result, LevelContext levelContext, Section sect) {
		var rowItem = new ResultRowItem();
		rowItem.setT("N");
		var itemValue = new ResultRowItemValue();
		
		var title = getTitle(levelContext);
		if (title == null) {
			if (levelContext.getParentContext() == null) {
				itemValue.setV(sect.getFi().getN());
			} else {
				itemValue.setV("");
			}
		} else {
			itemValue.setV(title);	
		}		
		rowItem.addVItem(itemValue);
		result.addLItem(Arrays.asList(rowItem));
	}

	public String getTitle(LevelContext levelContext) {
		if (levelContext == null) {
			return null;
		}
		for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
			if (item.getT().equals("ZP2015_TITLE")) {
				DescriptionItemString title = (DescriptionItemString) item;
				return title.getV();
			}
		}
		return getTitle(levelContext.getParentContext());
	}

	public void generateDate(Result result, LevelContext levelContext) {
		var date = getDate(levelContext);
		if (date != null) {
			var rowItem = new ResultRowItem();
			rowItem.setT("D");
			var itemValue = new ResultRowItemValue();
			itemValue.setV(date);
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));
		}
	}

	public String getDate(LevelContext levelContext) {
		if (levelContext == null) {
			return null;
		}		
		for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
			if (item.getT().equals("ZP2015_UNIT_DATE_TEXT")) {
				DescriptionItemString dateText = (DescriptionItemString) item;
				return dateText.getV();
			}
		}				
		for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
			if (item.getT().equals(ElzaTypes.ZP2015_UNIT_DATE)) {
				DescriptionItemUnitDate unitDate = (DescriptionItemUnitDate) item;
				return UnitDateConvertor.convertToString(unitDate);
			}
		}
		return getDate(levelContext.getParentContext());
	}
	
	public void generateUnitContent(Result result, LevelContext levelContext) {
		var unitContent = getUnitContent(levelContext);
		if (unitContent!=null) {
			var rowItem = new ResultRowItem();
			rowItem.setT("CONT");
			var itemValue = new ResultRowItemValue();
			itemValue.setV(StringUtils.abbreviate(unitContent, 500));
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));
		}
	}
	
	public String getUnitContent(LevelContext levelContext) {
		if (levelContext == null) {
			return null;
		}		
		for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
			if (item.getT().equals("ZP2015_UNIT_CONTENT")) {
				DescriptionItemString dateText = (DescriptionItemString) item;
				return dateText.getV();
			}
		}
		return null;
	}
	
	public void generateSignature(Result result, LevelContext levelContext) {
		
		String spisZnak = null;
		String signatura = null;
		String puvodniSignatura = null;
		
		for(DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
			if (item.getT().equals(ElzaTypes.ZP2015_OTHER_ID)) {
				switch (item.getS()) {
				case "ZP2015_OTHERID_OLDSIG2":
					spisZnak = ((DescriptionItemString)item).getV();
					break;
				case "ZP2015_OTHERID_SIG":
					signatura = ((DescriptionItemString)item).getV();
					break;
				case "ZP2015_OTHERID_SIG_ORIG":
					puvodniSignatura = ((DescriptionItemString)item).getV();
					break;
				default:
					// ignore
				}				
			}
		}
		
		boolean somethingAdded = false;
        var rowItem = new ResultRowItem();
        rowItem.setT("SIGN");
        if (StringUtils.isNotBlank(spisZnak)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(spisZnak);
			rowItem.addVItem(itemValue);
			somethingAdded = true;
		}
        if (StringUtils.isNotBlank(signatura)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(signatura);
			rowItem.addVItem(itemValue);
			somethingAdded = true;
		}
        if (StringUtils.isNotBlank(puvodniSignatura)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(puvodniSignatura);
			rowItem.addVItem(itemValue);
			somethingAdded = true;
		}		
        if (somethingAdded) {
			result.addLItem(Arrays.asList(rowItem));
		}
	}
	
	public void generateNumber(Result result, LevelContext levelContext) {
		String refOzn = null;
        String invCislo = null;
        String poradoveCislo = null;
        String ukladaciCislo = null;
		for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
			if ("ZP2015_UNIT_ID".equals(item.getT()) && (item instanceof DescriptionItemString)) {
				DescriptionItemString title = (DescriptionItemString) item;
				refOzn = title.getV();
			} else if ("ZP2015_INV_CISLO".equals(item.getT()) && (item instanceof DescriptionItemString)) {
				DescriptionItemString title = (DescriptionItemString) item;
				invCislo = title.getV();
			} else if ("ZP2015_SERIAL_NUMBER".equals(item.getT()) && (item instanceof DescriptionItemInteger)) {
				DescriptionItemInteger serNum = (DescriptionItemInteger) item;
				poradoveCislo = serNum.getV().toString();
			} else if ("ZP2015_PACKET_NUMBER".equals(item.getT()) && (item instanceof DescriptionItemString) ) {
				DescriptionItemString uklC = (DescriptionItemString) item;
				ukladaciCislo = uklC.getV();
			}
		}
        
		var items = new ArrayList<ResultRowItem>(2);
		
        boolean somethingAdded = false;
        var rowItem = new ResultRowItem();
		rowItem.setT("NUM");		
		if (StringUtils.isNotBlank(refOzn)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(refOzn);
			rowItem.addVItem(itemValue);
			somethingAdded = true;
		}
		if (StringUtils.isNotBlank(invCislo)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(invCislo);
			rowItem.addVItem(itemValue);
			somethingAdded = true;
		}
		if (StringUtils.isNotBlank(poradoveCislo)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(poradoveCislo);
			rowItem.addVItem(itemValue);
			somethingAdded = true;
		}
		if (somethingAdded) {
			items.add(rowItem);
		}
				
		if (ukladaciCislo!=null) {
			var ucRowItem = new ResultRowItem();
			ucRowItem.setT("UC");
			var ucRowItemValue = new ResultRowItemValue();
			ucRowItem.addVItem(ucRowItemValue);
			ucRowItemValue.setV(ukladaciCislo);
			items.add(ucRowItem);
		}
		
		if (!items.isEmpty()) {
			result.addLItem(items);
		}
	}

	public void generateInstitutionAndFund(Result result, InstitutionInfo institutionInfo, Section sect,
			UUID fundApuUuid, LevelContext levelContext) {
		var instName = institutionInfo.getShortName();
		var instItem = new ResultRowItem();
		instItem.setT("IR");
		var itemValue = new ResultRowItemValue();
		itemValue.setV(instName);
		itemValue.id(institutionInfo.getUuid().toString());
		instItem.addVItem(itemValue);
		var fundItem = new ResultRowItem();
		fundItem.setT("FR");
		itemValue = new ResultRowItemValue();
		itemValue.setV(sect.getFi().getN());
		itemValue.id(fundApuUuid.toString());
		fundItem.addVItem(itemValue);
		result.addLItem(Arrays.asList(instItem, fundItem));
	}
	
	public void generateParent(Result result, LevelContext levelContext, Section sect) {
		var parentTitle = getTitle(levelContext.getParentContext());
		if (parentTitle == null && levelContext.getParentContext() != null
				&& levelContext.getParentContext().getParentContext() == null) {
			parentTitle = sect.getFi().getN();
		}
		if (parentTitle != null) {
			var contextItem = new ResultRowItem();
			contextItem.setT("CTX");
			var itemValue = new ResultRowItemValue();
			itemValue.setV(parentTitle);
			itemValue.id(levelContext.getParentContext().getLevel().getUuid().toString());
			contextItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(contextItem));
		}
	}

	private static Set<String> GEO_ROLES = new HashSet<>(Arrays.asList("ZP2015_ENTITY_ROLE_80",
			"ZP2015_ENTITY_ROLE_86",
			"ZP2015_ENTITY_ROLE_56",
			"ZP2015_ENTITY_ROLE_57",
			"ZP2015_ENTITY_ROLE_88",
			"ZP2015_ENTITY_ROLE_58",
			"ZP2015_ENTITY_ROLE_60",
			"ZP2015_ENTITY_ROLE_61",
			"ZP2015_ENTITY_ROLE_62",
			"ZP2015_ENTITY_ROLE_89",
			"ZP2015_ENTITY_ROLE_64",
			"ZP2015_ENTITY_ROLE_65",
			"ZP2015_ENTITY_ROLE_71",
			"ZP2015_ENTITY_ROLE_82"));
	
	public void generateGeoEntities(Result result, LevelContext levelContext, ElzaXmlReader elzaXmlReader) {		
				
		// get uuids of geo entities
		var uuids = new ArrayList<UUID>();
		for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
			if ((item instanceof DescriptionItemAPRef) && "ZP2015_ENTITY_ROLE".equals(item.getT()) && GEO_ROLES.contains(item.getS()) ) {
				var apRef = (DescriptionItemAPRef)item;				
				var accessPoint = elzaXmlReader.getApMap().get(apRef.getApid());
				if (accessPoint!=null) {
					uuids.add(UUID.fromString(accessPoint.getApe().getUuid()));
				}				
			}						
		}
		
		var geoItems = new ResultRowItem();
		if (!uuids.isEmpty()) {			
			// check entities are used
			var entities = aeRepository.getArchEntityApuSourcesWithScheduled(uuids);
			if (!entities.isEmpty()) {
				for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
					if ((item instanceof DescriptionItemAPRef) && "ZP2015_ENTITY_ROLE".equals(item.getT()) && GEO_ROLES.contains(item.getS()) ) {
						var apRef = (DescriptionItemAPRef)item;
						var accessPoint = elzaXmlReader.getApMap().get(apRef.getApid());
						if (accessPoint!=null) {
							var uuid = UUID.fromString(accessPoint.getApe().getUuid());
							label:for(var ent:entities) {
								if (Objects.equals(ent.getUuid(),uuid)&&ent.getStatus()==EntityStatus.AVAILABLE) {
									for(var fragment:accessPoint.getFrgs().getFrg()) {
										if (CoreTypes.PT_NAME.equals(fragment.getT())) {
											for(var itm:fragment.getDdOrDoOrDp()) {
												if (ElzaTypes.NM_MAIN.equals(itm.getT())) {
													var str = (DescriptionItemString)itm;
													str.getV();													
													var value = new ResultRowItemValue();
													value.setId(accessPoint.getApe().getUuid());
													value.setV(str.getV());
													geoItems.addVItem(value);
													break label;
												}
											}
										}
									}
								}
							}														
						}				
					}						
				}	
			}
			
			if (!geoItems.getV().isEmpty()) {
				geoItems.setT("G");
				result.addLItem(Arrays.asList(geoItems));
			}
	
		}
		
	}
	
}
