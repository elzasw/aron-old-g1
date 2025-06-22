package cz.aron.customizations.soaplzen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Section;

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
	private static final String ARCHIVALIE_LEVEL_JEDNOTLIVOST_VYKRES = "A_D";
	private static final String ARCHIVALIE_LEVEL_JEDNOTLIVOST_CAST = "A_IP";

	private final ObjectMapper objectMapper;

	private final ArchivalEntityRepository aeRepository;

	public ImportArchDescResultBuilderImpl(ObjectMapper objectMapper, ArchivalEntityRepository aeRepository) {
		this.objectMapper = objectMapper;
		this.aeRepository = aeRepository;
	}

	@Override
	public String buildResult(InstitutionInfo instInfo, Section sect, Level lvl, LevelContext levelContext,
			UUID fundApuUuid, ElzaXmlReader elzaXmlReader) {

		var valueCollector = new ValueCollector(elzaXmlReader);
		valueCollector.collect(levelContext, true);

		var result = new Result();
		result.setId(lvl.getUuid());
		result.setT(getType(valueCollector));
		result.setTn(
				"https://www.inventare.cz/fcgi-bin/iipsrv.fcgi?DeepZoom=archival-aid/soap-pn/soap-pn_ap0002_10005_ku-plzen2/soap-pn_ap0002_10005_ku-plzen2_0030.jp2");

		generateName(result, levelContext, sect, valueCollector);
		generateDate(result, valueCollector);
		generateUnitContent(result, valueCollector);
		generateGeoEntities(result, levelContext, elzaXmlReader, valueCollector);
		generateSignature(result, valueCollector);
		generateNumber(result, valueCollector);
		generateInstitutionAndFund(result, instInfo, sect, fundApuUuid);
		generateParent(result, levelContext, sect, valueCollector);
		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
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
		String poradoveCislo = valueCollector.getPoradoveCislo();
		String ukladaciCislo = valueCollector.getUkladaciCislo();

		var items = new ArrayList<ResultRowItem>(2);

		boolean somethingAdded = false;
		var rowItem = new ResultRowItem();
		if (StringUtils.isNotBlank(refOzn)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(refOzn);
			rowItem.addVItem(itemValue);
			rowItem.setT("J_IC");
			somethingAdded = true;
		} else if (StringUtils.isNotBlank(invCislo)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(invCislo);
			rowItem.addVItem(itemValue);
			rowItem.setT("J_IC");
			somethingAdded = true;
		} else if (StringUtils.isNotBlank(poradoveCislo)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(poradoveCislo);
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

		var geoItems = new ResultRowItem();
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
				geoItems.setT("J_G");
				result.addLItem(Arrays.asList(geoItems));
			}
		}

	}

	private String getType(ValueCollector valueCollector) {
		var levelType = valueCollector.getLevelType();
		if (ElzaTypes.ZP2015_LEVEL_PART.equals(levelType.getS())) {
			return ARCHIVALIE_LEVEL_JEDNOTLIVOST_CAST;
		} else if (ElzaTypes.ZP2015_LEVEL_ITEM.equals(levelType.getS())) {
			var unitType = valueCollector.getUnitType();
			if ("ZP2015_UNIT_TYPE_TVY".equals(unitType.getS())) {
				return ARCHIVALIE_LEVEL_JEDNOTLIVOST_VYKRES;
			} else if ("ZP2015_UNIT_TYPE_TVY".equals(unitType.getS())) {
				return ARCHIVALIE_LEVEL_JEDNOTLIVOST_KNIHA;
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
		String poradoveCislo = null;
		String ukladaciCislo = null;

		String title = null;
		String parentTitle = null;

		public ValueCollector(ElzaXmlReader elzaXmlReader) {
			this.elzaXmlReader = elzaXmlReader;
		}

		public void collect(LevelContext levelContext, boolean leaf) {

			for (var pp : levelContext.getLevel().getDdOrDoOrDp()) {

				// not inherited attributes
				if (leaf) {
					if (ElzaTypes.ZP2015_LEVEL_TYPE.equals(pp.getT())) {
						levelType = (DescriptionItemEnum) pp;
					} else if (ElzaTypes.ZP2015_UNIT_TYPE.equals(pp.getT())) {
						unitType = (DescriptionItemEnum) pp;
					} else if ("ZP2015_UNIT_CONTENT".equals(pp.getT())) {
						unitContent = ((DescriptionItemString) pp).getV();
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
						DescriptionItemString title = (DescriptionItemString) pp;
						refOzn = title.getV();
					} else if ("ZP2015_INV_CISLO".equals(pp.getT()) && (pp instanceof DescriptionItemString)) {
						DescriptionItemString title = (DescriptionItemString) pp;
						invCislo = title.getV();
					} else if ("ZP2015_SERIAL_NUMBER".equals(pp.getT()) && (pp instanceof DescriptionItemInteger)) {
						DescriptionItemInteger serNum = (DescriptionItemInteger) pp;
						poradoveCislo = serNum.getV().toString();
					} else if ("ZP2015_PACKET_NUMBER".equals(pp.getT()) && (pp instanceof DescriptionItemString)) {
						DescriptionItemString uklC = (DescriptionItemString) pp;
						ukladaciCislo = uklC.getV();
					}
				} else {
					if ("ZP2015_INV_CISLO".equals(pp.getT()) && (pp instanceof DescriptionItemString)) {
						// try to find invCislo on parent level
						if (refOzn == null && invCislo == null) {
							invCislo = ((DescriptionItemString) pp).getV();
						}
					} else if (parentTitle == null && "ZP2015_TITLE".equals(pp.getT())) {
						parentTitle = ((DescriptionItemString) pp).getV();
					}
				}

				// inherited attributes
				if (unitDateText == null && unitDate == null) {
					if ("ZP2015_UNIT_DATE_TEXT".equals(pp.getT())) {
						unitDateText = ((DescriptionItemString) pp).getV();
					} else if (ElzaTypes.ZP2015_UNIT_DATE.equals(pp.getT())) {
						unitDate = UnitDateConvertor.convertToString((DescriptionItemUnitDate) pp);
					}
				}
				if (title == null && "ZP2015_TITLE".equals(pp.getT())) {
					title = ((DescriptionItemString) pp).getV();
				}

			}

			if (levelContext.getParentContext() != null) {
				collect(levelContext.getParentContext(), false);
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

		public String getPoradoveCislo() {
			return poradoveCislo;
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

	}

}
