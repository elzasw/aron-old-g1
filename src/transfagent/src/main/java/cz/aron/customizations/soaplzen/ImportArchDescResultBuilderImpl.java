package cz.aron.customizations.soaplzen;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
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
import cz.aron.transfagent.elza.convertor.EdxLinkConvertor;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.StructuredObject;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportArchDescResultBuilderImpl implements ImportArchDescResultBuilder {

	private static final int MAX_NAME_LENGTH = 200;

	private static final int MAX_CONTENT_LENGTH = 300;

	private static final String ARCHIVALIE_LEVEL_GENERIC = "A";
	private static final String ARCHIVALIE_LEVEL_ROOT = "A_R";
	private static final String ARCHIVALIE_LEVEL_SERIE = "A_S";
	private static final String ARCHIVALIE_LEVEL_SLOZKA = "A_F";
	private static final String ARCHIVALIE_LEVEL_JEDNOTLIVOST = "A_I";
	private static final String ARCHIVALIE_LEVEL_JEDNOTLIVOST_MATRIKA = "A_IM";
	private static final String ARCHIVALIE_LEVEL_JEDNOTLIVOST_KNIHA = "A_IB";
	private static final String ARCHIVALIE_LEVEL_JEDNOTLIVOST_VYKRES_MAPA = "A_D";
	private static final String ARCHIVALIE_LEVEL_JEDNOTLIVOST_CAST = "A_IP";
	
	private static Collator CZ_COLLATOR = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));

	private final ObjectMapper objectMapper;

	private final ArchivalEntityRepository aeRepository;
	
	private final ThumbnailFileStoreI thumbnailFileStore;

	public ImportArchDescResultBuilderImpl(ObjectMapper objectMapper, ArchivalEntityRepository aeRepository, ThumbnailFileStoreI thumbnailFileStore) {
		this.objectMapper = objectMapper;
		this.aeRepository = aeRepository;
		this.thumbnailFileStore = thumbnailFileStore;
	}

	@Override
	public String buildResult(InstitutionInfo instInfo, Section sect, Level lvl, LevelContext levelContext,
			UUID fundApuUuid, ElzaXmlReader elzaXmlReader) {

		var valueCollector = new ValueCollector(elzaXmlReader);
		valueCollector.collect(levelContext);

		var result = new Result();
		result.setId(lvl.getUuid());
		result.setT(getType(valueCollector));
		
		generateThumbnail(valueCollector, result);				
		
		if (valueCollector.isMatrika()) {
			generateMatrika(result, valueCollector, levelContext, elzaXmlReader, instInfo, sect, fundApuUuid);
		} else {
			generateName(result, levelContext, sect, valueCollector);
			generateDate(result, valueCollector);
			generateUnitContent(result, valueCollector);
			generateGeoEntities(result, levelContext, elzaXmlReader, valueCollector);
			generateSignature(result, valueCollector);
			generateNumber(result, valueCollector);
			generateInstitutionAndFund(result, instInfo, sect, fundApuUuid);
			generateParent(result, levelContext, sect, valueCollector);		
		}
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void generateThumbnail(ValueCollector valueCollector, Result result) {		
		var daoId = valueCollector.getDaoId();
		if (daoId != null) {
			if (thumbnailFileStore.fillDaoIdLink(daoId, result)) {
				return;
			}		
		}

		var daoLink = valueCollector.getDaoLink();
		if (daoLink != null) {
			thumbnailFileStore.fillThumbnailLink(daoLink, result);
		}
	}

	private void generateMatrika(Result result, ValueCollector valueCollector, LevelContext levelContext, ElzaXmlReader elzaXmlReader,
			InstitutionInfo instInfo, Section sect, UUID fundApuUuid) {
		generateNameMatrika(result, levelContext, sect, valueCollector);
		generateDateMatrika(result, valueCollector);
		generateUnitContent(result, valueCollector);
		generateGeoEntities(result, levelContext, elzaXmlReader, valueCollector);
		generateSignature(result, valueCollector);
		generateInstitutionAndFund(result, instInfo, sect, fundApuUuid);
	}
	
	public void generateName(Result result, LevelContext levelContext, Section sect, ValueCollector valueCollector) {
		var rowItem = new ResultRowItem();
		rowItem.setT("N");
		var itemValue = new ResultRowItemValue();

		var title = valueCollector.getTitle();
		if (title == null) {
			if (levelContext.getParentContext() == null) {
				itemValue.setV(StringUtils.abbreviate(sect.getFi().getN(), MAX_NAME_LENGTH));
			} else {
				itemValue.setV("");
			}
		} else {
			itemValue.setV(StringUtils.abbreviate(title, MAX_NAME_LENGTH));
		}
		rowItem.addVItem(itemValue);
		result.addLItem(Arrays.asList(rowItem));
	}
	
	public void generateNameMatrika(Result result, LevelContext levelContext, Section sect,
			ValueCollector valueCollector) {
		var rowItem = new ResultRowItem();
		rowItem.setT("N");
		var itemValue = new ResultRowItemValue();

		var title = valueCollector.getUkladaciJednotka();
		if (title != null) {
			itemValue.setV(StringUtils.abbreviate(title, MAX_NAME_LENGTH));
		} else {
			// pokud neni ukladaci jednotka pouzije se nazev
			title = valueCollector.getTitle();
			if (title != null) {
				itemValue.setV(StringUtils.abbreviate(title, MAX_NAME_LENGTH));
			}
		}
		rowItem.addVItem(itemValue);
		result.addLItem(Arrays.asList(rowItem));
	}

	public void generateDate(Result result, ValueCollector valueCollector) {

		String date = null;
		if (valueCollector.getUnitDateText() != null) {
			date = valueCollector.getUnitDateText();
		} else if (valueCollector.getUnitDate() != null) {
			date = valueCollector.getUnitDate();
		}

		if (date != null) {
			var rowItem = new ResultRowItem();
			rowItem.setT("D");
			var itemValue = new ResultRowItemValue();
			itemValue.setV(date);
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));
		}
	}

	public void generateDateMatrika(Result result, ValueCollector valueCollector) {
		String date = null;
		if (valueCollector.getUnitDateText() != null) {
			date = valueCollector.getUnitDateText();			
		} else if (valueCollector.getUnitDate() != null) {
			date = valueCollector.getUnitDate();
		}				
		if (date !=null && valueCollector.getTypZaznamu() != null) {
			date = date + " " + valueCollector.getTypZaznamu();
		}				

		if (date != null) {
			var rowItem = new ResultRowItem();
			rowItem.setT("D");
			var itemValue = new ResultRowItemValue();
			itemValue.setV(date);
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));
		}
	}
	
	public void generateUnitContent(Result result, ValueCollector valueCollector) {
		var unitContent = valueCollector.getUnitContent();
		if (unitContent != null) {
			var rowItem = new ResultRowItem();
			rowItem.setT("CONT");
			var itemValue = new ResultRowItemValue();
			itemValue.setV(StringUtils.abbreviate(unitContent, MAX_CONTENT_LENGTH));
			rowItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(rowItem));
		}
	}

	public void generateSignature(Result result, ValueCollector valueCollector) {
		String spisZnak = valueCollector.getSpisZnak();
		String signatura = valueCollector.getSignatura();
		String puvodniSignatura = valueCollector.getPuvodniSignatura();

		boolean somethingAdded = false;
		var rowItem = new ResultRowItem();
		rowItem.setT("J_S");
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

	public void generateNumber(Result result, ValueCollector valueCollector) {
		String refOzn = valueCollector.getRefOzn();
		String invCislo = valueCollector.getInvCislo();
		String ukladaciCislo = valueCollector.getUkladaciCislo();
		String ukladaciJednotka = valueCollector.getUkladaciJednotka();

		var items = new ArrayList<ResultRowItem>(2);

		boolean somethingAdded = false;
		var rowItem = new ResultRowItem();
		if (StringUtils.isNotBlank(refOzn)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(refOzn);
			rowItem.addVItem(itemValue);
			rowItem.setT("J_RO");
			somethingAdded = true;
		} else if (StringUtils.isNotBlank(invCislo)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(invCislo);
			rowItem.addVItem(itemValue);
			rowItem.setT("J_IC");
			somethingAdded = true;
		}
		if (somethingAdded) {
			items.add(rowItem);
		}

		if (ukladaciCislo != null) {
			var ucRowItem = new ResultRowItem();
			ucRowItem.setT("J_UC");
			var ucRowItemValue = new ResultRowItemValue();
			ucRowItem.addVItem(ucRowItemValue);
			ucRowItemValue.setV(ukladaciCislo);
			items.add(ucRowItem);
		} else if (ukladaciJednotka != null) {
			var ucRowItem = new ResultRowItem();
			ucRowItem.setT("J_UC");
			var ucRowItemValue = new ResultRowItemValue();
			ucRowItem.addVItem(ucRowItemValue);
			var ukladaciJednotkaPoradi = valueCollector.getUkladaciJednotkaPoradi();
			if (ukladaciJednotkaPoradi != null) {
				ucRowItemValue.setV(ukladaciJednotka+"/"+ukladaciJednotkaPoradi);
			} else {
				ucRowItemValue.setV(ukladaciJednotka);	
			}			
			items.add(ucRowItem);
		}

		if (!items.isEmpty()) {
			result.addLItem(items);
		}
	}

	public void generateInstitutionAndFund(Result result, InstitutionInfo institutionInfo, Section sect,
			UUID fundApuUuid) {
		var instName = institutionInfo.getShortName();
		var instItem = new ResultRowItem();
		instItem.setT("J_I");
		var itemValue = new ResultRowItemValue();
		itemValue.setV(instName);
		itemValue.id(institutionInfo.getUuid().toString());
		instItem.addVItem(itemValue);
		var fundItem = new ResultRowItem();
		fundItem.setT("J_F");
		itemValue = new ResultRowItemValue();
		itemValue.setV(sect.getFi().getN());
		itemValue.id(fundApuUuid.toString());
		fundItem.addVItem(itemValue);
		result.addLItem(Arrays.asList(instItem, fundItem));
	}

	public void generateParent(Result result, LevelContext levelContext, Section sect, ValueCollector valueCollector) {
		var parentTitle = valueCollector.getParentTitle();
		if (parentTitle == null && levelContext.getParentContext() != null
				&& levelContext.getParentContext().getParentContext() == null) {
			parentTitle = sect.getFi().getN();
		}
		if (parentTitle != null) {
			var contextItem = new ResultRowItem();
			contextItem.setT("J_C");
			var itemValue = new ResultRowItemValue();
			itemValue.setV(parentTitle);
			itemValue.id(levelContext.getParentContext().getLevel().getUuid().toString());
			contextItem.addVItem(itemValue);
			result.addLItem(Arrays.asList(contextItem));
		}
	}

	private static Set<String> GEO_ROLES = new HashSet<>(Arrays.asList("ZP2015_ENTITY_ROLE_80", "ZP2015_ENTITY_ROLE_86",
			"ZP2015_ENTITY_ROLE_56", "ZP2015_ENTITY_ROLE_57", "ZP2015_ENTITY_ROLE_88", "ZP2015_ENTITY_ROLE_58",
			"ZP2015_ENTITY_ROLE_60", "ZP2015_ENTITY_ROLE_61", "ZP2015_ENTITY_ROLE_62", "ZP2015_ENTITY_ROLE_89",
			"ZP2015_ENTITY_ROLE_64", "ZP2015_ENTITY_ROLE_65", "ZP2015_ENTITY_ROLE_71", "ZP2015_ENTITY_ROLE_82"));

	public void generateGeoEntities(Result result, LevelContext levelContext, ElzaXmlReader elzaXmlReader,
			ValueCollector valueCollector) {

		// get uuids of geo entities
		var uuids = valueCollector.getUuids();

		// fill geo entities to LevelContext
		if (!uuids.isEmpty()) {
			// check entities are used
			var entities = aeRepository.getArchEntityApuSourcesWithScheduled(uuids);
			if (!entities.isEmpty()) {
				for (DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
					if ((item instanceof DescriptionItemAPRef) && "ZP2015_ENTITY_ROLE".equals(item.getT())
							&& GEO_ROLES.contains(item.getS())) {
						var apRef = (DescriptionItemAPRef) item;
						var accessPoint = elzaXmlReader.getApMap().get(apRef.getApid());
						if (accessPoint != null) {
							var uuid = UUID.fromString(accessPoint.getApe().getUuid());
							label: for (var ent : entities) {
								if (Objects.equals(ent.getUuid(), uuid) && ent.getStatus() == EntityStatus.AVAILABLE) {
									for (var fragment : accessPoint.getFrgs().getFrg()) {
										if (CoreTypes.PT_NAME.equals(fragment.getT())) {
											for (var itm : fragment.getDdOrDoOrDp()) {
												if (ElzaTypes.NM_MAIN.equals(itm.getT())) {
													var str = (DescriptionItemString) itm;
													levelContext.addGeoEntity(str.getV(), accessPoint.getApe().getUuid());
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
		}		
		
		Map<String,String> geo = new TreeMap<>(CZ_COLLATOR);
		levelContext.inheritGeoEntity(geo);
		if (!geo.isEmpty()) {
			var geoItems = new ResultRowItem();
			geo.forEach((name,uuid)->{
				var value = new ResultRowItemValue();
				value.setId(uuid);
				value.setV(name);
				geoItems.addVItem(value);
			});			
			
			if (!geoItems.getV().isEmpty()) {
				geoItems.setT("J_G");
				result.addLItem(Arrays.asList(geoItems));
			}
		}

	}

	private String getType(ValueCollector valueCollector) {
		var levelType = valueCollector.getLevelType();
		if (levelType == null) {
			return ARCHIVALIE_LEVEL_GENERIC;
		}
		
		if (valueCollector.isMatrika() && ElzaTypes.ZP2015_LEVEL_ITEM.equals(levelType.getS())) {
			// ikona matriky pouze pro jednotlivost
			return ARCHIVALIE_LEVEL_JEDNOTLIVOST_MATRIKA;
		} else if (ElzaTypes.ZP2015_LEVEL_PART.equals(levelType.getS())) {
			return ARCHIVALIE_LEVEL_JEDNOTLIVOST_CAST;
		} else if (ElzaTypes.ZP2015_LEVEL_ITEM.equals(levelType.getS())) {
			var unitType = valueCollector.getUnitType();
			if (unitType != null) {
				if ("ZP2015_UNIT_TYPE_TVY".equals(unitType.getS())) {
					return ARCHIVALIE_LEVEL_JEDNOTLIVOST_VYKRES_MAPA;
				} else if ("ZP2015_UNIT_TYPE_MAP".equals(unitType.getS())) {
					return ARCHIVALIE_LEVEL_JEDNOTLIVOST_VYKRES_MAPA;
				} else if ("ZP2015_UNIT_TYPE_UKN".equals(unitType.getS())) {
					return ARCHIVALIE_LEVEL_JEDNOTLIVOST_KNIHA;
				}
			}
			return ARCHIVALIE_LEVEL_JEDNOTLIVOST;
		} else if (ElzaTypes.ZP2015_LEVEL_SERIES.equals(levelType.getS())) {
			return ARCHIVALIE_LEVEL_SERIE;
		} else if (ElzaTypes.ZP2015_LEVEL_FOLDER.equals(levelType.getS())) {
			return ARCHIVALIE_LEVEL_SLOZKA;
		} else if (ElzaTypes.ZP2015_LEVEL_ROOT.equals(levelType.getS())) {
			return ARCHIVALIE_LEVEL_ROOT;
		}
		return ARCHIVALIE_LEVEL_GENERIC;
	}

	/**
	 * Class for prepare of all level values
	 */
	private class ValueCollector {

		private final ElzaXmlReader elzaXmlReader;

		DescriptionItemEnum levelType = null;

		DescriptionItemEnum unitType = null;

		String unitContent = null;

		String spisZnak = null;
		String signatura = null;
		String puvodniSignatura = null;

		String unitDateText = null;
		String unitDate = null;

		List<UUID> uuids = new ArrayList<>();

		String refOzn = null;
		String invCislo = null;
		String ukladaciCislo = null;

		String title = null;
		String parentTitle = null;
		
		boolean matrika = false;
		
		String ukladaciJednotka = null;
		Integer ukladaciJednotkaPoradi = null;
		
		String typZaznamu = null;
		boolean typZaznamuSet = false;
		
		String daoLink = null;
		String daoId = null;
		
		boolean oddani = false;
		boolean oddaniIndex = false;
		boolean narozeni = false;
		boolean narozeniIndex = false;
		boolean zemreli = false;
		boolean zemreliIndex = false;

		public ValueCollector(ElzaXmlReader elzaXmlReader) {
			this.elzaXmlReader = elzaXmlReader;
		}

		public void collect(LevelContext levelContext) {
			var context = levelContext;
			collectNonInheritedAttributes(context);			
			while((context=context.getParentContext())!=null) {
				collectInheritedAttributes(context);
			}
			createTypZaznamu();			
		}
		
		private void collectNonInheritedAttributes(LevelContext levelContext) {
						
			for (var pp : levelContext.getLevel().getDdOrDoOrDp()) {

				if (ElzaTypes.ZP2015_LEVEL_TYPE.equals(pp.getT())) {
					levelType = (DescriptionItemEnum) pp;
				} else if (ElzaTypes.ZP2015_UNIT_TYPE.equals(pp.getT())) {
					unitType = (DescriptionItemEnum) pp;
				} else if ("ZP2015_UNIT_CONTENT".equals(pp.getT())) {
					unitContent = ((DescriptionItemString) pp).getV();
				} else if (ElzaTypes.ZP2015_TITLE.equals(pp.getT())) {
					title = ((DescriptionItemString) pp).getV();
				} else if ("ZP2015_UNIT_DATE_TEXT".equals(pp.getT())) {
					unitDateText = ((DescriptionItemString) pp).getV();
				} else if (ElzaTypes.ZP2015_UNIT_DATE.equals(pp.getT())) {
					if (pp instanceof DescriptionItemUnitDate) {
						unitDate = UnitDateConvertor.convertToString2((DescriptionItemUnitDate) pp);
					}
				} else if (ElzaTypes.ZP2015_OTHER_ID.equals(pp.getT())) {
					switch (pp.getS()) {
					case "ZP2015_OTHERID_OLDSIG2":
						spisZnak = ((DescriptionItemString) pp).getV();
						break;
					case "ZP2015_OTHERID_SIG":
						signatura = ((DescriptionItemString) pp).getV();
						break;
					case "ZP2015_OTHERID_SIG_ORIG":
						puvodniSignatura = ((DescriptionItemString) pp).getV();
						break;
					default:
						// ignore
					}
				} else if ((pp instanceof DescriptionItemAPRef) && "ZP2015_ENTITY_ROLE".equals(pp.getT())
						&& GEO_ROLES.contains(pp.getS())) {
					var apRef = (DescriptionItemAPRef) pp;
					var accessPoint = elzaXmlReader.getApMap().get(apRef.getApid());
					if (accessPoint != null) {
						uuids.add(UUID.fromString(accessPoint.getApe().getUuid()));
					}
				} else if ("ZP2015_UNIT_ID".equals(pp.getT()) && (pp instanceof DescriptionItemString)) {
					DescriptionItemString refOznStr = (DescriptionItemString) pp;
					refOzn = refOznStr.getV();
				} else if ("ZP2015_INV_CISLO".equals(pp.getT()) && (pp instanceof DescriptionItemString)) {
					DescriptionItemString invCisloStr = (DescriptionItemString) pp;
					invCislo = invCisloStr.getV();
				} else if ("ZP2015_PACKET_NUMBER".equals(pp.getT()) && (pp instanceof DescriptionItemInteger)) {
					var uklC = (DescriptionItemInteger) pp;
					ukladaciCislo = uklC.getV().toString();
				} else if (ElzaTypes.ZP2015_UNIT_SUBTYPE.equals(pp.getT())
						&& "ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
					matrika = true;
				} else if (ElzaTypes.ZP2015_STORAGE_ID.equals(pp.getT())
						&& (pp instanceof DescriptionItemStructObjectRef)) {
					DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;
					StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
					ukladaciJednotka = so.getV();
				} else if (ElzaTypes.ZP2015_ITEM_ORDER.equals(pp.getT())) {
					var itemInt = (DescriptionItemInteger) pp;
					ukladaciJednotkaPoradi = itemInt.getV().intValue();
				} else if (ElzaTypes.ZP2015_RECORD_TYPE.equals(pp.getT()) && (pp instanceof DescriptionItemEnum)) {
					processRecordType(pp);
				} else if (ElzaTypes.ZP2015_DAO_LINK.equals(pp.getT())) {
					var uriRef = (DescriptionItemUriRef) pp;
					if (!EdxLinkConvertor.ELZA_NODE.equals(uriRef.getSchm())) {
						daoLink = uriRef.getUri();
					}
				} else if (ElzaTypes.ZP2015_DAO_ID.equals(pp.getT())) {
					var daoIdStr = (DescriptionItemString) pp;
					daoId = daoIdStr.getV();
				}
			}
		}
		
		private void collectInheritedAttributes(LevelContext levelContext) {
						
			for (var pp : levelContext.getLevel().getDdOrDoOrDp()) {
				// inherited attributes
				if ("ZP2015_INV_CISLO".equals(pp.getT()) && (pp instanceof DescriptionItemString)) {
					// try to find invCislo on parent level
					if (refOzn == null && invCislo == null) {
						invCislo = ((DescriptionItemString) pp).getV();
					}
				} else if (title == null && ElzaTypes.ZP2015_TITLE.equals(pp.getT())) {
					title = ((DescriptionItemString) pp).getV();
				}  else if (parentTitle == null && ElzaTypes.ZP2015_TITLE.equals(pp.getT())) {
					parentTitle = ((DescriptionItemString) pp).getV();
				} else if (unitDateText == null && unitDate == null && ("ZP2015_UNIT_DATE_TEXT".equals(pp.getT())||ElzaTypes.ZP2015_UNIT_DATE.equals(pp.getT()))) {
					if ("ZP2015_UNIT_DATE_TEXT".equals(pp.getT())) {
						unitDateText = ((DescriptionItemString) pp).getV();
					} else if (ElzaTypes.ZP2015_UNIT_DATE.equals(pp.getT())) {
						unitDate = UnitDateConvertor.convertToString((DescriptionItemUnitDate) pp);
					}
				} else if (ukladaciCislo == null && "ZP2015_PACKET_NUMBER".equals(pp.getT())
						&& (pp instanceof DescriptionItemInteger)) {
					var uklC = (DescriptionItemInteger) pp;
					ukladaciCislo = uklC.getV().toString();
				} else if (ElzaTypes.ZP2015_UNIT_SUBTYPE.equals(pp.getT())
						&& "ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
					matrika = true;
				} else if (ukladaciJednotka == null && ElzaTypes.ZP2015_STORAGE_ID.equals(pp.getT())
						&& (pp instanceof DescriptionItemStructObjectRef)) {
					DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;
					StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
					ukladaciJednotka = so.getV();
				} else if (ukladaciJednotkaPoradi==null && ElzaTypes.ZP2015_ITEM_ORDER.equals(pp.getT())) {
					var itemInt = (DescriptionItemInteger) pp;
					ukladaciJednotkaPoradi = itemInt.getV().intValue();
				} else if (!typZaznamuSet && ElzaTypes.ZP2015_RECORD_TYPE.equals(pp.getT())
						&& (pp instanceof DescriptionItemEnum)) {
					processRecordType(pp);
				}
			}			
		}

		private void processRecordType(DescriptionItem pp) {
			DescriptionItemEnum rt = (DescriptionItemEnum) pp;
			switch (rt.getS()) {
			case "ZP2015_RECORD_TYPE_BORN":
				narozeni = true;
				typZaznamuSet = true;
				break;
			case "ZP2015_RECORD_TYPE_MARRIED":
				oddani = true;
				typZaznamuSet = true;
				break;
			case "ZP2015_RECORD_TYPE_DIED":
				zemreli = true;
				typZaznamuSet = true;
				break;
			case "ZP2015_RECORD_TYPE_INDEX_BORN":
				narozeniIndex = true;
				typZaznamuSet = true;
				break;
			case "ZP2015_RECORD_TYPE_INDEX_MARRIED":
				oddaniIndex = true;
				typZaznamuSet = true;
				break;
			case "ZP2015_RECORD_TYPE_INDEX_DIED":
				zemreliIndex = true;
				typZaznamuSet = true;
				break;
			}
		}		
		
		private void createTypZaznamu() {
			StringJoiner sj = new StringJoiner(", ");
			if (narozeni) {
				sj.add("*");
			}
			if (narozeniIndex) {
				sj.add("*i");
			}
			if (oddani) {
				sj.add("oo");
			}
			if (oddaniIndex) {
				sj.add("ooi");
			}
			if (zemreli) {
				sj.add("+");
			}
			if (zemreliIndex) {
				sj.add("+i");
			}
			String tmpTypZaznamu = sj.toString();
			if (StringUtils.isNotBlank(tmpTypZaznamu)) {
				typZaznamu = tmpTypZaznamu;
			}
		}

		public DescriptionItemEnum getLevelType() {
			return levelType;
		}

		public DescriptionItemEnum getUnitType() {
			return unitType;
		}

		public String getUnitContent() {
			return unitContent;
		}

		public String getSpisZnak() {
			return spisZnak;
		}

		public String getSignatura() {
			return signatura;
		}

		public String getPuvodniSignatura() {
			return puvodniSignatura;
		}

		public String getUnitDateText() {
			return unitDateText;
		}

		public String getUnitDate() {
			return unitDate;
		}

		public List<UUID> getUuids() {
			return uuids;
		}

		public String getRefOzn() {
			return refOzn;
		}

		public String getInvCislo() {
			return invCislo;
		}

		public String getUkladaciCislo() {
			return ukladaciCislo;
		}

		public String getTitle() {
			return title;
		}

		public String getParentTitle() {
			return parentTitle;
		}

		public boolean isMatrika() {
			return matrika;
		}
		
		public String getUkladaciJednotka() {
			return ukladaciJednotka;
		}
		
		public Integer getUkladaciJednotkaPoradi() {
			return ukladaciJednotkaPoradi;
		}

		public String getTypZaznamu() {
			return typZaznamu;
		}
		
		public String getDaoLink() {
			return daoLink;
		}

		public String getDaoId() {
			return daoId;
		}
		
	}

	public interface ThumbnailFileStoreI {
		
		void fillThumbnailLink(String daoLink, Result result);
		
		/**
		 * Fill link from ZP2015_DAOID
		 * @param daoId
		 * @param result
		 * @return true - filled, false - not filled
		 */
		default boolean fillDaoIdLink(String daoId, Result result) {
			return false;
		}
		
	}
	
}
