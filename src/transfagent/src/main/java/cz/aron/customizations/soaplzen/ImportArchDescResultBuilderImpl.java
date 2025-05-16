package cz.aron.customizations.soaplzen;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.core.api.rest.model.Result;
import cz.aron.core.api.rest.model.ResultRowItem;
import cz.aron.core.api.rest.model.ResultRowItemValue;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.elza.ImportArchDescResultBuilder;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Section;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportArchDescResultBuilderImpl implements ImportArchDescResultBuilder {

	private final ObjectMapper objectMapper;

	public ImportArchDescResultBuilderImpl(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String buildResult(InstitutionInfo instInfo, Section sect, Level lvl, LevelContext levelContext,
			UUID fundApuUuid) {
		var result = new Result();
		result.setId(lvl.getUuid());
		result.setT("A");
		result.setTn("https://www.inventare.cz/fcgi-bin/iipsrv.fcgi?DeepZoom=archival-aid/soap-pn/soap-pn_ap0002_10005_ku-plzen2/soap-pn_ap0002_10005_ku-plzen2_0030.jp2");

		generateName(result, levelContext, sect);
		generateDate(result, levelContext);
		generateSignature(result, levelContext);
		generateInstitutionAndFund(result, instInfo, sect, fundApuUuid, levelContext);

		try {
			return objectMapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateName(Result result, LevelContext levelContext, Section sect) {
		var rowItem = new ResultRowItem();
		rowItem.setT("name");
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
			rowItem.setT("DATE");
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
			if (item.getT().equals("ZP2015_DATE")) {
				DescriptionItemUnitDate unitDate = (DescriptionItemUnitDate) item;
				return UnitDateConvertor.convertToString(unitDate);
			}
		}
		return getDate(levelContext.getParentContext());
	}
	
	public void generateSignature(Result result, LevelContext levelContext) {
		String refOzn = null;
        String invCislo = null;
        String poradoveCislo = null;
        String otherIdent = null;
        String otherIdentType = null;
        int otherIdentPriority = -1;
        for(DescriptionItem item : levelContext.getLevel().getDdOrDoOrDp()) {
            if(item.getT().equals("ZP2015_UNIT_ID")&&(item instanceof DescriptionItemString)) {
                DescriptionItemString title = (DescriptionItemString)item;
                refOzn = title.getV();
            } else 
            if(item.getT().equals("ZP2015_INV_CISLO")&&(item instanceof DescriptionItemString)) {
                DescriptionItemString title = (DescriptionItemString)item;
                invCislo = title.getV();
            } else 
            if(item.getT().equals("ZP2015_SERIAL_NUMBER")&&(item instanceof DescriptionItemInteger)) {
                DescriptionItemInteger serNum = (DescriptionItemInteger)item;
                poradoveCislo = serNum.getV().toString();
            } else 
            if(item.getT().equals(ElzaTypes.ZP2015_OTHER_ID)&&(item instanceof DescriptionItemString)) {
                DescriptionItemString otherId = (DescriptionItemString) item;
                if(ImportArchDesc.otherIdPriorityMap.get(otherId.getS())>otherIdentPriority) {
                    otherIdentPriority = ImportArchDesc.otherIdPriorityMap.get(otherId.getS());
                    otherIdentType = ElzaTypes.otherIdNameMap.get(otherId.getS());
                    otherIdent = otherId.getV();
                }
            }
        }
        
        boolean somethingAdded = false;
        var rowItem = new ResultRowItem();
		rowItem.setT("SIGNATURE");		
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
		if (StringUtils.isNotBlank(otherIdent)) {
			var itemValue = new ResultRowItemValue();
			itemValue.setV(otherIdent);
			rowItem.addVItem(itemValue);
			somethingAdded = true;
		}
		if (somethingAdded) {
			result.addLItem(Arrays.asList(rowItem));
		}
	}

	public void generateInstitutionAndFund(Result result, InstitutionInfo institutionInfo, Section sect,
			UUID fundApuUuid, LevelContext levelContext) {
		var instName = institutionInfo.getShortName();
		var instItem = new ResultRowItem();
		instItem.setT("INST_REF");
		var itemValue = new ResultRowItemValue();
		itemValue.setV(instName);
		itemValue.id(institutionInfo.getUuid().toString());
		instItem.addVItem(itemValue);
		var fundItem = new ResultRowItem();
		fundItem.setT("F");
		itemValue = new ResultRowItemValue();
		itemValue.setV(sect.getFi().getN());
		itemValue.id(fundApuUuid.toString());
		fundItem.addVItem(itemValue);
		List<ResultRowItem> items;
		var parentTitle = getTitle(levelContext.getParentContext());
		if (parentTitle == null&&levelContext.getParentContext()!=null&&levelContext.getParentContext().getParentContext()==null) {
			parentTitle = sect.getFi().getN();
		}				
		if (parentTitle != null) {
			var contextItem = new ResultRowItem();
			contextItem.setT("CTX");
			itemValue = new ResultRowItemValue();
			itemValue.setV(parentTitle);
			itemValue.id(levelContext.getParentContext().getLevel().getUuid().toString());
			contextItem.addVItem(itemValue);
			items = Arrays.asList(instItem, contextItem, fundItem);
		} else {
			items = Arrays.asList(instItem, fundItem);
		}
		result.addLItem(items);
	}

}
