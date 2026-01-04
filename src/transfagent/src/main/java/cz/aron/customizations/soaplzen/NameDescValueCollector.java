package cz.aron.customizations.soaplzen;

import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.StructuredObject;

public class NameDescValueCollector {
	
	private final ElzaXmlReader elzaXmlReader;
	
	boolean root = false;
	boolean matrika = false;

	private String title = null;
	private boolean titleInherited = false;
	
	private String ukladaciJednotka = null;
	private boolean ukladaciJednotkaInherited = false;
	
	private String datace = null;
	private boolean dataceInherited = false;
	
	public NameDescValueCollector(ElzaXmlReader elzaXmlReader) {
		this.elzaXmlReader = elzaXmlReader;
	}

	public void collect(LevelContext levelContext) {		
		var context = levelContext;
		
		// not inherited attributes
		for (var pp : context.getLevel().getDdOrDoOrDp()) {
			if (ElzaTypes.ZP2015_LEVEL_TYPE.equals(pp.getT())) {
				DescriptionItemEnum levelType = (DescriptionItemEnum) pp;
				if (ElzaTypes.ZP2015_LEVEL_ROOT.equals(levelType.getS())) {
					root = true;
				}
			} else if (ElzaTypes.ZP2015_TITLE.equals(pp.getT())) {
				DescriptionItemString titleItem = (DescriptionItemString) pp;
				title = titleItem.getV();
			} else if (pp.getT().equals(ElzaTypes.ZP2015_UNIT_DATE) && (pp instanceof DescriptionItemUnitDate)) {
				DescriptionItemUnitDate dataceUnitDate = (DescriptionItemUnitDate) pp;
				datace = UnitDateConvertor.convertToString(dataceUnitDate);
			} else if (ElzaTypes.ZP2015_STORAGE_ID.equals(pp.getT())
					&& (pp instanceof DescriptionItemStructObjectRef)) {
				DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;
				StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
				ukladaciJednotka = so.getV();
			} else if (ElzaTypes.ZP2015_UNIT_SUBTYPE.equals(pp.getT()) && "ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
				matrika = true;
			}
		}
		
		// inherited attributes
		while((context = context.getParentContext())!=null) {
			for (var pp : context.getLevel().getDdOrDoOrDp()) {			

					if (title==null&&ElzaTypes.ZP2015_TITLE.equals(pp.getT())) {
		                DescriptionItemString titleItem = (DescriptionItemString) pp;
		                title = titleItem.getV();
		                titleInherited = true;
		            } else if (datace==null&&pp.getT().equals(ElzaTypes.ZP2015_UNIT_DATE) && (pp instanceof DescriptionItemUnitDate)) {
		                DescriptionItemUnitDate dataceUnitDate = (DescriptionItemUnitDate) pp;
		                datace = UnitDateConvertor.convertToString(dataceUnitDate);
		                dataceInherited = true;
		            }  else if (ukladaciJednotka==null&&ElzaTypes.ZP2015_STORAGE_ID.equals(pp.getT()) && (pp instanceof DescriptionItemStructObjectRef)) {
						DescriptionItemStructObjectRef uklJ = (DescriptionItemStructObjectRef) pp;						
						StructuredObject so = elzaXmlReader.getSoMap().get(uklJ.getSoid());
						ukladaciJednotka = so.getV();
						ukladaciJednotkaInherited = true;
					} else if (ElzaTypes.ZP2015_UNIT_SUBTYPE.equals(pp.getT()) && "ZP2015_UNIT_SUBTYPE_MTR".equals(pp.getS())) {
						matrika = true;
					}										
			}
		}	
	}

	public ElzaXmlReader getElzaXmlReader() {
		return elzaXmlReader;
	}

	public String getTitle() {
		return title;
	}

	public boolean isTitleInherited() {
		return titleInherited;
	}

	public String getUkladaciJednotka() {
		return ukladaciJednotka;
	}

	public boolean isUkladaciJednotkaInherited() {
		return ukladaciJednotkaInherited;
	}

	public String getDatace() {
		return datace;
	}

	public boolean isDataceInherited() {
		return dataceInherited;
	}

	public boolean isRoot() {
		return root;
	}

	public boolean isMatrika() {
		return matrika;
	}

}
