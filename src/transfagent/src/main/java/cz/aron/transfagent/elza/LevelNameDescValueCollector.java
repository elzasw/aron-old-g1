package cz.aron.transfagent.elza;

import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;

import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.StructuredObject;

public class LevelNameDescValueCollector implements LevelNameDescValues {
	
	private final ElzaXmlReader elzaXmlReader;
	
	private final boolean descriptionGenerator;
	
	private String name;
	private boolean nameInherited;
	
	private boolean root;
	
	private boolean matrika;
	
	private DescriptionItemEnum levelType = null;
		
	private String typZaznamu = null;
	private boolean typZaznamuSet = false;
	private boolean oddani = false;
	private boolean oddaniIndex = false;
	private boolean narozeni = false;
	private boolean narozeniIndex = false;
	private boolean zemreli = false;
	private boolean zemreliIndex = false;
	
	private String ukladaciJednotka;
	private boolean ukladaciJednotkaInherited = false;
	
	private String datace = null;
	private boolean dataceInherited = false;
	
	private String nameStr;
	private int nameStrLevel = Integer.MAX_VALUE; 
	private String formalTitleStr;
	private int formalTitleStrLevel = Integer.MAX_VALUE;
	private String contentStr;
	private int contentStrLevel = Integer.MAX_VALUE;
	private String unitContentStr;
	private int unitContentStrLevel = Integer.MAX_VALUE;
	
	private String otherId;
	private String cj;
	private String docid;
	private String oldsig2;
	private String sigOrig;
	private String sig;
	private String oldSig;	
	
	public LevelNameDescValueCollector(ElzaXmlReader elzaXmlReader, boolean descriptionGenerator) {
		this.elzaXmlReader = elzaXmlReader;
		this.descriptionGenerator = descriptionGenerator;
	}
	
	public void collect(LevelContext levelContext) {
		collectLevelAttributes(levelContext);
		collectInheritedAttributes(levelContext);
		createTypZaznamu();
		int level = Math.min(Math.min(nameStrLevel, formalTitleStrLevel),
				Math.min(unitContentStrLevel, contentStrLevel));
		if (level != Integer.MAX_VALUE) {
			if (level > 0) {
				nameInherited = true;
			}
			if (descriptionGenerator) {				
				var suffix = createSuffix();
				if (level>0 && StringUtils.isNotBlank(suffix)) {
					// mam suffix nededim
					name = suffix;
				} else {
					if (level == nameStrLevel) {
						name = nameStr;
					} else if (level == contentStrLevel) {
						name = contentStr;
					} else if (level == formalTitleStrLevel) {
						name = formalTitleStr;
					} else if (level == unitContentStrLevel) {
						name = unitContentStr;
					}	
				}								
			} else {
				name = StringUtils.firstNonBlank(nameStr, formalTitleStr, contentStr, unitContentStr);
			}
		}
	}
		
	private String createSuffix() {
		int count = 0;
		var sj = new StringJoiner(", ");
		if(otherId!=null) {
			sj.add(otherId);
			count++;
		}		
		if(cj!=null) {
			sj.add(cj);
			count++;
		}
		if (docid!=null&&count<2) {
			sj.add(docid);
			count++;
		}
		if (oldsig2!=null&&count<2) {
			sj.add(oldsig2);
			count++;
		}
		if (sigOrig!=null&&count<2) {
			sj.add(sigOrig);
			count++;
		}
		if (sig!=null&&count<2) {
			sj.add(sig);
			count++;
		}
		if (oldSig!=null&&count<2) {
			sj.add(oldSig);
			count++;
		}
		return sj.toString();
	}

	private void collectLevelAttributes(LevelContext levelContext) {
		for (var pp : levelContext.getLevel().getDdOrDoOrDp()) {
			switch (pp.getT()) {
			case ElzaTypes.ZP2015_LEVEL_TYPE:
				levelType = (DescriptionItemEnum) pp;
				if (ElzaTypes.ZP2015_LEVEL_ROOT.equals(levelType.getS())) {
					root = true;
				}
				break;
			case ElzaTypes.ZP2015_UNIT_SUBTYPE:
				if ("ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
					matrika = true;
				}
				break;
			case ElzaTypes.ZP2015_NAME:
				nameStr = ((DescriptionItemString) pp).getV();
				nameStrLevel = 0;
				break;
			case ElzaTypes.ZP2015_FORMAL_TITLE:
				formalTitleStr = ((DescriptionItemString) pp).getV();
				formalTitleStrLevel = 0;
				break;
			case ElzaTypes.ZP2015_CONTENT:
				contentStr = ((DescriptionItemString) pp).getV();
				contentStrLevel = 0;
				break;
			case ElzaTypes.ZP2015_UNIT_CONTENT:
				unitContentStr = ((DescriptionItemString) pp).getV();
				unitContentStrLevel = 0;
				break;
			case ElzaTypes.ZP2015_RECORD_TYPE:
				if (!typZaznamuSet && (pp instanceof DescriptionItemEnum)) {
					processRecordType(pp);
				}
				break;
			case ElzaTypes.ZP2015_STORAGE_ID:
				DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;
				StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
				if (so!=null) {
					ukladaciJednotka = so.getV();
				}
				break;
			case ElzaTypes.ZP2015_UNIT_DATE:
				datace = UnitDateConvertor.convertToString2((DescriptionItemUnitDate) pp);
				break;
			case "ZP2015_OTHER_ID":
				otherId = ((DescriptionItemString) pp).getV();
				break;
			case "ZP2015_OTHERID_CJ":
				cj = ((DescriptionItemString) pp).getV();
				break;
			case "ZP2015_OTHERID_DOCID":
				docid = ((DescriptionItemString) pp).getV();
				break;
			case "ZP2015_OTHERID_OLDSIG2":
				oldsig2 = ((DescriptionItemString) pp).getV();
				break;
			case "ZP2015_OTHERID_SIG_ORIG":
				sigOrig = ((DescriptionItemString) pp).getV();
				break;
			case "ZP2015_OTHERID_SIG":
				sig = ((DescriptionItemString) pp).getV();
				break;
			case "ZP2015_OTHERID_OLDSIG":
				oldSig = ((DescriptionItemString) pp).getV();
				break;
			}
		}
	}

	private void collectInheritedAttributes(LevelContext levelContext) {
		var context = levelContext.getParentContext();
		while (context != null) {
			int level = 1;
			for (var pp : context.getLevel().getDdOrDoOrDp()) {
				switch (pp.getT()) {
				case ElzaTypes.ZP2015_NAME:
					if (nameStr==null) {
						nameStr = ((DescriptionItemString) pp).getV();
						nameStrLevel = level;
					}
					break;
				case ElzaTypes.ZP2015_FORMAL_TITLE:
					if (formalTitleStr==null) {
						formalTitleStr = ((DescriptionItemString) pp).getV();
						formalTitleStrLevel = level;
					}
					break;
				case ElzaTypes.ZP2015_CONTENT:
					if (contentStr==null) {
						contentStr = ((DescriptionItemString) pp).getV();
						contentStrLevel = level;
					}					
					break;
				case ElzaTypes.ZP2015_UNIT_CONTENT:
					if (unitContentStr==null) {
						unitContentStr = ((DescriptionItemString) pp).getV();
						unitContentStrLevel = level;
					}					
					break;
				case ElzaTypes.ZP2015_RECORD_TYPE:
					if (!typZaznamuSet && (pp instanceof DescriptionItemEnum)) {
						processRecordType(pp);
					}
					break;
				case ElzaTypes.ZP2015_STORAGE_ID:
					DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;
					StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
					if (so != null) {
						ukladaciJednotka = so.getV();
					}
					break;
				case ElzaTypes.ZP2015_UNIT_DATE:
					if (datace == null && (pp instanceof DescriptionItemUnitDate)) {
						datace = UnitDateConvertor.convertToString2((DescriptionItemUnitDate) pp);
						dataceInherited = true;
					}
					break;					
				}
			}
			context = context.getParentContext();
			level++;
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

	public boolean isRoot() {
		return root;
	}

	public void setRoot(boolean root) {
		this.root = root;
	}

	public boolean isMatrika() {
		return matrika;
	}

	public void setMatrika(boolean matrika) {
		this.matrika = matrika;
	}

	@Override
	public ElzaXmlReader getElzaXmlReader() {
		return elzaXmlReader;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isTitleInherited() {
		return nameInherited;
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
	public String getDatace() {
		return datace;
	}

	@Override
	public boolean isDataceInherited() {
		return dataceInherited;
	}

	@Override
	public String getTypZaznamu() {
		return typZaznamu;
	}

}
