package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.datace.ItemDateRangeAppender;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Levels;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.Sections;

public class ImportFundInfo {

	private ElzaXmlReader elzaXmlReader;

	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	private ContextDataProvider dataProvider;

	private String institutionCode;
	
	private boolean archdescFlag;
	
	public static void main(String[] args) {
		Path inputFile = Path.of(args[0]);
		ImportFundInfo ifi = new ImportFundInfo();
		try {
			ApuSourceBuilder apusrcBuilder = ifi.importFundInfo(inputFile, args[1]);
			Path ouputPath = Paths.get(args[2]);
			try(OutputStream fos = Files.newOutputStream(ouputPath)) {
				apusrcBuilder.build(fos);
			}
		} catch(Exception e) {
			System.err.println("Failed to process input file: "+inputFile);
			e.printStackTrace();
		}

	}

	public ApuSourceBuilder importFundInfo(Path inputFile, String propFile) throws IOException, JAXBException {
		Validate.isTrue(propFile!=null&&propFile.length()>0);
		
		PropertiesDataProvider pdp = new PropertiesDataProvider();
		Path propPath = Paths.get(propFile);
		pdp.load(propPath);
		
		return importFundInfo(inputFile, null, pdp, false);
	}

	public ApuSourceBuilder importFundInfo(final Path inputFile, UUID uuid, final ContextDataProvider cdp,
			final boolean archdescFlag) throws IOException, JAXBException {
		this.dataProvider = cdp;
		this.archdescFlag = archdescFlag;
		try (InputStream is = Files.newInputStream(inputFile)) {
			elzaXmlReader = ElzaXmlReader.read(is);
			return importFundInfo(uuid);
		}
	}

    /**
     * 
     * @param uuid Optional fund UUID. If known, might be null
     * @return
     */
    
	private ApuSourceBuilder importFundInfo(UUID uuid) {
		Sections sections = elzaXmlReader.getEdx().getFs();
		if(sections==null||sections.getS().size()==0) {
			throw new RuntimeException("Missing section data");
		}
		if(sections.getS().size()>1) {
			throw new RuntimeException("Exports with one section are supported");
		}

		Section sect = sections.getS().get(0);
		FundInfo fi = sect.getFi();
		String fundName = createName(fi);

		Apu apu = apusBuilder.createApu(fundName,ApuType.FUND, uuid);
		Part partName = apusBuilder.addPart(apu, CoreTypes.PT_TITLE);
		partName.setValue(fundName);
		apusBuilder.addString(partName, CoreTypes.TITLE, fundName);

		institutionCode = fi.getIc();
		var instIndo = dataProvider.getInstitutionApu(institutionCode);
		if(instIndo==null) {
            throw new RuntimeException("Missing institution: " + institutionCode);
		}

		Part partFundInfo = apusBuilder.addPart(apu, CoreTypes.PT_FUND_INFO);
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instIndo.getUuid());
		var rootLvlUuid = getRootLevelUuid(sect.getLvls());
		if(rootLvlUuid!=null) {
			apusBuilder.addApuRef(partFundInfo, "ARCHDESC_ROOT_REF", rootLvlUuid);
			if (archdescFlag) {
				ApuSourceBuilder.addEnum(partFundInfo, "ARCHDESC_FLAG", "Ano", false);	
			}			
		}
        if(fi.getNum()!=null) {
            apusBuilder.addString(partFundInfo, "CISLO_NAD", fi.getNum().toString());
        }
        if(fi.getMrk()!=null) {
            apusBuilder.addString(partFundInfo, "FUND_MARK", fi.getMrk());
        }

		// Puvodce
		var puvodci = getPuvodci(sect.getLvls());
		for(var puvodceUuid: puvodci) {
			apusBuilder.addApuRef(partFundInfo, CoreTypes.ORIGINATOR_REF, puvodceUuid);
		}

        Set<String> apRefs = new HashSet<>();
        for(Level lvl : sect.getLvls().getLvl()) {
            // collect all date intervals
            if(lvl.getPid() != null) {
                var ranges = getItemDateRanges(lvl);
                for(ItemDateRange range : ranges) {
                    ItemDateRangeAppender dateRangeAppender = new ItemDateRangeAppender(range);
                    dateRangeAppender.appendTo(apu);
                }
            }
            
            // zjisteni rejstrikovych hesel
            for(var item: lvl.getDdOrDoOrDp()) {
                if(item instanceof DescriptionItemAPRef) {
                    DescriptionItemAPRef apRef = (DescriptionItemAPRef)item;
                    
                    String apid = apRef.getApid();
                    if(apRef.getT().equals(ElzaTypes.ZP2015_ORIGINATOR)) {
                        apRefs.add(apid);
                    } else
                    if(apRef.getT().equals(ElzaTypes.ZP2015_ENTITY_ROLE)) {
                        apRefs.add(apid);
                    }
                }
            }
        }
        // add refs
        Map<String, AccessPoint> apMap = elzaXmlReader.getApMap();
        for(String apid: apRefs) {
            var ap = apMap.get(apid);
            if(ap!=null) {
                UUID apUuid = UUID.fromString(ap.getApe().getUuid());
                apusBuilder.addApuRef(partFundInfo, CoreTypes.FUND_AP_REF, apUuid, false);
            }
        }

        return apusBuilder;
    }
	
    private String createName(FundInfo fi) {
        var sb = new StringBuilder();
        sb.append(fi.getN());
        var date = fi.getTr();
        if (date != null) {
            sb.append(", ").append(date);
        }
        return sb.toString();
    }

    private List<ItemDateRange> getItemDateRanges(Level lvl) {
        List<ItemDateRange> items = new ArrayList<>();
        for(DescriptionItem item : lvl.getDdOrDoOrDp()) {
            if(item.getT().equals("ZP2015_UNIT_DATE")) {
            	if (item instanceof DescriptionItemUnitDate) {
            		items.add(fromDescriptionItemUnitDate((DescriptionItemUnitDate) item));
            	}
            }
        }
        return items;
    }

    private ItemDateRange fromDescriptionItemUnitDate(DescriptionItemUnitDate date) {
        var idr = new ItemDateRange();
        idr.setF(date.getD().getF());
        idr.setFe(date.getD().isFe());
        idr.setTo(date.getD().getTo());
        idr.setToe(date.getD().isToe());
        idr.setFmt(date.getD().getFmt());
        idr.setVisible(false);
        idr.setType("UNIT_DATE");
        return idr;
    }

	private UUID getRootLevelUuid(Levels lvls) {
		if(lvls==null) {
			return null;
		}
		List<Level> lvlList = lvls.getLvl();
		if(lvlList.size()==0) {
			return null;
		}
		Level lvl = lvlList.get(0);
		return UUID.fromString(lvl.getUuid());
	}

	private List<UUID> getPuvodci(Levels lvls) {
		if(lvls==null) {
			return Collections.emptyList();
		}
		List<String> puvodciXmlId = new ArrayList<>();
		Set<String> found = new HashSet<>();
		for(Level lvl: lvls.getLvl()) {
			for(DescriptionItem item: lvl.getDdOrDoOrDp()) {
				if(item.getT().equals(ElzaTypes.ZP2015_ORIGINATOR)) {
			        if(item instanceof DescriptionItemUndefined) {
			            continue;
			        }				    
					DescriptionItemAPRef apRef = (DescriptionItemAPRef)item;
					if(!found.contains(apRef.getApid())) {
						found.add(apRef.getApid());
						puvodciXmlId.add(apRef.getApid());
					}
				}
			}
		}
		if(found.size()==0) {
			return Collections.emptyList();
		}

		Map<String, AccessPoint> apMap = elzaXmlReader.getApMap();

		List<UUID> puvodci = new ArrayList<>(puvodciXmlId.size());
		for(String xmlId: puvodciXmlId) {
			AccessPoint ap = apMap.get(xmlId);
			if(ap==null) {
				throw new RuntimeException("Missing AP with ID: "+xmlId);
			}
			puvodci.add(UUID.fromString(ap.getApe().getUuid()));
		}
		return puvodci;
	}

	public String getInstitutionCode() {
		return institutionCode;
	}

}
