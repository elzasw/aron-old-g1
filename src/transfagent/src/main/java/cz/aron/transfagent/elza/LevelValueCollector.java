package cz.aron.transfagent.elza;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.convertor.EdxLinkConvertor;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;
import cz.tacr.elza.schema.v2.StructuredObject;

public class LevelValueCollector implements LevelNameDescValues, LevelResultValues {
	
	private static final Set<String> GEO_ROLES = new HashSet<>(Arrays.asList("ZP2015_ENTITY_ROLE_80", "ZP2015_ENTITY_ROLE_86",
			"ZP2015_ENTITY_ROLE_56", "ZP2015_ENTITY_ROLE_57", "ZP2015_ENTITY_ROLE_88", "ZP2015_ENTITY_ROLE_58",
			"ZP2015_ENTITY_ROLE_60", "ZP2015_ENTITY_ROLE_61", "ZP2015_ENTITY_ROLE_62", "ZP2015_ENTITY_ROLE_89",
			"ZP2015_ENTITY_ROLE_64", "ZP2015_ENTITY_ROLE_65", "ZP2015_ENTITY_ROLE_71", "ZP2015_ENTITY_ROLE_82"));
	
	private final ElzaXmlReader elzaXmlReader;
	
	private DescriptionItemEnum levelType = null;

	private DescriptionItemEnum unitType = null;
	
	private boolean root = false;
	private boolean matrika = false;

	private String title = null;
	private boolean titleInherited = false;

	private String parentTitle;
	
	private String unitContent;
	
	private String content;
	private boolean contentInherited = false;

	private String ukladaciJednotka = null;
	private boolean ukladaciJednotkaInherited = false;
	private Integer ukladaciJednotkaPoradi = null;
	
	private String datace = null;
	private boolean dataceInherited = false;
	
	private String unitDateText = null;
	
	private String spisZnak = null;
	private String signatura = null;
	private String puvodniSignatura = null;
	
	private String refOzn = null;
	private String invCislo = null;
	private String ukladaciCislo = null;
	
	private String daoLink = null;
	private String daoId = null;
	
	private final List<UUID> uuids = new ArrayList<>();

	private String typZaznamu = null;
	private boolean typZaznamuSet = false;
	private boolean oddani = false;
	private boolean oddaniIndex = false;
	private boolean narozeni = false;
	private boolean narozeniIndex = false;
	private boolean zemreli = false;
	private boolean zemreliIndex = false;
	
	public LevelValueCollector(ElzaXmlReader elzaXmlReader) {
		this.elzaXmlReader = elzaXmlReader;
	}

	public void collect(LevelContext levelContext) {		
		var context = levelContext;		
		collectLevelAttributes(context);
		collectInheritedAttributes(context);		
		createTypZaznamu();
		
		// fallback pro sarsi pomucky
		if (title == null) {			
			title = content;
		}
	}

	
	private void collectLevelAttributes(LevelContext context) {
		
		String nameStr = null;
		String formalTitleStr = null;
		String contentStr = null;
		String unitContentStr = null;
		
		// not inherited attributes
		for (var pp : context.getLevel().getDdOrDoOrDp()) {			
			switch(pp.getT()) {
			case ElzaTypes.ZP2015_LEVEL_TYPE:
				levelType = (DescriptionItemEnum) pp;
				if (ElzaTypes.ZP2015_LEVEL_ROOT.equals(levelType.getS())) {
					root = true;
				}
				break;
			case ElzaTypes.ZP2015_UNIT_TYPE:
				unitType = (DescriptionItemEnum) pp;
				break;
			case ElzaTypes.ZP2015_NAME:
				nameStr = ((DescriptionItemString) pp).getV();
				break;
			case ElzaTypes.ZP2015_FORMAL_TITLE:
				formalTitleStr = ((DescriptionItemString) pp).getV();
				break;
			case ElzaTypes.ZP2015_CONTENT:
				contentStr = ((DescriptionItemString) pp).getV();
				content = contentStr;
				break;
			case ElzaTypes.ZP2015_UNIT_CONTENT:
				unitContentStr = ((DescriptionItemString) pp).getV();
				unitContent = unitContentStr;
				break;
			case ElzaTypes.ZP2015_UNIT_DATE:
				datace = UnitDateConvertor.convertToString2((DescriptionItemUnitDate) pp);
				break;
			case ElzaTypes.ZP2015_STORAGE_ID:
				DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;
				StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
				if (so!=null) {
					ukladaciJednotka = so.getV();
				}
				break;
			case ElzaTypes.ZP2015_ITEM_ORDER:
				var itemInt = (DescriptionItemInteger) pp;
				ukladaciJednotkaPoradi = itemInt.getV().intValue();
				break;
			case ElzaTypes.ZP2015_UNIT_SUBTYPE:
				if ("ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
					matrika = true;
				}
				break;
			case ElzaTypes.ZP2015_RECORD_TYPE:
				if (pp instanceof DescriptionItemEnum) {
					processRecordType(pp);
				}
				break;
			case ElzaTypes.ZP2015_OTHER_ID:
				if (pp.getS() != null) {
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
				}
				break;
			case ElzaTypes.ZP2015_UNIT_ID:
				refOzn = ((DescriptionItemString) pp).getV();
				break;
			case ElzaTypes.ZP2015_INV_CISLO:
				invCislo = ((DescriptionItemString) pp).getV();
				break;
			case ElzaTypes.ZP2015_PACKET_NUMBER:
				ukladaciCislo = ((DescriptionItemInteger) pp).getV().toString();
				break;
			case ElzaTypes.ZP2015_ENTITY_ROLE:
				if (GEO_ROLES.contains(pp.getS())) {
					var apRef = (DescriptionItemAPRef) pp;
					var accessPoint = elzaXmlReader.getApMap().get(apRef.getApid());
					if (accessPoint != null) {
						uuids.add(UUID.fromString(accessPoint.getApe().getUuid()));
					}	
				}
				break;
			case ElzaTypes.ZP2015_DAO_LINK:
				var uriRef = (DescriptionItemUriRef) pp;
				if (!EdxLinkConvertor.ELZA_NODE.equals(uriRef.getSchm())) {
					daoLink = uriRef.getUri();
				}
				break;
			case ElzaTypes.ZP2015_DAO_ID:
				daoId = ((DescriptionItemString) pp).getV();
				break;
			case ElzaTypes.ZP2015_UNIT_DATE_TEXT:
				unitDateText = ((DescriptionItemString) pp).getV();
				break;
			}
		}
		title = StringUtils.firstNonBlank(nameStr/*, formalTitleStr, contentStr, unitContentStr*/);
	}
	
	private void collectInheritedAttributes(LevelContext context) {
		
		String nameStr = null;
		String formalTitleStr = null;
		String contentStr = null;;
		String unitContentStr = null;
				
		// inherited attributes
		while ((context = context.getParentContext()) != null) {
			for (var pp : context.getLevel().getDdOrDoOrDp()) {
				switch (pp.getT()) {
				case ElzaTypes.ZP2015_NAME:
					if (title == null || parentTitle == null) {
						nameStr = ((DescriptionItemString) pp).getV();
					}
					break;
				case ElzaTypes.ZP2015_FORMAL_TITLE:
					if (title == null || parentTitle == null) {
						formalTitleStr = ((DescriptionItemString) pp).getV();
					}
					break;
				case ElzaTypes.ZP2015_CONTENT:
					if (title == null || parentTitle == null) {
						contentStr = ((DescriptionItemString) pp).getV();
					}
					if (content == null) {
						content = ((DescriptionItemString) pp).getV();
						contentInherited = true;
					}
					break;
				case ElzaTypes.ZP2015_UNIT_CONTENT:
					if (title == null || parentTitle == null) {
						unitContentStr = ((DescriptionItemString) pp).getV();
					}
					break;
				case ElzaTypes.ZP2015_UNIT_DATE:
					if (datace == null && (pp instanceof DescriptionItemUnitDate)) {
						datace = UnitDateConvertor.convertToString2((DescriptionItemUnitDate) pp);
						dataceInherited = true;
					}
					break;
				case ElzaTypes.ZP2015_UNIT_DATE_TEXT:
					if (unitDateText == null && datace == null) {
						unitDateText = ((DescriptionItemString) pp).getV();
					}
					break;
				case ElzaTypes.ZP2015_STORAGE_ID:
					if (ukladaciJednotka == null && (pp instanceof DescriptionItemStructObjectRef)) {
						DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;
						StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
						if (so!=null) {
							ukladaciJednotka = so.getV();
							ukladaciJednotkaInherited = true;
						}
					}
					break;
				case ElzaTypes.ZP2015_PACKET_NUMBER:
					if (ukladaciCislo == null) {
						ukladaciCislo = ((DescriptionItemInteger) pp).getV().toString();
					}
					break;
				case ElzaTypes.ZP2015_ITEM_ORDER:
					if (ukladaciJednotkaPoradi == null) {
						ukladaciJednotkaPoradi = ((DescriptionItemInteger) pp).getV().intValue();	
					}
					break;
				case ElzaTypes.ZP2015_UNIT_SUBTYPE:
					if ("ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
						matrika = true;
					}
					break;
				case ElzaTypes.ZP2015_RECORD_TYPE:
					if (!typZaznamuSet && (pp instanceof DescriptionItemEnum)) {
						processRecordType(pp);
					}
					break;
				}				
			}			
			if (title==null) {
				title = StringUtils.firstNonBlank(nameStr/*, formalTitleStr, contentStr, unitContentStr*/);				
				if (title!=null) {
					nameStr = null;
					formalTitleStr = null;
					contentStr = null;
					unitContentStr = null;
					titleInherited = true;
				}				
			} else if (parentTitle==null) {
				parentTitle = StringUtils.firstNonBlank(nameStr/*, formalTitleStr, contentStr, unitContentStr*/);
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

	@Override
	public ElzaXmlReader getElzaXmlReader() {
		return elzaXmlReader;
	}

	@Override
	public DescriptionItemEnum getLevelType() {
		return levelType;
	}

	@Override
	public DescriptionItemEnum getUnitType() {
		return unitType;
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public boolean isTitleInherited() {
		return titleInherited;
	}

	@Override
	public String getUkladaciJednotka() {
		return ukladaciJednotka;
	}

	@Override
	public boolean isUkladaciJednotkaInherited() {
		return ukladaciJednotkaInherited;
	}

	@Override
	public Integer getUkladaciJednotkaPoradi() {
		return ukladaciJednotkaPoradi;
	}

	@Override
	public String getDatace() {
		return datace;
	}

	@Override
	public boolean isDataceInherited() {
		return dataceInherited;
	}

	@Override
	public String getSpisZnak() {
		return spisZnak;
	}

	@Override
	public String getSignatura() {
		return signatura;
	}

	@Override
	public String getPuvodniSignatura() {
		return puvodniSignatura;
	}

	@Override
	public String getRefOzn() {
		return refOzn;
	}

	@Override
	public String getInvCislo() {
		return invCislo;
	}

	@Override
	public String getUkladaciCislo() {
		return ukladaciCislo;
	}

	@Override
	public String getDaoLink() {
		return daoLink;
	}

	@Override
	public String getDaoId() {
		return daoId;
	}

	@Override
	public List<UUID> getUuids() {
		return uuids;
	}

	@Override
	public boolean isRoot() {
		return root;
	}

	@Override
	public boolean isMatrika() {
		return matrika;
	}

	@Override
	public String getTypZaznamu() {
		return typZaznamu;
	}

	@Override
	public String getUnitContent() {
		return unitContent;
	}

	@Override
	public String getUnitDateText() {
		return unitDateText;
	}

	@Override
	public String getUnitDate() {
		return datace;
	}

	@Override
	public String getParentName() {
		return parentTitle;
	}


}
