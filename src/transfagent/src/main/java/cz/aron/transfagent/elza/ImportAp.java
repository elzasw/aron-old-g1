package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.google.common.base.Objects;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.convertor.EdxEnumConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemCovertContext;
import cz.aron.transfagent.elza.convertor.EdxStringConvertor;
import cz.aron.transfagent.elza.convertor.EdxUnitDateConvertor;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;
import cz.tacr.elza.schema.v2.Level;

public class ImportAp implements EdxItemCovertContext {

    ElzaXmlReader elzaXmlReader;

    ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

    private final ApTypeService apTypeService;
    
    private final ElzaNameBuilder nameBuilder;

    private ContextDataProvider dataProvider;

    Integer parentElzaId;

    private UUID apUuid;

    private Integer elzaId;
    
    private String entityClass;

    /**
     * ElzaIds which needs to be requested
     */
    private Set<Integer> requiredEntities = new HashSet<>();

    private Part activePart;

    private Apu apu;

    public ImportAp(ApTypeService apTypeService, ElzaNameBuilder nameBuilder) {
        this.apTypeService = apTypeService;
        this.nameBuilder = nameBuilder;
    }

    public UUID getApUuid() {
        return apUuid;
    }
    
    public String getEntityClass() {
        return entityClass;
    }

    public Set<Integer> getRequiredEntities() {
        return requiredEntities;
    }

    public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);        
        ApTypeService apTypeService = new ApTypeService();
        ElzaNameBuilder nameBuilder = new ElzaNameBuilder(apTypeService);        
        ImportAp iap = new ImportAp(apTypeService, nameBuilder);
        try {
            ApuSourceBuilder apusrcBuilder = iap.importAp(inputFile, args[1]);
            Path ouputPath = Paths.get(args[2]);
            try(OutputStream fos = Files.newOutputStream(ouputPath)) {
                apusrcBuilder.build(fos);
            }
        } catch(Exception e) {
            System.err.println("Failed to process input file: " + inputFile);
            e.printStackTrace();
        }
    }

    private ApuSourceBuilder importAp(Path inputFile, String propFile) throws IOException, JAXBException {
        Validate.isTrue(propFile!=null&&propFile.length()>0);

        PropertiesDataProvider pdp = new PropertiesDataProvider();
        Path propPath = Paths.get(propFile);
        pdp.load(propPath);
        return importAp(inputFile, null, pdp);
    }

    public ApuSourceBuilder importAp(Path inputFile, UUID expUuid, final ContextDataProvider cdp) throws IOException, JAXBException {
        this.dataProvider = cdp;

        try(InputStream is = Files.newInputStream(inputFile);) {
            elzaXmlReader = ElzaXmlReader.read(is);
            return importAp(expUuid);
        }
    }

    private ApuSourceBuilder importAp(final UUID expUuid) {
        AccessPoint ap;
        if(expUuid!=null) {
            ap = elzaXmlReader.findAccessPointByUUID(expUuid.toString());
            if(ap==null) {
                throw new IllegalStateException("AccessPoint not found: "+expUuid);
            }
        } else {
            // find first
            ap = elzaXmlReader.getSingleAccessPoint();
            if(ap==null) {
                throw new IllegalStateException("AccessPoint not found in result");
            }
        }
        this.apUuid = UUID.fromString(ap.getApe().getUuid());
        this.elzaId = Integer.valueOf(ap.getApe().getId());
        this.entityClass = ap.getApe().getT();

        apu = apusBuilder.createApu(null, ApuType.ENTITY, apUuid);

        // extrakce dat
        // entity info
        String entityClass = ap.getApe().getT();
        String ecName = apTypeService.getTypeName(entityClass);
        Validate.notNull(ecName, "Entity class name not found, code: %s", entityClass);
        Part aeInfoPart = apusBuilder.addPart(apu, CoreTypes.PT_AE_INFO);
        apusBuilder.addEnum(aeInfoPart, CoreTypes.AE_SUBCLASS, ecName, true);

        String parentEcName = apTypeService.getParentName(entityClass);
        if(parentEcName!=null) {
            apusBuilder.addEnum(aeInfoPart, CoreTypes.AE_CLASS, parentEcName, false);
        }

        // oznaceni
        Fragments frgs = ap.getFrgs();
        if(frgs==null) {        	
        	throw new MarkAsNonAvailableException("AP without fragments, uuid: "+apUuid + ", elzaId: "+elzaId);        
        }
        for(Fragment frg: frgs.getFrg()) {
            switch(frg.getT()) {
            case "PT_NAME":
                importName(apu, frg);
                break;
            case "PT_BODY":
                importBody(apu, aeInfoPart, frg);
                break;
            case "PT_IDENT":
                importIdent(apu, frg);
                break;
            case "PT_REL":
                if (frg.getPid() == null) {
                    importRel(apu, frg);
                }
                break;
            case "PT_EVENT":
                importEvent(apu, frg, frgs);
                break;
            case "PT_CRE":
                importCre(apu, frg);
                break;
            case "PT_EXT":
                importExt(apu, frg);
                break;
            default:
                throw new IllegalStateException("AP with unsupported part, type: " + frg.getT());
            }
        }
        if(apu.getName()==null) {
            throw new IllegalStateException("AP without name: "+apUuid);
        }
        apu = null;

        return apusBuilder;
    }

    private void importEvent(Apu apu, Fragment frg, Fragments frgs) {
        String eventClass = ElzaXmlReader.getEnumValue(frg, "EV_TYPE");
        if(eventClass==null) {
            // TODO: warning - missing creation class
            return ;
        }
        Map<String, EdxItemConvertor> stringTypeMap = new HashMap<>();
        stringTypeMap.put("EV_BEGIN", new EdxUnitDateConvertor("EV_BEGIN"));
        stringTypeMap.put("EV_END", new EdxUnitDateConvertor("EV_END"));
        stringTypeMap.put("NOTE", new EdxStringConvertor("NOTE"));
        String typePart;
        switch(eventClass) {
        case "ET_MEMBERSHIP":
            typePart = "PT_AE_MEMBERSHIP";
            break;
        case "ET_MILESTONES":
            typePart = "PT_AE_MILESTONES";
            break;
        case "ET_MARRIAGE":
            typePart = "PT_AE_MARRIAGE";
            break;
        case "ET_REASSIGN":
            typePart = "PT_AE_REASSIGN";
            break;
        case "ET_STUDY":
            typePart = "PT_AE_STUDY";
            break;
        case "ET_AVARD":
            typePart = "PT_AE_AWARD";
            break;
        case "ET_HOBBY":
            typePart = "PT_AE_HOBBY";
            break;
        case "ET_JOB":
            typePart = "PT_AE_JOB";
            break;
        case "ET_AWARD":
            typePart = "PT_AE_AWARD";
            break;
        default:
            throw new IllegalStateException("Unknown event class: " + eventClass);
        }

        activePart = ApuSourceBuilder.addPart(apu, typePart);

        for(var item : frg.getDdOrDoOrDp()) {
            EdxItemConvertor convertor = stringTypeMap.get(item.getT());
            if(convertor==null) {
                // TODO: add warning
                continue;
            }
            convertor.convert(this, item);
        }

        // add subordinate elements
        for(Fragment frgt: frgs.getFrg()) {
            if(frgt.getT().equals("PT_REL") && Objects.equal(frg.getFid(), frgt.getPid())) {
                addApuRef(activePart, frgt);
            }
        }

        // remove empty part
        if(activePart.getItms()==null||activePart.getItms().getStrOrLnkOrEnm().size()==0) {
            apu.getPrts().getPart().remove(activePart);
        }

        activePart = null;
    }

    private void importCre(Apu apu, Fragment frg) {
        String creClass = ElzaXmlReader.getEnumValue(frg, ElzaTypes.CRE_CLASS);
        if(creClass==null) {
            // TODO: warning - missing creation class
            return ;
        }
        Map<String, EdxItemConvertor> stringTypeMap = new HashMap<>();
        stringTypeMap.put("NOTE",new EdxStringConvertor(CoreTypes.NOTE));
        stringTypeMap.put(ElzaTypes.CRE_TYPE,new EdxEnumConvertor(CoreTypes.CRE_TYPE, ElzaTypes.creTypeMap));
        switch(creClass) {
        case "CRC_BIRTH":
            stringTypeMap.put(ElzaTypes.CRE_DATE, new EdxUnitDateConvertor(CoreTypes.CRC_BIRTH_DATE));
            break;
        case "CRC_RISE":
            stringTypeMap.put(ElzaTypes.CRE_DATE, new EdxUnitDateConvertor(CoreTypes.CRC_RISE_DATE));
            break;
        case "CRC_BEGINSCOPE":
            stringTypeMap.put(ElzaTypes.CRE_DATE, new EdxUnitDateConvertor(CoreTypes.CRC_BEGINSCOPE_DATE));
            break;
        case "CRC_FIRSTMBIRTH":
            stringTypeMap.put(ElzaTypes.CRE_DATE, new EdxUnitDateConvertor(CoreTypes.CRC_FIRSTMBIRTH_DATE));
            break;
        case "CRC_FIRSTWMENTION":
            stringTypeMap.put(ElzaTypes.CRE_DATE, new EdxUnitDateConvertor(CoreTypes.CRC_FIRSTWMENTION_DATE));
            break;
        case "CRC_ORIGIN":
            stringTypeMap.put(ElzaTypes.CRE_DATE, new EdxUnitDateConvertor(CoreTypes.CRC_ORIGIN_DATE));
            break;
        case "CRC_BEGINVALIDNESS":
            stringTypeMap.put(ElzaTypes.CRE_DATE, new EdxUnitDateConvertor(CoreTypes.CRC_BEGINVALIDNESS_DATE));
            break;
        default:
            throw new IllegalStateException("Unknown creation class: "+creClass);
        }
        
        // PT_AE_CRE
        activePart = this.apusBuilder.addPart(apu, "PT_AE_CRE");
        
        for(var item: frg.getDdOrDoOrDp()) {
            EdxItemConvertor convertor = stringTypeMap.get(item.getT());
            if(convertor==null) {
                // TODO: add warning
                continue;
            }
            convertor.convert(this, item);
        }
        // remove empty part
        if(activePart.getItms()==null||activePart.getItms().getStrOrLnkOrEnm().size()==0) {
            apu.getPrts().getPart().remove(activePart);
        }
        
        activePart = null;
    }

	private void importExt(Apu apu, Fragment frg) {
        String extClass = ElzaXmlReader.getEnumValue(frg, ElzaTypes.EXT_CLASS);
        if(extClass==null) {
            // TODO: warning - missing creation class
            return ;
        }
        Map<String, EdxItemConvertor> stringTypeMap = new HashMap<>();
        stringTypeMap.put("NOTE",new EdxStringConvertor(CoreTypes.NOTE));
        stringTypeMap.put(ElzaTypes.EXT_TYPE,new EdxEnumConvertor(CoreTypes.EXT_TYPE, ElzaTypes.extTypeMap));
        switch(extClass) {
        case "EXC_DEATH":
            stringTypeMap.put(ElzaTypes.EXT_DATE, new EdxUnitDateConvertor("EXC_DEATH"));
            break;
        case "EXC_EXTINCTION":
            stringTypeMap.put(ElzaTypes.EXT_DATE, new EdxUnitDateConvertor("EXC_EXTINCTION"));
            break;
        case "EXC_ENDSCOPE":
            stringTypeMap.put(ElzaTypes.EXT_DATE, new EdxUnitDateConvertor("EXC_ENDSCOPE"));
            break;
        case "EXC_LASTMDEATH":
            stringTypeMap.put(ElzaTypes.EXT_DATE, new EdxUnitDateConvertor("EXC_LASTMDEATH"));
            break;
        case "EXC_LASTWMENTION":
            stringTypeMap.put(ElzaTypes.EXT_DATE, new EdxUnitDateConvertor("EXC_LASTWMENTION"));
            break;
        case "EXC_END":
            stringTypeMap.put(ElzaTypes.EXT_DATE, new EdxUnitDateConvertor("EXC_END"));
            break;
        case "EXC_ENDVALIDNESS":
            stringTypeMap.put(ElzaTypes.EXT_DATE, new EdxUnitDateConvertor("EXC_ENDVALIDNESS"));
            break;
        default:            
            throw new IllegalStateException("Unknown extinction class: "+extClass);        
        }

        // PT_AE_EXT
        activePart = ApuSourceBuilder.addPart(apu, CoreTypes.PT_AE_EXT);
        
        for(var item: frg.getDdOrDoOrDp()) {
            EdxItemConvertor convertor = stringTypeMap.get(item.getT());
            if(convertor==null) {
                // TODO: add warning
                continue;
            }
            convertor.convert(this, item);
        }
        // remove empty part
        if(activePart.getItms()==null||activePart.getItms().getStrOrLnkOrEnm().size()==0) {
            apu.getPrts().getPart().remove(activePart);
        }
        
        activePart = null;
    }

    private void addApuRef(Part part, Fragment frg) {
        DescriptionItemAPRef apRef = ElzaXmlReader.getApRef(frg, ElzaTypes.REL_ENTITY);
        if(apRef==null) {
            return;
        }
        String relType = ElzaTypes.relEntityMap.get(apRef.getS());
        if(relType==null) {
            throw new IllegalStateException("Unrecognized relation type: "+apRef.getS());
        }
        Integer apElzaId = Integer.valueOf(apRef.getApid());

        // Set parent relation for taxonomical categories
        if(apRef.getS().equals("RT_CATEGORY")) {
            Validate.isTrue(this.parentElzaId==null);
            parentElzaId = apElzaId;
        }

        var archEntityInfo = this.dataProvider.getArchivalEntityWithParentsByElzaId(apElzaId);
        if(CollectionUtils.isEmpty(archEntityInfo)) {
            this.requiredEntities.add(apElzaId);
            return;
        }
        var apUuids = archEntityInfo.stream().map(i -> i.getUuid()).collect(Collectors.toList());

        this.apusBuilder.addApuRefsFirstVisible(part, relType, apUuids);
    }

    private void importRel(Apu apu, Fragment frg) {
        DescriptionItemAPRef apRef = ElzaXmlReader.getApRef(frg, ElzaTypes.REL_ENTITY);
        if(apRef==null) {
            return;
        }
        String relType = ElzaTypes.relEntityMap.get(apRef.getS());
        if(relType==null) {
            throw new IllegalStateException("Unrecognized relation type: "+apRef.getS());
        }
        Integer apElzaId = Integer.valueOf(apRef.getApid());

        // Set parent relation for taxonomical categories
        if(apRef.getS().equals("RT_CATEGORY")&&"TERM_TAXONOMY".equals(entityClass)) {
            Validate.isTrue(this.parentElzaId==null);
            parentElzaId = apElzaId;
        }

        var archEntityInfo = this.dataProvider.getArchivalEntityWithParentsByElzaId(apElzaId);
        if(CollectionUtils.isEmpty(archEntityInfo)) {
            this.requiredEntities.add(apElzaId);
            return;
        }
        var apUuids = archEntityInfo.stream().map(i -> i.getUuid()).collect(Collectors.toList());

        // entity exists -> we can create link
        Part part = this.apusBuilder.addPart(apu, "PT_AE_REL");
        this.apusBuilder.addApuRefsFirstVisible(part, relType, apUuids);
    }

    private void importIdent(Apu apu, Fragment frg) {
        String identValue = ElzaXmlReader.getStringType(frg, ElzaTypes.IDN_VALUE);
        if(StringUtils.isEmpty(identValue)) {
            // skip empty idents
            return;
        }
        String identType = ElzaXmlReader.getEnumValue(frg, ElzaTypes.IDN_TYPE);
        if(identType==null) {
            // skip ident without type
            return;
        }
        switch(identType) {
        case "ISO3166_2":
        case "ISO3166_3":
        case "ISO3166_NUM":
        case "ISO3166_PART2":
        case "CZ_RETRO":
        case "TAXONOMY":
        case "ORCID":
        case "PEVA":
        case "RUIAN":
        case "ARCHNUM":
        case "CLO_ID":
        case "VIAF":
        case "LCNAF":
            // ignored idents
            return;
        case "IC":        
            identType = "IČ";
            break;
        case "VAT":        
            identType = "DIČ";
            break;            
        case "INTERPI":
            identType = "INTERPI";
            break;
        case "NUTSLAU":
            identType = "NUTS/LAU";
            break;
        case "ORCID_ID":
        case "RODNE_CISLO":
        case "WHOIS_ID_OSOBA":
        case "WHOIS_CISLO_UK":
            // processed idents
            break;
        case "AUT":
        	identType = "Autority NKP";
        	break;
        default:
            throw new IllegalStateException("Unrecognized identifier: "+identType + ", value: "+identValue);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(identType).append(": ").append(identValue);

        // add ident
        Part part = apusBuilder.addPart(apu, "PT_IDENT");
        part.setValue(sb.toString());
        apusBuilder.addEnum(part, "AE_IDENT_TYPE", identType, true);
        apusBuilder.addString(part, "AE_IDENT_VALUE", identValue);
    }

    private void importBody(Apu apu, Part part, Fragment frg) {
		String briefDesc = ElzaXmlReader.getStringType(frg, ElzaTypes.BRIEF_DESC);
		if(StringUtils.isNotEmpty(briefDesc)) {
			apu.setDesc(briefDesc);
		}

		String adminPrntRefId = ElzaXmlReader.getApRefId(frg, ElzaTypes.GEO_ADMIN_CLASS);
		if(StringUtils.isNotEmpty(adminPrntRefId)) {
			Validate.isTrue(this.parentElzaId == null);

			parentElzaId = Integer.valueOf(adminPrntRefId);

			var parentEntityInfo = this.dataProvider.getArchivalEntityWithParentsByElzaId(parentElzaId);
			if(CollectionUtils.isEmpty(parentEntityInfo)) {
			    this.requiredEntities.add(parentElzaId);
			} else {
		        var parentEntUuid = parentEntityInfo.stream().map(i -> i.getUuid()).collect(Collectors.toList());
			    this.apusBuilder.addApuRefsFirstVisible(part, "AE_GEO_ADMIN_REF", parentEntUuid);
			}
		}

		// primy prenos
		addStringIfExists(frg, ElzaTypes.CORP_PURPOSE, part, CoreTypes.CORP_PURPOSE);
		addStringIfExists(frg, ElzaTypes.FOUNDING_NORMS, part, CoreTypes.FOUNDING_NORMS);
	    addStringIfExists(frg, ElzaTypes.SCOPE_NORMS, part, CoreTypes.SCOPE_NORMS);
	    addStringIfExists(frg, ElzaTypes.CORP_STRUCTURE, part, CoreTypes.CORP_STRUCTURE);
	    addStringIfExists(frg, ElzaTypes.SOURCE_INFO, part, CoreTypes.SOURCE_INFO);
		addStringIfExists(frg, ElzaTypes.HISTORY, part, CoreTypes.HISTORY);
	    addStringIfExists(frg, ElzaTypes.GENEALOGY, part, CoreTypes.GENEALOGY);
	    addStringIfExists(frg, ElzaTypes.BIOGRAPHY, part, CoreTypes.BIOGRAPHY);
	    addStringIfExists(frg, ElzaTypes.DESCRIPTION, part, CoreTypes.DESCRIPTION);
	    addLinkIfExists(frg, ElzaTypes.SOURCE_LINK, part, CoreTypes.SOURCE_LINK);
	    
	    // TODO: Add warning for all non processed elements
	}

	private void addLinkIfExists(Fragment frg, String srcType, Part part, String trgType) {
	    DescriptionItemUriRef item = ElzaXmlReader.getLink(frg, srcType);
        if(item!=null) {
            ApuSourceBuilder.addLink(part, trgType, item.getUri(), item.getLbl());
        }        
    }

    private void addStringIfExists(Fragment frg, String srcType, Part part, String trgType) {
        String value = ElzaXmlReader.getStringType(frg, srcType);
        if(StringUtils.isNotBlank(value)) {
            ApuSourceBuilder.addString(part, trgType, value);
        }
    }

    private void importName(Apu apu, Fragment frg) {
        String fullName = nameBuilder.createFullName(frg, entityClass, false);
        // add name
        if (apu.getName() == null) {
            apu.setName(fullName);
        }
        apusBuilder.addName(apu, fullName);
    }

	public Integer getParentElzaId() {
		return parentElzaId;
	}

	public Integer getElzaId() {
		return elzaId;
	}

    @Override
    public ApuSourceBuilder getApusBuilder() {
        return apusBuilder;
    }

    @Override
    public Part getActivePart() {
        return activePart;
    }

    @Override
    public ElzaXmlReader getElzaXmlReader() {
        return this.elzaXmlReader;
    }

    @Override
    public Apu getActiveApu() {
        return apu;
    }
    
    @Override
	public Level getProcessedLevel() {
		return null;
	}

	@Override
    public void addArchEntityRef(ArchEntityInfo aei) {        
    }		
    
    @Override
	public boolean isArchEntityReferenced(UUID uuid) {
		return false;
	}

	/**
     * Vyhodit tuto vyjimku pokud dojde k chybe kterou 
     */
    public static class MarkAsNonAvailableException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		
		public MarkAsNonAvailableException(String message) {
			super(message);
		}

    }
    
    
}
