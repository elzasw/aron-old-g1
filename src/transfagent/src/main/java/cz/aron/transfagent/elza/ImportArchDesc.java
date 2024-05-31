package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.ItemEnum;
import cz.aron.apux._2020.Part;
import cz.aron.common.itemtypes.TypesConfiguration;
import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.config.ConfigElzaInheritedType;
import cz.aron.transfagent.elza.convertor.EdxAmountConvertor;
import cz.aron.transfagent.elza.convertor.EdxApRefConvertor;
import cz.aron.transfagent.elza.convertor.EdxApRefWithRole;
import cz.aron.transfagent.elza.convertor.EdxAttachment;
import cz.aron.transfagent.elza.convertor.EdxEnumConvertor;
import cz.aron.transfagent.elza.convertor.EdxIntConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemCovertContext;
import cz.aron.transfagent.elza.convertor.EdxLinkConvertor;
import cz.aron.transfagent.elza.convertor.EdxNullConvertor;
import cz.aron.transfagent.elza.convertor.EdxSizeConvertor;
import cz.aron.transfagent.elza.convertor.EdxStorageConvertor;
import cz.aron.transfagent.elza.convertor.EdxStringConvertor;
import cz.aron.transfagent.elza.convertor.EdxStringSpecConvertor;
import cz.aron.transfagent.elza.convertor.EdxTimeLenghtConvertor;
import cz.aron.transfagent.elza.convertor.EdxUnitDateConvertor;
import cz.aron.transfagent.elza.convertor.EdxUnitDateConvertorEnum;
import cz.aron.transfagent.elza.convertor.UnitDateConvertor;
import cz.aron.transfagent.elza.dao.ArchDescLevelDaoImporter;
import cz.aron.transfagent.elza.datace.ItemDateRangeAppender;
import cz.aron.transfagent.peva.ArchiveFundId;
import cz.aron.transfagent.peva.ImportPevaGeo;
import cz.aron.transfagent.service.DaoFileStoreService;
import cz.aron.transfagent.service.LevelEnrichmentService;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.DigitalArchivalObject;
import cz.tacr.elza.schema.v2.DigitalArchivalObjects;
import cz.tacr.elza.schema.v2.File;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Levels;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.Sections;

public class ImportArchDesc implements EdxItemCovertContext {

    private final static Logger log = LoggerFactory.getLogger(ImportArchDesc.class);

	ElzaXmlReader elzaXmlReader;

	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	ContextDataProvider dataProvider;
	
	private final Map<String, LevelContext> apuContexts = new LinkedHashMap<>();

	private final Map<UUID, ArchEntityInfo> apRefs = new HashMap<>();
	private final Map<UUID, ArchEntityInfo> apLevelRefs = new HashMap<>();
	
	/**
	 * Mapa (source,daos)
	 */
	private final Map<String,Set<ArchDescDaoRef>> daoRefs = new HashMap<>();		

	private UUID instApuUuid;
	private UUID fundApuUuid;

	private Part activePart;

	private String institutionCode;

	private String institutionName;
	
	private String institutionShortName;
	
	private Integer fundId;

	private Apu activeApu;
	
	private Level processedLevel;

    static final String ignoredTypes[] = {"ZP2015_ARRANGEMENT_TYPE", "ZP2015_LEVEL_TYPE",
            "ZP2015_SERIAL_NUMBER", "ZP2015_NAD", "ZP2015_ZNACKA_FONDU",
            "ZP2015_LEVEL_TYPE", "ZP2015_ARRANGER",
            "ZP2015_UNIT_DATE_BULK","ZP2015_FOLDER_TYPE",
            "ZP2015_ITEM_ORDER", // processed with ZP2015_STORAGE_ID
            "ZP2015_UNIT_COUNT_ITEM",
            "ZP2015_INTERNAL_NOTE",
            "ZP2015_AIP_ID",
            "ZP2015_RESTRICTION_ACCESS_SHARED",
            "ZP2015_RESTRICTION_ACCESS_NAME",
            "ZP2015_RESTRICTED_ACCESS_REASON",
            "ZP2015_RESTRICTED_ACCESS_TYPE",
            "ZP2015_RESTRICTION_ACCESS_INLINE",
            // geo souradnice - neumime prevest
            ElzaTypes.ZP2015_POSITION,
            "ZP2015_DAO_ID",
            "ZP2015_CONNECTED_RECORD",
            "ZP2015_INVALID_RECORD",
            "ZP2015_TITLE_PUBLIC",
            "ZP2015_UNIT_DAMAGE_TYPE",
            "ZP2015_MAJOR_LANG",
            ElzaTypes.ZP2015_APPLIED_RESTRICTION,
            ElzaTypes.ZP2015_APPLIED_RESTRICTION_CHANGE,

    };

    /**
     * Map of item convertors
     */
    Map<String, EdxItemConvertor> stringTypeMap;
    
    Map<String, EdxItemConvertor> multiTypeMap;

    private final ApTypeService apTypeService;
    
    private final DaoFileStoreService daoFileStoreService;
    
    private final LevelEnrichmentService levelEnrichmentService;
    
    private final List<ArchDescLevelDaoImporter> levelDaoImporters;
    
    private final ConfigElzaArchDesc configArchDesc;
    
    private final List<String> attachmentIds = new ArrayList<>();
    
    private final List<ArchDescAttachment> attachments = new ArrayList<>();
    
    private final TypesConfiguration typesConfig;

    public ImportArchDesc(ApTypeService apTypeService, DaoFileStoreService daoFileStoreService,
                          LevelEnrichmentService levelEnrichmentService,
                          ConfigElzaArchDesc configArchDesc, List<ArchDescLevelDaoImporter> levelDaoImporters,
                          TypesConfiguration typesConfig) {
        this.apTypeService = apTypeService;
        this.daoFileStoreService = daoFileStoreService;
        this.levelEnrichmentService = levelEnrichmentService;
        this.configArchDesc = configArchDesc;
        this.levelDaoImporters = levelDaoImporters;
        this.typesConfig = typesConfig;
    }

    public Set<UUID> getApRefs() {
        return apRefs.keySet();
    }

    public String getInstitutionCode() {
        return institutionCode;
    }
    
    public Integer getFundId() {
    	return fundId;
    }
    
    public Map<String,Set<ArchDescDaoRef>> getDaoRefs() {
    	return daoRefs;
    }
    
    public List<ArchDescAttachment> getAttachments() {
        return attachments;
    }

	public static void main(String[] args) {
		Path inputFile = Path.of(args[0]);
		ImportArchDesc iad = new ImportArchDesc(new ApTypeService(), null, null, new ConfigElzaArchDesc(), Collections.emptyList(),null);
		try {
			ApuSourceBuilder apusrcBuilder = iad.importArchDesc(inputFile, args[1]);
			Path outputPath = Path.of(args[2]);
			try(OutputStream fos = Files.newOutputStream(outputPath)) {
				apusrcBuilder.build(fos);
			}
	        System.out.println(args[2] + " is done.");
		} catch(Exception e) {
			System.err.println("Failed to process input file: " + inputFile);
			e.printStackTrace();
		}
	}

	public ApuSourceBuilder importArchDesc(Path inputFile, String propFile) throws IOException, JAXBException {
		Validate.isTrue(propFile!=null&&propFile.length()>0);

		PropertiesDataProvider pdp = new PropertiesDataProvider();
		Path propPath = Paths.get(propFile);
		pdp.load(propPath);
		return importArchDesc(inputFile, pdp);
	}

    public ApuSourceBuilder importArchDesc(Path inputFile, final ContextDataProvider cdp) throws IOException, JAXBException {
        this.dataProvider = cdp;

        try(InputStream is = Files.newInputStream(inputFile)) {
            elzaXmlReader = ElzaXmlReader.read(is);
            return importArchDesc(inputFile);
        }
    }

    private ApuSourceBuilder importArchDesc(Path inputFile) {
    	initConvertor();
    	initMultiItemConvertor();
        Sections sections = elzaXmlReader.getEdx().getFs();
        if(sections==null||sections.getS().size()==0) {
            throw new RuntimeException("Missing section data");
        }
        if(sections.getS().size()>1) {
            throw new RuntimeException("Exports with one section are supported");
        }
        Section sect = sections.getS().get(0);
        if(sect.getLvls()==null) {
            throw new RuntimeException("Missing levels.");
        }

        // read fund info
        FundInfo fi = sect.getFi();
        institutionCode = fi.getIc();
        var institutionInfo = dataProvider.getInstitutionApu(institutionCode);
        instApuUuid = institutionInfo.getUuid();
        institutionName = institutionInfo.getName();
        institutionShortName = institutionInfo.getShortName();
        Validate.notNull(instApuUuid, "Missing institution, code: %s", institutionCode);

		if (fi.getC() != null) {
			fundApuUuid = dataProvider.getFundApu(institutionCode, fi.getC());
			if (fi.getNum()!=null) {
				fundId = fi.getNum().intValue();
			}
		}
		if (fundApuUuid==null&&fi.getNum() != null) {
			fundId = fi.getNum().intValue();
			fundApuUuid = dataProvider.getFundApu(institutionCode,
					ArchiveFundId.createJaFaId(institutionCode, fi.getNum().toString(), null));
		}
		Validate.notNull(fundApuUuid, "Missing fund, code: %s, institution: %s,%s", fi.getC(), fi.getNum(),
				institutionCode);

		Levels lvls = sect.getLvls();
		var levelDuplicities = computeLevelDuplicities(lvls);        
        for(Level lvl: lvls.getLvl()) {
            processLevel(sect, lvl, levelDuplicities);
        }

        // add dates to siblings
        for (Entry<String, LevelContext> item : apuContexts.entrySet()) {
            var context = item.getValue(); 
            var apu = context.getApu();
            var ranges = ApuSourceBuilder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
            if (ranges.isEmpty()) {
                copyDateRangeFromParent(context, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
            }
        }        

        if (configArchDesc.isImportAttachments()) {
            Path dir = inputFile.getParent();
            if (sect.getFs() != null) {
            	int pos = 1;
                for (File f : sect.getFs().getF()) {
                    if (attachmentIds.contains(f.getId())) {
                        var file = dir.resolve(f.getFn());
                        try {
                            Files.write(file, f.getD());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        apusBuilder.addAttachment(apusBuilder.getMainApu(), f.getN(), f.getMt(), pos++);
                        attachments.add(new ArchDescAttachment(file));
                    }
                }
            }
        }
        return apusBuilder;
    }
    
	private Map<String, Set<String>> computeLevelDuplicities(Levels lvls) {

		var tmp = new HashMap<String, Map<String, Integer>>();
		if (configArchDesc.isUniqueLevels()) {
			for (Level lvl : lvls.getLvl()) {
				LevelContext parentContext = null;
				if (lvl.getPid() != null) {
					parentContext = apuContexts.get(lvl.getPid());
					if (parentContext == null) {
						throw new RuntimeException("Missing parent for level: " + lvl.getPid());
					}
				}
				var context = new LevelContext(lvl, null, parentContext);
				apuContexts.put(lvl.getId(), context);

				var desc = getDesc(lvl, parentContext, null);
				tmp.compute(lvl.getPid(), (k, v) -> {
					if (v != null) {
						v.compute(desc, (k1, v1) -> {
							if (v1 == null) {
								return 1;
							} else {
								return v1 + 1;
							}
						});
						return v;
					} else {
						var x = new HashMap<String, Integer>();
						x.put(desc, 1);
						return x;
					}
				});

			}
			apuContexts.clear();
		}

		var ret = new HashMap<String, Set<String>>();
		for (var entry : tmp.entrySet()) {
			var levelDescs = new HashSet<String>();
			for (var levelDescEntry : entry.getValue().entrySet()) {
				if (levelDescEntry.getValue() > 1) {
					levelDescs.add(levelDescEntry.getKey());
				}
			}
			if (!levelDescs.isEmpty()) {
				ret.put(entry.getKey(), levelDescs);
			}
		}
		return ret;
	}
    
    public static class ArchDescAttachment {
        
        private final Path path;
        
        public ArchDescAttachment(Path path) {
            this.path = path;
        }
        
        public Path getPath() {
            return path;
        }
    }

    private void processLevel(Section sect, Level lvl, Map<String,Set<String>> duplicities) {
        log.debug("Importing level, id: {}, u2uid: {}", lvl.getId(), lvl.getUuid());
        apLevelRefs.clear();
                
        Apu apu = apusBuilder.createApu("", ApuType.ARCH_DESC);
        apu.setUuid(lvl.getUuid());        
        
        // set parent
        LevelContext parentContext = null;
        Apu parentApu = null;
        if(lvl.getPid()!=null) {
            parentContext = apuContexts.get(lvl.getPid());                       
            if(parentContext==null) {
                throw new RuntimeException("Missing parent for level: "+lvl.getPid());
            }
            parentApu = parentContext.getApu();
            apu.setPrnt(parentApu.getUuid());
        }
        var levelContext = new LevelContext(lvl,apu,parentContext);
        apuContexts.put(lvl.getId(), levelContext);
        
        String name = getName(sect, lvl, parentContext);
        apu.setName(name);
        String desc = getDesc(lvl, parentContext, duplicities.get(lvl.getPid()));        
        apu.setDesc(desc);

        activateArchDescPart(apu);                

        //Set<ConfigElzaInheritedType> toInherit = new HashSet<>(configArchDesc.getInherited());
        Set<EdxItemConvertor> usedMultiConvertors = new HashSet<>();
        // add items
        processedLevel = lvl;
        for(DescriptionItem item: lvl.getDdOrDoOrDp()) {
            addItem(apu, item, usedMultiConvertors, levelContext);
        }
		if (parentContext != null) {
			for (var type : configArchDesc.getInherited()) {
				parentContext.inheritValue(type);
			}
		}
        processedLevel = null;
        
        
        processLevelEnrichment(lvl);

		// daos
		boolean daoExist = false;
		DigitalArchivalObjects daos = lvl.getDaos();
		if (daos != null && daos.getDao().size() > 0) {
			for (DigitalArchivalObject dao : daos.getDao()) {
				var daoHandle = dao.getDoid();
				addDaoRef("dspace", null, daoHandle);
				UUID daoUuid = dataProvider.getDao(daoHandle);
				if (daoUuid != null) {
					ApuSourceBuilder.addDao(activeApu, daoUuid);
					daoExist = true;
				}
			}
		}
		
		daoExist = daoExist || processExternalDaos(lvl);
        
        if(daoExist) {
            ApuSourceBuilder.addEnum(activePart, "DIGITAL", "Ano", false);
        }

        // copy values from parent
        if(parentApu!=null) {
            List<ItemEnum> itemEnums = ApuSourceBuilder.getItemEnums(parentApu, ApuType.ARCH_DESC, CoreTypes.UNIT_TYPE);
            if(CollectionUtils.isNotEmpty(itemEnums)) {
            	ApuSourceBuilder.copyEnums(activePart, itemEnums);
            }
        }

        deactivatePart(apu);

        // add static values
        activatePart(apu, CoreTypes.PT_ARCH_DESC_FUND);
        addEntityClasses(activePart);
        apusBuilder.addApuRef(activePart, "FUND_REF", fundApuUuid);
        apusBuilder.addApuRef(activePart, "FUND_INST_REF", instApuUuid);
        deactivatePart(apu);

        // add date to parent(s)
        var itemDateRanges = ApuSourceBuilder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        for (ItemDateRange item : itemDateRanges) {
            var context = parentContext;
            while (context != null) {
                var apuParent = context.getApu();
                ItemDateRangeAppender dateRangeAppender = new ItemDateRangeAppender(item);
                dateRangeAppender.appendTo(apuParent);
                context = context.getParentContext();
            }
        }
    }
    
	private boolean processExternalDaos(Level lvl) {
		boolean daoExist = false;
		if (daoFileStoreService != null) {
			if (daoFileStoreService.getDaoDir(lvl.getUuid()) != null) {
				addDaoRef(daoFileStoreService.getName(), UUID.fromString(lvl.getUuid()), lvl.getUuid());
				ApuSourceBuilder.addDao(activeApu, UUID.fromString(lvl.getUuid()));
				daoExist = true;
			}
		}
		
		if (fundId != null) {
			// TODO fond z elzy nemusi mit fundId, rozsirit i na moznost pouzit kod fondu
			for (ArchDescLevelDaoImporter daoImporter : levelDaoImporters) {
				if (daoImporter.importDaos(institutionCode, fundId, lvl, activeApu, dataProvider,
						(source, handle, uuid) -> {
							addDaoRef(source, uuid, handle);
						}) > 0) {
					daoExist = true;
				}
			}
		}
        return daoExist;
	}
	
    private void processLevelEnrichment(Level lvl) {
        if (levelEnrichmentService != null) {
            String url = levelEnrichmentService.getUrlForLevel(lvl.getUuid());
            if (url != null) {
                ApuSourceBuilder.addLink(activePart, "URL", url, levelEnrichmentService.getLabel());
            }
        }
    }
	
	private void addDaoRef(String source, UUID uuid, String handle) {
		Set<ArchDescDaoRef> sourceDaoRefs = daoRefs.get(source);
		if (sourceDaoRefs == null) {
			sourceDaoRefs = new HashSet<>();
			daoRefs.put(source, sourceDaoRefs);
		}
		sourceDaoRefs.add(new ArchDescDaoRef(handle, uuid));
	}
	
	
	private void initMultiItemConvertor() {
		multiTypeMap = new HashMap<>();
		var sizeConvertor = new EdxSizeConvertor();		
		multiTypeMap.put(ElzaTypes.ZP2015_SIZE_WIDTH, sizeConvertor);
        multiTypeMap.put(ElzaTypes.ZP2015_SIZE_HEIGHT, sizeConvertor);
        multiTypeMap.put(ElzaTypes.ZP2015_SIZE_DEPTH, sizeConvertor);
        multiTypeMap.put(ElzaTypes.ZP2015_SIZE_UNITS, sizeConvertor);        
	}

    private void initConvertor() {    	
    	ElzaNameBuilder nameBuilder = new ElzaNameBuilder(apTypeService);    	
	    stringTypeMap = new HashMap<>();
        // TITLE is used as default APU name, it is not converted as separate ABSTRACT
        //stringTypeMap.put("ZP2015_TITLE",new EdxStringConvertor("ABSTRACT"));     
        stringTypeMap.put("ZP2015_TITLE",new EdxNullConvertor());
        stringTypeMap.put("ZP2015_UNIT_TYPE", new EdxEnumConvertor(CoreTypes.UNIT_TYPE, ElzaTypes.unitTypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_EXTRA_UNITS, new EdxEnumConvertor(CoreTypes.UNIT_TYPE, ElzaTypes.extraUnitTypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_UNIT_SUBTYPE, new EdxEnumConvertor(CoreTypes.UNIT_TYPE, ElzaTypes.subtypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_RECORD_TYPE, new EdxEnumConvertor(CoreTypes.RECORD_TYPE, ElzaTypes.recordTypeMap));      
        stringTypeMap.put("ZP2015_UNIT_ID",new EdxStringConvertor("UNIT_ID").addIndexedItem(CoreTypes.UNIT_ID_INDEX));
		stringTypeMap.put(ElzaTypes.ZP2015_STORAGE_ID, new EdxStorageConvertor(CoreTypes.STORAGE_ID,
				configArchDesc.isStorage() ? CoreTypes.STORAGE : null, elzaXmlReader.getSoMap()));
        stringTypeMap.put("ZP2015_UNIT_HIST",new EdxStringConvertor("HISTORY"));
        stringTypeMap.put("ZP2015_UNIT_ARR",new EdxStringConvertor("UNIT_ARR"));
        stringTypeMap.put("ZP2015_UNIT_CONTENT",new EdxStringConvertor("UNIT_CONTENT"));
        stringTypeMap.put("ZP2015_UNIT_SOURCE",new EdxStringConvertor("UNIT_SOURCE"));
        stringTypeMap.put("ZP2015_FUTURE_UNITS",new EdxStringConvertor("FUTURE_UNITS"));
        stringTypeMap.put("ZP2015_UNIT_ACCESS",new EdxStringConvertor("UNIT_ACCESS"));
        stringTypeMap.put("ZP2015_UNIT_CURRENT_STATUS",new EdxStringConvertor("UNIT_CURRENT_STATUS"));
        stringTypeMap.put("ZP2015_ARRANGE_RULES",new EdxStringConvertor("ARRANGE_RULES"));
		stringTypeMap.put(ElzaTypes.ZP2015_ORIGINATOR, new EdxApRefConvertor(CoreTypes.ORIGINATOR_REF, dataProvider,
				configArchDesc.isProcessInternalSupplement(), nameBuilder, typesConfig));
		stringTypeMap.put("ZP2015_AP_REF", new EdxApRefConvertor("AP_REF", dataProvider,
				configArchDesc.isProcessInternalSupplement(), nameBuilder, typesConfig));
		stringTypeMap.put("ZP2015_ITEM_TITLE_REF", new EdxApRefConvertor("ITEM_TITLE", dataProvider,
				configArchDesc.isProcessInternalSupplement(), nameBuilder, typesConfig));
        stringTypeMap.put("ZP2015_FORMAL_TITLE",new EdxStringConvertor("FORMAL_TITLE"));
        stringTypeMap.put("ZP2015_SCALE",new EdxStringConvertor("SCALE"));
        stringTypeMap.put(ElzaTypes.ZP2015_LANGUAGE, new EdxEnumConvertor(CoreTypes.LANGUAGE, ElzaTypes.languageTypeMap));
        stringTypeMap.put(ElzaTypes.ZP2015_ORIENTATION, new EdxStringConvertor(CoreTypes.ORIENTATION));
        stringTypeMap.put(ElzaTypes.ZP2015_ITEM_MAT, new EdxStringConvertor(CoreTypes.ITEM_MAT));
        stringTypeMap.put(ElzaTypes.ZP2015_PART, new EdxStringConvertor(CoreTypes.PART));
        stringTypeMap.put("ZP2015_STORAGE_COND",new EdxStringConvertor("STORAGE_COND"));
        stringTypeMap.put("ZP2015_RELATED_UNITS",new EdxStringConvertor("RELATED_UNITS"));
        stringTypeMap.put(ElzaTypes.ZP2015_UNIT_DATE,new EdxUnitDateConvertor(CoreTypes.UNIT_DATE));
        stringTypeMap.put(ElzaTypes.ZP2015_DATE_OTHER,new EdxUnitDateConvertorEnum(ElzaTypes.dateOtherMap, ElzaTypes.dateOtherMapIndex));
        stringTypeMap.put("ZP2015_SIZE",new EdxStringConvertor("SIZE"));
        stringTypeMap.put("ZP2015_ITEM_MAT",new EdxStringConvertor("ITEM_MAT"));
        stringTypeMap.put("ZP2015_INV_CISLO",new EdxStringConvertor("INV_CISLO").addIndexedItem("OTHER_ID_PROC_INDEX"));
        stringTypeMap.put(ElzaTypes.ZP2015_OTHER_ID, new EdxStringSpecConvertor(ElzaTypes.otherIdMap).addIndexedItem(ElzaTypes.otherIdIndexMap));
        stringTypeMap.put("ZP2015_EDITION",new EdxStringConvertor("EDITION"));
        stringTypeMap.put("ZP2015_UNIT_DATE_TEXT",new EdxStringConvertor("UNIT_DATE_TEXT"));
        stringTypeMap.put("ZP2015_EXERQUE",new EdxStringConvertor("EXERQUE"));
        stringTypeMap.put("ZP2015_PAINTING_CHAR",new EdxStringConvertor("PAINTING_CHAR"));
        stringTypeMap.put(ElzaTypes.ZP2015_CORROBORATION, new EdxStringConvertor(CoreTypes.CORROBORATION));
        stringTypeMap.put(ElzaTypes.ZP2015_IMPRINT_COUNT, new EdxStringConvertor(CoreTypes.IMPRINT_COUNT));
        stringTypeMap.put(ElzaTypes.ZP2015_IMPRINT_ORDER, new EdxStringConvertor(CoreTypes.IMPRINT_ORDER));
        stringTypeMap.put(ElzaTypes.ZP2015_LEGEND, new EdxStringConvertor(CoreTypes.LEGEND));
        stringTypeMap.put(ElzaTypes.ZP2015_MOVIE_LENGTH, new EdxTimeLenghtConvertor(CoreTypes.MOVIE_LENGTH));
        stringTypeMap.put(ElzaTypes.ZP2015_RECORD_LENGTH, new EdxTimeLenghtConvertor(CoreTypes.RECORD_LENGTH));
        stringTypeMap.put(ElzaTypes.ZP2015_WRITING, new EdxStringConvertor(CoreTypes.WRITING));
        stringTypeMap.put(ElzaTypes.ZP2015_ITEM_LINK, new EdxLinkConvertor(CoreTypes.ARCH_DESC_REF, CoreTypes.SOURCE_LINK));
        stringTypeMap.put(ElzaTypes.ZP2015_DAO_LINK, new EdxLinkConvertor(CoreTypes.ARCH_DESC_REF, CoreTypes.DAO_LINK));
        stringTypeMap.put(ElzaTypes.ZP2015_RELATED_UNITS_LINK, new EdxLinkConvertor(CoreTypes.ARCH_DESC_REF, CoreTypes.RELATED_UNITS_LINK));

        stringTypeMap.put("ZP2015_EXISTING_COPY",new EdxStringConvertor("EXISTING_COPY"));
        stringTypeMap.put("ZP2015_ARRANGEMENT_INFO",new EdxStringConvertor("ARRANGEMENT_INFO"));
		stringTypeMap.put(ElzaTypes.ZP2015_ENTITY_ROLE,
				new EdxApRefWithRole(CoreTypes.PT_ENTITY_ROLE, dataProvider, ElzaTypes.roleSpecMap,
						configArchDesc.getApMappings(), configArchDesc.isProcessInternalSupplement(), nameBuilder,
						typesConfig));
        stringTypeMap.put("ZP2015_UNIT_COUNT",new EdxIntConvertor("UNIT_COUNT"));
        stringTypeMap.put("ZP2015_NOTE",new EdxStringConvertor(CoreTypes.NOTE));
        stringTypeMap.put("ZP2015_DESCRIPTION_DATE",new EdxStringConvertor("DESCRIPTION_DATE"));
        
        if (configArchDesc.isImportAttachments()) {
            stringTypeMap.put("ZP2015_ATTACHMENT", new EdxAttachment(attachmentIds));
        } else {
            stringTypeMap.put("ZP2015_ATTACHMENT", new EdxNullConvertor());
        }
        if (configArchDesc.isShowAccessRestrictions()) {
            stringTypeMap.put(ElzaTypes.ZP2015_APPLIED_RESTRICTION_TEXT, new EdxStringConvertor("UNIT_RESTRICTION_TEXT"));
        } else {
            stringTypeMap.put(ElzaTypes.ZP2015_APPLIED_RESTRICTION_TEXT, new EdxNullConvertor());
        }
        
        stringTypeMap.put(ElzaTypes.ZP2015_AMOUNT, new EdxAmountConvertor(ElzaTypes.AMOUNT_SUBTYPES));
    }

	private void addItem(Apu apu, DescriptionItem item, Set<EdxItemConvertor> usedMultiConvertors,
			LevelContext currentContext) {
		
		var typeSpec = new ConfigElzaInheritedType(item.getT(),item.getS());		
		if (item instanceof DescriptionItemUndefined) {
			return;
		}
		// check ignored items
		for (String ignoredType : ignoredTypes) {
			if (ignoredType.equals(item.getT())) {
				return;
			}
		}

		EdxItemConvertor convertor = stringTypeMap.get(item.getT());
		if (convertor != null) {
			convertor.convert(this, item);			
			if(configArchDesc.isInherited(typeSpec)) {
				currentContext.addInheritedConvertor(convertor, item, typeSpec);
			}
			return;
		}

		EdxItemConvertor multiConvertor = multiTypeMap.get(item.getT());
		if (multiConvertor != null) {
			if (usedMultiConvertors.contains(multiConvertor)) {
				return;
			} else {
				multiConvertor.convert(this, item);
				usedMultiConvertors.add(multiConvertor);
				return;
			}
		}
		throw new RuntimeException("Unsupported item type: " + item.getT());
	}

    private void addEntityClasses(Part part) {
        if(apLevelRefs.isEmpty()) {
            return;
        }
        
        Set<String> rootClasses = new HashSet<>();

        Map<String, String> rtMap = new HashMap<>();
        rtMap.put("PARTY_GROUP", "rejstřík korporací");
        rtMap.put("PERSON", "rejstřík osob, bytostí");
        rtMap.put("DYNASTY", "rejstřík rodů, rodin");
        rtMap.put("EVENT", "rejstřík událostí");
        rtMap.put("ARTWORK", "rejstřík děl");
        rtMap.put("GEO", "rejstřík zeměpisný");
        rtMap.put("TERM", "rejstřík obecných pojmů");

        Map<String, String> rctMap = new HashMap<>();
        rctMap.put("PARTY_GROUP", "REG_GROUP_PARTY_REF");
        rctMap.put("PERSON", "REG_PERSON_REF");
        rctMap.put("DYNASTY", "REG_DYNASTY_REF");
        rctMap.put("EVENT", "REG_EVENT_REF");
        rctMap.put("ARTWORK", "REG_ARTWORK_REF");
        rctMap.put("GEO", "REG_GEO_REF");
        rctMap.put("TERM", "REG_TERM_REF");

        for(var apr: apLevelRefs.values()) {
            // TODO proc se navaze na geo entitu z pevy?
            if (!ImportPevaGeo.ENTITY_CLASS.equals(apr.getEntityClass())) {
                var parentCode = apTypeService.getParentCode(apr.getEntityClass());
                Validate.notNull(parentCode, "Failed to get parent code for class: %s", apr.getEntityClass());

                String itemType = rctMap.get(parentCode);
                Validate.notNull(itemType, "Failed to get itemType for parentCode: %s", parentCode);
                // Add APref
                apusBuilder.addApuRef(part, itemType, apr.getUuid(), false);
                rootClasses.add(parentCode);                
            }            
        }

        // add rootClasses
        for (String rootCls : rootClasses) {
            var name = rtMap.get(rootCls);
            Validate.notNull(name, "Missing mapping for root class: %s", rootCls);
            ApuSourceBuilder.addEnum(part, "REGISTRY_TYPE", name, false);
        }
    }

    private void copyDateRangeFromParent(LevelContext levelContext, String partType, String itemType) {
        var parentContext = levelContext.getParentContext();
        var apu = levelContext.getApu();
        while (parentContext != null) {
            var apuParent = parentContext.getApu();
            var rangesParent = ApuSourceBuilder.getItemDateRanges(apuParent, partType, CoreTypes.UNIT_DATE);
            if (rangesParent.size() > 0) {
                Part part = ApuSourceBuilder.getFirstPart(apu, partType);
                if (part == null) {
                    part = ApuSourceBuilder.addPart(apu, partType);
                }
                ApuSourceBuilder.copyDateRanges(part, rangesParent);
                return;
            } else {
                parentContext = parentContext.getParentContext();
            }
        }
    }

    private void deactivatePart(Apu apu) {
        if(activePart!=null) {
            // delete empty part
            if(activePart.getItms()==null&&activePart.getValue()==null) {
               apu.getPrts().getPart().remove(activePart); 
            }
            activePart = null;
        }
    }

    /**
	 * Set PT_ARCH_DESC as active part
	 */
	void activateArchDescPart(Apu apu) {
	    activatePart(apu, CoreTypes.PT_ARCH_DESC);
	}

    void activatePart(Apu apu, String partName) {
        activeApu = apu;
        activePart = null;
        if(apu.getPrts()!=null) {
            for(Part p : apu.getPrts().getPart()) {
                if(p.getType().equals(partName)) {
                    activePart = p;
                    break;
                }
            }
        }
        if(activePart==null) {
            activePart = ApuSourceBuilder.addPart(apu, partName);
        }
    }

    public static Map<String, Integer> otherIdPriorityMap = new HashMap<>();
    static {
        otherIdPriorityMap.put("ZP2015_OTHERID_SIG_ORIG", 99);
        otherIdPriorityMap.put("ZP2015_OTHERID_SIG", 100);
        otherIdPriorityMap.put("ZP2015_OTHERID_STORAGE_ID", 96);
        otherIdPriorityMap.put("ZP2015_OTHERID_CJ", 95);
        otherIdPriorityMap.put("ZP2015_OTHERID_DOCID", 94);
        otherIdPriorityMap.put("ZP2015_OTHERID_FORMAL_DOCID", 92);
        otherIdPriorityMap.put("ZP2015_OTHERID_ADDID", 98);
        otherIdPriorityMap.put("ZP2015_OTHERID_OLDSIG", 83);
        otherIdPriorityMap.put("ZP2015_OTHERID_OLDSIG2", 93);
        otherIdPriorityMap.put("ZP2015_OTHERID_OLDID", 97);
        otherIdPriorityMap.put("ZP2015_OTHERID_INVALID_UNITID", 81);
        otherIdPriorityMap.put("ZP2015_OTHERID_INVALID_REFNO", 82);
        otherIdPriorityMap.put("ZP2015_OTHERID_PRINTID", 84);
        otherIdPriorityMap.put("ZP2015_OTHERID_PICID", 91);
        otherIdPriorityMap.put("ZP2015_OTHERID_NEGID", 90);
        otherIdPriorityMap.put("ZP2015_OTHERID_CDID", 89);
        otherIdPriorityMap.put("ZP2015_OTHERID_ISBN", 88);
        otherIdPriorityMap.put("ZP2015_OTHERID_ISSN", 87);
        otherIdPriorityMap.put("ZP2015_OTHERID_ISMN", 86);
        otherIdPriorityMap.put("ZP2015_OTHERID_MATRIXID", 85);
    }
    

    private String getName(Section sect, Level lvl, LevelContext parentContext) {
        String parentId = lvl.getPid();

        StringBuilder sb = new StringBuilder();
        
        if (configArchDesc.isComposedShortName()) {
        	sb.append(institutionShortName).append(": ");
        } else {        
        	sb.append(institutionName).append(": ");
        }
        sb.append(sect.getFi().getN());

        if(parentId == null) {
            // Koren AS je bez dalsich identifikatoru
            return sb.toString();
        }

        String refOzn = null;
        String invCislo = null;
        String poradoveCislo = null;
        String otherIdent = null;
        String otherIdentType = null;
        int otherIdentPriority = -1;
        String datace = null;
        for(DescriptionItem item : lvl.getDdOrDoOrDp()) {
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
                if(otherIdPriorityMap.get(otherId.getS())>otherIdentPriority) {
                    otherIdentPriority = otherIdPriorityMap.get(otherId.getS());
                    otherIdentType = ElzaTypes.otherIdNameMap.get(otherId.getS());
                    otherIdent = otherId.getV();
                }
            }
            if(item.getT().equals(ElzaTypes.ZP2015_UNIT_DATE)&&(item instanceof DescriptionItemUnitDate)) {
                DescriptionItemUnitDate dataceUnitDate = (DescriptionItemUnitDate)item;
                datace = UnitDateConvertor.convertToString(dataceUnitDate);
            }
        }
        
        if (StringUtils.isBlank(datace)) {
            datace = inheritDatace(parentContext);
        }

        if(StringUtils.isNotEmpty(refOzn)) {
            sb.append(", ").append(refOzn);
        } else if(StringUtils.isNotEmpty(invCislo)) {
            sb.append(", inv. č.: ").append(invCislo);
        } else if(StringUtils.isNotEmpty(poradoveCislo)) {
            sb.append(", poř. č.: ").append(poradoveCislo);
        } else if(StringUtils.isNotEmpty(otherIdent)) {
            if(otherIdentType!=null) {
                sb.append(", ").append(otherIdentType);                
            }
            sb.append(": ").append(otherIdent);
        } else {
            // TODO: stanoveni cesty pomoci hierarchie
            // var parentApu = this.apuMap.get(lvl.getPid());
            // while(parentApu!=null) {
            //    apuPosMap.get()
            // }
            
            // Variantne lze pridat UUID jako ident
            //sb.append(lvl.getUuid().toString());
        }
        if(StringUtils.isNotBlank(datace)) {
            sb.append(", ").append(datace);            
        }
        return sb.toString();
    }
    
    public String inheritTitle(LevelContext parentContext) {
        if (parentContext==null) {
            return null;
        }        
        for(DescriptionItem item : parentContext.getLevel().getDdOrDoOrDp()) {
            if(item.getT().equals("ZP2015_TITLE")) {
                DescriptionItemString title = (DescriptionItemString)item;
                return title.getV();
            }
        }        
        return null;
    }

    /**
     * Precte dataci z nadrizenych urvni
     * 
     * @param parentContext
     *            context nadrizenych urovni, muze byt null
     * @return textova reprezentace datace nebo null
     */
    public String inheritDatace(LevelContext parentContext) {
        while (parentContext != null) {
            for (DescriptionItem item : parentContext.getLevel().getDdOrDoOrDp()) {
                if (item.getT().equals(ElzaTypes.ZP2015_UNIT_DATE) && (item instanceof DescriptionItemUnitDate)) {
                    DescriptionItemUnitDate dataceUnitDate = (DescriptionItemUnitDate) item;
                    return UnitDateConvertor.convertToString(dataceUnitDate);
                }
            }
            parentContext = parentContext.getParentContext();
        }
        return null;
    }

    private String getDesc(Level lvl, LevelContext parentContext, Set<String> levelDuplicities) {
        if (lvl.getPid() == null) {
            // for root no description
            return null;
        }

        String titleStr = null;
        String datace = null;
        StringBuilder sb = new StringBuilder();
        for (DescriptionItem item : lvl.getDdOrDoOrDp()) {
            if (item.getT().equals("ZP2015_TITLE")) {
                DescriptionItemString title = (DescriptionItemString) item;
                titleStr = title.getV();
            }
            if (item.getT().equals(ElzaTypes.ZP2015_UNIT_DATE) && (item instanceof DescriptionItemUnitDate)) {
                DescriptionItemUnitDate dataceUnitDate = (DescriptionItemUnitDate) item;
                datace = UnitDateConvertor.convertToString(dataceUnitDate);
            }
        }

        boolean titleInherited = false;
        boolean dateInherited = false;
        // inherit title and name when null
        if (StringUtils.isBlank(titleStr)) {
            titleStr = inheritTitle(parentContext);
            titleInherited = true;
        }
        if (StringUtils.isBlank(datace) && configArchDesc.isInheritNameDate()) {
            datace = inheritDatace(parentContext);
            dateInherited = true;
        }

        if (StringUtils.isNotBlank(titleStr)) {
            sb.append(titleStr);
        }
		if (StringUtils.isNotBlank(datace)
				&& (configArchDesc.isAddDateToName() || (configArchDesc.isInheritNameDate() && titleInherited))) {
			sb.append(", ").append(datace);
		}
		
		if (levelDuplicities != null) {
			var rawDesc = sb.toString();
			if (levelDuplicities.contains(rawDesc) && StringUtils.isNotBlank(datace) && dateInherited == false
					&& !configArchDesc.isAddDateToName()) {
				sb.append(", ").append(datace);
			}
		}

        if (sb.length() == 0) {
            return null;
        }
        
        
        
        return sb.toString();
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
		return elzaXmlReader;
	}

	@Override
	public Apu getActiveApu() {
		return activeApu;
	}

	@Override
	public Level getProcessedLevel() {
		return processedLevel;
	}
	
    @Override
    public void addArchEntityRef(ArchEntityInfo aei) {
        apRefs.put(aei.getUuid(), aei);
        apLevelRefs.put(aei.getUuid(), aei);
    }
    
    @Override
	public boolean isArchEntityReferenced(UUID uuid) {		
		return apLevelRefs.containsKey(uuid);
	}

	public static class ArchDescDaoRef {
    	
    	private final String handle;
    	
    	private final UUID uuid;
    	
    	public ArchDescDaoRef(String handle, UUID uuid) {
    		this.handle = handle;
    		this.uuid = uuid;
    	}

		public String getHandle() {
			return handle;
		}

		public UUID getUuid() {
			return uuid;
		}

		@Override
		public int hashCode() {
			return Objects.hash(handle, uuid);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ArchDescDaoRef other = (ArchDescDaoRef) obj;
			return Objects.equals(handle, other.handle) && Objects.equals(uuid, other.uuid);
		}    	
    			
    }
    
    private class LevelContext implements EdxItemCovertContext {

        private final Level level;
        private final Apu apu;
        private final LevelContext parentContext;
        
        private Map<ConfigElzaInheritedType,List<InheritedConvertor>> inheritedConvertors = null;

        public LevelContext(Level level, Apu apu, LevelContext parentContext) {
            this.level = level;
            this.apu = apu;
            this.parentContext = parentContext;
        }
        
		public void addInheritedConvertor(EdxItemConvertor convertor, DescriptionItem item, ConfigElzaInheritedType typeSpec) {
			if (inheritedConvertors == null) {
				inheritedConvertors = new HashMap<>();
			}
			inheritedConvertors.compute(typeSpec, (k, v) -> {
				if (v == null) {
					v = new ArrayList<>();
				}
				v.add(new InheritedConvertor(this, convertor, item));
				return v;
			});
		}
		
		public void inheritValue(ConfigElzaInheritedType type) {			
			if (inheritedConvertors!=null) {
				var convertors = inheritedConvertors.get(type);
				if (convertors!=null) {
					for(var convertor:convertors) {
						convertor.convert();
					}
				}
				return;
			}
			if (parentContext!=null) {
				parentContext.inheritValue(type);
			}
		}

        public Level getLevel() {
            return level;
        }

        public Apu getApu() {
            return apu;
        }

        public LevelContext getParentContext() {
            return parentContext;
        }

		@Override
		public ApuSourceBuilder getApusBuilder() {
			return ImportArchDesc.this.getApusBuilder();
		}

		@Override
		public Part getActivePart() {			
			return ImportArchDesc.this.getActivePart();
		}

		@Override
		public ElzaXmlReader getElzaXmlReader() {			
			return ImportArchDesc.this.getElzaXmlReader();
		}

		@Override
		public void addArchEntityRef(ArchEntityInfo aei) {
			ImportArchDesc.this.addArchEntityRef(aei);			
		}

		@Override
		public Apu getActiveApu() {
			return ImportArchDesc.this.getActiveApu();
		}

		@Override
		public Level getProcessedLevel() {
			return level;
		}

		@Override
		public boolean isArchEntityReferenced(UUID uuid) {
			return ImportArchDesc.this.isArchEntityReferenced(uuid);
		}		

    }

    
    private static class InheritedConvertor {
    	
    	private final LevelContext levelContext;
    	
    	private final EdxItemConvertor convertor;
    	
    	private final DescriptionItem item;

		public InheritedConvertor(LevelContext levelContext, EdxItemConvertor convertor, DescriptionItem item) {
			this.levelContext = levelContext;
			this.convertor = convertor;
			this.item = item;
		}
		
		public void convert() {
			convertor.convert(levelContext, item);
		}
    	
    }
    
}
