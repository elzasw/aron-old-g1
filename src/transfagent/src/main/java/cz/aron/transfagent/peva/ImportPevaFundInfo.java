package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.EvidenceUnit;
import cz.aron.peva2.wsdl.EvidenceUnitProcedure;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.NadHeader;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.NadSubsheet;
import cz.aron.peva2.wsdl.Quantity;
import cz.aron.transfagent.config.ConfigPeva2FundProperties;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.peva.jsoncomponent.Column;
import cz.aron.transfagent.peva.jsoncomponent.JSONComponent;
import cz.aron.transfagent.peva.jsoncomponent.Table;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.aron.transfagent.transformation.PropertiesDataProvider;

public class ImportPevaFundInfo {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaFundInfo.class);
		
	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
    
    private ContextDataProvider dataProvider;
    
    private Peva2CodeListProvider codeListProvider;
    
    private final ConfigPeva2FundProperties fundProperties;
    
    private final ApTypeService apTypeService;
    
    private final StorageService storageService;
    
    private String institutionCode;
    
    private String nadNumber;
    
    private UUID archDescRoot;
    
    private final List<String> originators = new ArrayList<>();
    
    private final List<String> camOriginators = new ArrayList<>();
    
    private final List<String> findingAids = new ArrayList<>();
    
    private final List<String> geos = new ArrayList<>();
            
    public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
        ImportPevaFundInfo ifi = new ImportPevaFundInfo(new ConfigPeva2FundProperties(),null, null);
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
    
    public ImportPevaFundInfo(ConfigPeva2FundProperties fundProperties, ApTypeService apTypeService, StorageService storageService) {
    	if (fundProperties!=null) {
    		this.fundProperties = fundProperties;
    	} else {
    		this.fundProperties = new ConfigPeva2FundProperties();
    	}
    	this.apTypeService = apTypeService;
    	this.storageService = storageService;
    }

    public ApuSourceBuilder importFundInfo(Path inputFile, String propFile) throws IOException, JAXBException {
        Validate.isTrue(propFile!=null&&propFile.length()>0);
        
        PropertiesDataProvider pdp = new PropertiesDataProvider();
        Path propPath = Paths.get(propFile);
        pdp.load(propPath);
        
        return importFundInfo(inputFile, null, pdp, null, null, null, null);
    }

	public ApuSourceBuilder importFundInfo(final Path inputFile, UUID uuid, final ContextDataProvider cdp,
			final FundProvider fundProvider, final Peva2CodeListProvider codeListProvider,
			Peva2CachedEntityDownloader entityDownloader, UUID archDescRoot) throws IOException, JAXBException {
		dataProvider = cdp;
		this.codeListProvider = codeListProvider;
		this.archDescRoot = archDescRoot;
		GetNadSheetResponse gnsr = Peva2XmlReader.unmarshalGetNadSheetResponse(inputFile);
		
		//TODO po uprave v PEvA II
		/*
		if (gnsr.getNadPrimarySheet()!=null&&gnsr.getNadPrimarySheet().getState()!=State.OK) {
			throw new FundRemoved();
		}*/
		
		if (gnsr.getNadPrimarySheet() != null) {
			importPrimarySheet(gnsr.getNadPrimarySheet());
		} else {
			var parentUUID = gnsr.getNadSubsheet().getParent();
			NadPrimarySheet primarySheet = fundProvider.getFundByUUID(UUID.fromString(parentUUID));
			if (primarySheet == null) {
				log.info("Fund ignored for now {}", inputFile.getFileName().toString());
				throw new FundIgnored();
			}
			importNadSubsheet(gnsr.getNadSubsheet(), primarySheet);
		}
		return apusBuilder;
	}
    
	private void importNadSubsheet(NadSubsheet subsheet, NadPrimarySheet primarySheet)
			throws IOException, JAXBException {
		var instInfo = getInstitutionInfo(subsheet);
		var nadHeader = subsheet.getHeader();
		var fullFundName = createFundName(instInfo, subsheet);

		Apu apu = apusBuilder.createApu(fullFundName, ApuType.FUND, UUID.fromString(subsheet.getId()));
		createTitlePart(apu, nadHeader);

		Part partFundInfo = ApuSourceBuilder.addPart(apu, CoreTypes.PT_FUND_INFO);
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instInfo.getUuid());

		processArchDesc(partFundInfo);
		processEvidenceNumber(primarySheet, subsheet, partFundInfo);
		processNadHeaderCommon(nadHeader, partFundInfo);
		processNadSheetSubNadSheetCommon(subsheet, partFundInfo);
	}

	private void importPrimarySheet(NadPrimarySheet primarySheet) {
		var instInfo = getInstitutionInfo(primarySheet);
		var nadHeader = primarySheet.getHeader();
		var fullFundName = createFundName(instInfo, primarySheet);

		Apu apu = apusBuilder.createApu(fullFundName, ApuType.FUND, UUID.fromString(primarySheet.getId()));
		createTitlePart(apu, nadHeader);

		Part partFundInfo = ApuSourceBuilder.addPart(apu, CoreTypes.PT_FUND_INFO);
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instInfo.getUuid());

		processArchDesc(partFundInfo);
		processEvidenceNumber(primarySheet, null, partFundInfo);
		processNadHeaderCommon(nadHeader, partFundInfo);
		processNadSheetSubNadSheetCommon(primarySheet, partFundInfo);
	}
    
	private void createTitlePart(Apu apu, NadHeader nadHeader) {
		if (fundProperties.isTitlePart()) {
			var fundName = nadHeader.getName();
			Part partName = ApuSourceBuilder.addPart(apu, CoreTypes.PT_TITLE);
			partName.setValue(fundName);
			ApuSourceBuilder.addString(partName, CoreTypes.TITLE, fundName);
		}
	}

	private InstitutionInfo getInstitutionInfo(NadSheet nadSheet) {
		var instInfo = dataProvider.getInstitutionApu(nadSheet.getInstitution().getExternalId());
		if (instInfo == null) {
			throw new RuntimeException("Missing institution: " + institutionCode);
		}
		institutionCode = nadSheet.getInstitution().getExternalId();
		return instInfo;
	}
    
	private String createFundName(InstitutionInfo institutionInfo, NadSheet nadSheet) {
		if (fundProperties.isComposedFundName()) {
			return institutionInfo.getName() + ": " + nadSheet.getHeader().getName() + " "
					+ (nadSheet.getHeader().getTimeRange()!=null?Peva2Utils.getAsString(nadSheet.getHeader().getTimeRange()):"");
		} else if (fundProperties.isComposedShortFundName()) {
			return institutionInfo.getShortName() + ": " + nadSheet.getHeader().getName() + " "
					+ (nadSheet.getHeader().getTimeRange()!=null?Peva2Utils.getAsString(nadSheet.getHeader().getTimeRange()):"");
		} else {
			return nadSheet.getHeader().getName();
		}
	}
	
	private void processArchDesc(Part partFundInfo) {
		if (archDescRoot != null) {
			apusBuilder.addApuRef(partFundInfo, "ARCHDESC_ROOT_REF", archDescRoot);
			if (fundProperties.isArchdescFlag()) {
				ApuSourceBuilder.addEnum(partFundInfo, "ARCHDESC_FLAG", "Ano", false);
			}
		}
	}

	private void processEvidenceNumber(NadPrimarySheet primarySheet, NadSubsheet subsheet, Part partFundInfo) {
		if (StringUtils.isNotBlank(primarySheet.getEvidenceNumber())) {
			if (subsheet!=null&&subsheet.getNumber()!=null) {
				ApuSourceBuilder.addString(partFundInfo, "CISLO_NAD", primarySheet.getEvidenceNumber()+"/"+subsheet.getNumber());
				nadNumber = primarySheet.getEvidenceNumber()+"_"+subsheet.getNumber();
			} else {
				ApuSourceBuilder.addString(partFundInfo, "CISLO_NAD", primarySheet.getEvidenceNumber());
				nadNumber = primarySheet.getEvidenceNumber();
			}
		}		
	}
	
    private void processNadHeaderCommon(NadHeader nadHeader, Part partFundInfo) {
    	var mark = nadHeader.getMark();
        if(mark!=null) {
        	ApuSourceBuilder.addString(partFundInfo, "FUND_MARK", mark);
        }        
        // datace
        if (nadHeader.getTimeRange()!=null) {
        	Peva2Utils.fillDateRange(nadHeader.getTimeRange(), partFundInfo, apusBuilder);
        }
        // ke kdy jsou udaje platne
		if (fundProperties.isToDate()&&nadHeader.getToDate()!=null) {
			ApuSourceBuilder.addString(partFundInfo, "FUND_UPTODATE", nadHeader.getToDate().toString());
		}
		processEvidenceStatus(nadHeader, partFundInfo);
		processAccessibility(nadHeader, partFundInfo);
    }
    
	private void processNadSheetSubNadSheetCommon(NadSheet nadSheet, Part partFundInfo) {
		generateDescription(nadSheet, partFundInfo);
		processAdditionalInfo(nadSheet, partFundInfo);
		processFindingAids(nadSheet, partFundInfo);
		processOriginators(nadSheet, partFundInfo);
		processLanguages(nadSheet, partFundInfo);
		processEvidenceUnits(nadSheet, partFundInfo);
		processPlacesOfOrigin(nadSheet, partFundInfo);
		processPreservationStatus(nadSheet, partFundInfo);
		processLength(nadSheet, partFundInfo);
		processDigital(nadSheet, partFundInfo);
		processThematicGroups(nadSheet, partFundInfo);
	}
	
	private void generateDescription(NadSheet nadSheet, Part partFundInfo) {
		
		var sj = new StringJoiner("\r\n");
		boolean add = false;
		
		var additionalInfo = nadSheet.getAdditionalInfo();		
		// prida popis puvodce do main apu
		if (fundProperties.isOriginatorAsDescription()&&StringUtils.isNotBlank(additionalInfo.getOriginator())) {			
			var originator = correctString(additionalInfo.getOriginator());
			if (fundProperties.isOriginatorNoteRemovePrefix()) {
				originator = removePrefixCaseInsensitive(originator,nadSheet.getHeader().getName());
			}
			sj.add(originator);
			add = true;
		}
		if (fundProperties.isNoteAsDescription()&&StringUtil.isNotBlank(additionalInfo.getNote())) {
			sj.add(correctString(additionalInfo.getNote()));
			add = true;
		}
		if (fundProperties.isParseInternalChangesAsDescription()&& StringUtils.isNotBlank(additionalInfo.getInternalChanges())) {
			additionalInfo.getInternalChanges().lines().findFirst().ifPresent(l -> {
				sj.add(l);
			});		
			add = true;
		}
		if (add) {
			apusBuilder.getMainApu().setDesc(sj.toString());
		}
	}
	
	
	private String removePrefixCaseInsensitive(String text, String prefix) {
		var textLowerCase = text.toLowerCase();
		var prefixLowerCase = prefix.toLowerCase();
		if (textLowerCase.startsWith(prefixLowerCase)) {
			text = text.substring(prefixLowerCase.length()).trim();
		}
		return text;
	}

	private void processAdditionalInfo(NadSheet nadSheet, Part partFundInfo) {
		var additionalInfo = nadSheet.getAdditionalInfo();
		if (additionalInfo != null) {
			if (fundProperties.isNote()&&StringUtils.isNotBlank(additionalInfo.getNote())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_NOTE", correctString(additionalInfo.getNote()));
			}
			if (fundProperties.isOriginatorNote()&&StringUtils.isNotBlank(additionalInfo.getOriginator())) {				
				var origNote = additionalInfo.getOriginator();
				if (fundProperties.isOriginatorNoteRemovePrefix()) {
					origNote = removePrefixCaseInsensitive(origNote,nadSheet.getHeader().getName());
				}				
				ApuSourceBuilder.addString(partFundInfo, "FUND_ORIG_NOTE",
						correctString(origNote));
			}
			if (StringUtils.isNotBlank(additionalInfo.getThematicDescription())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_TOPIC",
						correctString(additionalInfo.getThematicDescription()));
			}
			if (StringUtils.isNotBlank(additionalInfo.getEdition())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_EDITIONS", correctString(additionalInfo.getEdition()));
			} else if (fundProperties.isParseInternalChanges()
					&& StringUtils.isNotBlank(additionalInfo.getInternalChanges())) {
				additionalInfo.getInternalChanges().lines().findFirst().ifPresent(l -> {
					ApuSourceBuilder.addString(partFundInfo, "FUND_EDITIONS", correctString(l));
				});
			}
			if (fundProperties.isLiterature() && StringUtils.isNotBlank(additionalInfo.getLiterature())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_LITERATURE",
						correctString(additionalInfo.getLiterature()));
			}
			if (fundProperties.isArchiveGroupParts()&&StringUtils.isNotBlank(additionalInfo.getArchiveGroupParts())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_ARCHIVE_GROUP_PARTS",
						correctString(additionalInfo.getArchiveGroupParts()));
			}
		}
	}

	private void processFindingAids(NadSheet nadSheet, Part partFundInfo) {
		var fillFindingAids = fundProperties.isFindingAids();
		var findingAids = nadSheet.getFindingAids();
		if (findingAids != null && findingAids.getFindingAid() != null) {
			findingAids.getFindingAid().forEach(fa -> {
				if (fillFindingAids) {
					apusBuilder.addApuRef(partFundInfo, "FINDINGAID_REF", UUID.fromString(fa));
				}
				this.findingAids.add(fa);
			});
		}
	}
    
	private void processOriginators(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isOriginators()) {
			var originators = nadSheet.getOriginators();
			if (originators != null && originators.getOriginator() != null) {
				if (fundProperties.isCamOriginators()) {
					List<UUID> uuids = originators.getOriginator().stream().map(uuid -> UUID.fromString(uuid))
							.collect(Collectors.toList());
					var archEntitySources = this.dataProvider.getArchEntityApuSources(uuids,
							ImportPevaOriginator.ENTITY_CLASS);
					for (var archEntitySource : archEntitySources) {
						if (archEntitySource.getStatus() != EntityStatus.ACCESSIBLE
								&& archEntitySource.getStatus() != EntityStatus.AVAILABLE) {
							// entita neni dostupna nebo neni urcena k publikaci
							continue;
						}
						var ipoi = new ImportPevaOriginatorInfo(apTypeService, codeListProvider);
						var dataDir = storageService.getApuDataDir(archEntitySource.getDataDir());
						var originatorPath = ImportPevaOriginator.getPevaOriginatorFile(dataDir);
						if (originatorPath.isEmpty()) {
							log.error("Fail to read peva originator, not exist, path={}", dataDir);
							throw new IllegalStateException();
						}
						try {
							ipoi.importOriginator(originatorPath.get(), dataProvider);
						} catch (IOException e) {
							log.error("Fail to read peva originator", e);
							throw new IllegalStateException();
						} catch (JAXBException e) {
							log.error("Fail to read peva originator", e);
							throw new IllegalStateException();
						}
						if (ipoi.getCamUUID() != null) {
							apusBuilder.addApuRef(partFundInfo, CoreTypes.ORIGINATOR_REF,
									UUID.fromString(ipoi.getCamUUID()));
							camOriginators.add(ipoi.getCamUUID());
						}
					}
					this.originators.addAll(originators.getOriginator());
				} else {
					originators.getOriginator().forEach(o -> {
						apusBuilder.addApuRef(partFundInfo, CoreTypes.ORIGINATOR_REF, UUID.fromString(o));
						this.originators.add(o);
					});
				}
			}
		}
	}	
	
    private static Comparator<Object> COMPARATOR_CS_CZ = Collator.getInstance(new Locale("cs","CZ"));
    
	private void processLanguages(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isLanguages()) {
			var languages = nadSheet.getLanguageRecords();									
			var lng = new TreeSet<String>(COMPARATOR_CS_CZ);
			for (var language : languages.getLanguageRecord()) {
				var p2Lang = codeListProvider.getCodeLists().getLanguage(language.getLanguage());
				if (p2Lang != null) {
					var result = ApuSourceBuilder.addEnum(partFundInfo, CoreTypes.LANGUAGE, p2Lang.getName());
					result.setVisible(false);
					lng.add(p2Lang.getName());
				}
			}
			if (!lng.isEmpty()) {
				StringJoiner sj = new StringJoiner(", ");
				lng.forEach(l->sj.add(l));
				ApuSourceBuilder.addString(partFundInfo, CoreTypes.LANGUAGE_TEXT, sj.toString());
			}
		}
	}
	
	private void processLength(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isLength()) {
			var length = nadSheet.getLength();
			if (length!=null) {
				StringJoiner sj = new StringJoiner("");
				processEvidenceUnitProcedure("", length, sj);
				if (sj.length()>0) {
					ApuSourceBuilder.addString(partFundInfo, "FUND_LENGTH", sj.toString());				
				}
			}
		}
	}
	
	private void processDigital(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isDigitalLength()) {
			var digitalLength = nadSheet.getDigital();
			if (digitalLength!=null) {
				StringJoiner sj = new StringJoiner("");
				processEvidenceUnitProcedure("", digitalLength, sj);
				if (sj.length()>0) {
					ApuSourceBuilder.addString(partFundInfo, "FUND_DIGITAL_LENGTH", sj.toString());				
				}
			}
		}
	}

	class EUSort {
		
		private final EvidenceUnit evidenceUnit;
		
		private final List<EvidenceUnit> partial = new ArrayList<>();
		
		public EUSort(EvidenceUnit evidenceUnit) {
			this.evidenceUnit = evidenceUnit;
		}

		public EvidenceUnit getEvidenceUnit() {
			return evidenceUnit;
		}

		public List<EvidenceUnit> getPartial() {
			return partial;
		}
		
	}
	
	private List<EvidenceUnit> orderEvidenceUnit(List<EvidenceUnit> evidenceUnits) {
		var map = new LinkedHashMap<String, EUSort>();
		var ret = new ArrayList<EvidenceUnit>();
		// insert main evidence unit types into map
		for (var evidenceUnit : evidenceUnits) {
			var id = evidenceUnit.getId();
			var type = codeListProvider.getCodeLists()
					.getEvidenceUnitType(evidenceUnit.getEvidenceUnitType().getValue());
			if (type!=null&&type.getMainEUTId() == null) {
				map.put(evidenceUnit.getEvidenceUnitType().getValue(), new EUSort(evidenceUnit));
			}
		}

		// attach partial evidence unit types
		for (var evidenceUnit : evidenceUnits) {
			var type = codeListProvider.getCodeLists()
					.getEvidenceUnitType(evidenceUnit.getEvidenceUnitType().getValue());
			if (type != null && type.getMainEUTId() != null) {
				var main = map.get(type.getMainEUTId());
				if (main == null) {
					log.error("Undefined main evidence unit type {}, for partial type {}", type.getMainEUTId(),
							evidenceUnit.getEvidenceUnitType().getValue());
					// pokud nemam main evidence unit, zaradim ji na uroven main
					map.put(evidenceUnit.getEvidenceUnitType().getValue(), new EUSort(evidenceUnit));
					// throw new IllegalStateException("Undefined main evidence unit type:" +
					// type.getMainEUTId());
				} else {
					main.getPartial().add(evidenceUnit);
				}
			}
		}
		map.forEach((k, v) -> {
			ret.add(v.getEvidenceUnit());
			ret.addAll(v.getPartial());
		});
		return ret;
	}
	
	private void processEvidenceUnits(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isEvidenceUnits()) {
			var evidenceUnits = nadSheet.getEvidenceUnits();
			if (evidenceUnits!=null) {
				var rows = new ArrayList<List<String>>();
				var ejs = orderEvidenceUnit(evidenceUnits.getEvidenceUnit());
				for(var evidenceUnit:ejs) {
					var type = evidenceUnit.getEvidenceUnitType();
					var typeName = codeListProvider.getCodeLists().getEvidenceUnitType(type.getValue());
					var name = "";
					if (typeName!=null) {
						name = typeName.getName();
					}
					if (StringUtils.isNotBlank(evidenceUnit.getNote())) {
						name += "\r\n" + evidenceUnit.getNote();
					}					
					String timeRange = "";
					if (evidenceUnit.getTimeRange()!=null) {
						timeRange = Peva2Utils.getAsString(evidenceUnit.getTimeRange());
					}
					
					var digitalEJ = processEvidenceUnitProcedure(evidenceUnit.getDigitalProcedure());
					var countEJ = processEvidenceUnitProcedure(evidenceUnit.getCountProcedure());
					var lengthEJ = processEvidenceUnitProcedure(evidenceUnit.getLengthProcedure());
					
					var unprocessed = new StringJoiner("/");
					var processed = new StringJoiner("/");
					var inventarized = new StringJoiner("/");
					var damaged = new StringJoiner("/");
					
					boolean add = false;
					if (digitalEJ.isActive()) {
						digitalEJ.addDamaged(damaged);
						digitalEJ.addInventarized(inventarized);
						digitalEJ.addProcessed(processed);
						digitalEJ.addUnprocessed(unprocessed);
						add = true;
					}
					if (countEJ.isActive()) {
						countEJ.addDamaged(damaged);
						countEJ.addInventarized(inventarized);
						countEJ.addProcessed(processed);
						countEJ.addUnprocessed(unprocessed);
						add = true;
					}
					
					/*
					if (lengthEJ.isActive()) {
						lengthEJ.addDamaged(damaged);
						lengthEJ.addInventarized(inventarized);
						lengthEJ.addProcessed(processed);
						lengthEJ.addUnprocessed(unprocessed);
						add = true;
					}*/
				
					if (add||true) {
						if (unprocessed.length()==0) {
							unprocessed.add("0");
						}
						if (processed.length()==0) {
							processed.add("0");
						}
						if (inventarized.length()==0) {
							inventarized.add("0");
						}
						if (damaged.length()==0) {
							damaged.add("0");
						}
						rows.add(Arrays.asList(name,unprocessed.toString(),processed.toString(),inventarized.toString(),damaged.toString(),timeRange));
					}
				}
				if (!rows.isEmpty()) {					
					var table = new Table(Arrays.asList(new Column("Název"), new Column("Nezpracováno"),
							new Column("Zpracováno"), new Column("Inventarizováno"), new Column("Poškozeno"),
							new Column("Časový rozsah")), rows);
					var jsonComponent = new JSONComponent("table",table);
					String value;
					try {
						value = jsonComponent.serializeToString();
					} catch (JsonProcessingException e) {
						log.error("Fail to serialize EVIDENCE_UNIT_JSON to json",e);
						throw new IllegalStateException(e);
					}
					ApuSourceBuilder.addJson(partFundInfo, "EVIDENCE_UNIT", value);
				}
			}		
		}
	}
	
	class EJProcedure {
		
		private String damaged;
		
		private String inventarized;
		
		private String unprocessed;
		
		private String processed;
		
		private boolean active = false;

		public String getDamaged() {
			return damaged;
		}

		public void setDamaged(String damaged) {
			this.damaged = damaged;
			active = true;
		}

		public String getInventarized() {
			return inventarized;
		}

		public void setInventarized(String inventarized) {
			this.inventarized = inventarized;
			active = true;
		}

		public String getUnprocessed() {
			return unprocessed;
		}

		public void setUnprocessed(String unprocessed) {
			this.unprocessed = unprocessed;
			active = true;
		}

		public String getProcessed() {
			return processed;
		}

		public void setProcessed(String processed) {
			this.processed = processed;
			active = true;
		}

		public boolean isActive() {
			return active;
		}
		
		public void addDamaged(StringJoiner sj) {
			if (damaged!=null) {
				sj.add(damaged);
			}
		}
		
		public void addInventarized(StringJoiner sj) {
			if (inventarized!=null) {
				sj.add(inventarized);
			}
		}
		
		public void addUnprocessed(StringJoiner sj) {
			if (unprocessed!=null) {
				sj.add(unprocessed);
			}
		}
		
		public void addProcessed(StringJoiner sj) {
			if (processed!=null) {
				sj.add(processed);
			}
		}
	}
	
	private EJProcedure processEvidenceUnitProcedure(EvidenceUnitProcedure evidenceUnitProcedure) {
		var ejProcedure = new EJProcedure();
		if (evidenceUnitProcedure == null) {
			return ejProcedure;
		}		
		var quantity = evidenceUnitProcedure.getDamaged(); 
		if (quantity !=null&&quantity.getAmount().compareTo(BigDecimal.ZERO)>0) {
			ejProcedure.setDamaged(quantity.getAmount().toString()+getUnit(quantity));
		}		
		quantity = evidenceUnitProcedure.getInventory();
		if (quantity != null && quantity.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			ejProcedure.setInventarized(quantity.getAmount().toString() + getUnit(quantity));
		}						
		quantity = evidenceUnitProcedure.getNotProcessed();
		if (quantity != null && quantity.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			ejProcedure.setUnprocessed(quantity.getAmount().toString() + getUnit(quantity));
		}						
		quantity = evidenceUnitProcedure.getProcessed();
		if (quantity != null && quantity.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			ejProcedure.setProcessed(quantity.getAmount().toString() + getUnit(quantity));
		}						
		return ejProcedure;
	}
	
	private void processEvidenceUnitProcedure(String name, EvidenceUnitProcedure evidenceUnitProcedure,
			StringJoiner sj) {
		if (evidenceUnitProcedure == null) {
			return;
		}
		boolean add = false;
		StringJoiner sjInt = new StringJoiner(", ", name, "");
		if (processQuantity(evidenceUnitProcedure.getDamaged(), "Poškozeno: ", sjInt)) {
			add = true;
		}
		if (processQuantity(evidenceUnitProcedure.getInventory(), "Inventarizováno: ", sjInt)) {
			add = true;
		}
		if (processQuantity(evidenceUnitProcedure.getNotProcessed(), "Nezpracováno: ", sjInt)) {
			add = true;
		}
		if (processQuantity(evidenceUnitProcedure.getProcessed(), "Zpracováno: ", sjInt)) {
			add = true;
		}
		if (add) {
			sj.add(sjInt.toString());
		}
	}

	private boolean processQuantity(Quantity quantity, String name, StringJoiner sj) {
		if (quantity!=null&&quantity.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			sj.add(name + quantity.getAmount()
					+ getUnit(quantity));
			return true;
		}
		return false;
	}
	
	private String getUnit(Quantity quantity) {
		if (quantity.getCountUnit() != null) {
			switch (quantity.getCountUnit()) {
			case SINGLE:
				return "";
			case DOZEN:
				return " tucet";
			case PAIR:
				return " pár";
			default:
				return "";
			}		
		}
		if (quantity.getDigitalUnit() != null) {
			return " " + quantity.getDigitalUnit();
		}
		if (quantity.getLengthUnit() != null) {
			return " " + quantity.getLengthUnit();
		}
		return "";
	}
	
	private void processPlacesOfOrigin(NadSheet nadSheet, Part partFundInfo) {
		if (!fundProperties.isPlacesOfOrigin()) {
			return;
		}
		//var places = new TreeSet<String>();
		var placesOfOrigin = nadSheet.getPlacesOfOrigin();
		for(var placeOfOrigin:placesOfOrigin.getPlaceOfOrigin()) {
			var placeId = placeOfOrigin.getPlace();
			//var geo = entityDownloader.getGeoObject(placeId);
			//var name = geo.getPreferredName().getPrimaryPart();
			//places.add(name);								
			apusBuilder.addApuRef(partFundInfo, "ORIG_PLACES_REF", UUID.fromString(placeId));
			geos.add(placeId);
		}
		
	}
	
	private void processPreservationStatus(NadSheet nadSheet, Part partFundInfo) {
		if (!fundProperties.isPreservationStatus()) {
			return;
		}
		if (nadSheet.getPreservationStatus() != null) {
			StringJoiner sj = new StringJoiner(", ");
			boolean add = false;
			var preservationStatus = nadSheet.getPreservationStatus();
			if (preservationStatus.getIntegrity() != null) {
				var integrity = codeListProvider.getCodeLists().getIntegrityName(preservationStatus.getIntegrity());
				sj.add(integrity);
				add = true;
			}
			if (preservationStatus.getPhysicalState() != null) {
				var physicalState = codeListProvider.getCodeLists()
						.getPhysicalStateName(preservationStatus.getPhysicalState());
				sj.add(physicalState);
				add = true;
			}
			if (preservationStatus.getDamages() != null
					&& !CollectionUtils.isEmpty(preservationStatus.getDamages().getDamage())) {
				StringJoiner sjDmg = new StringJoiner(", ", "Poškození:", "");
				for (var damage : preservationStatus.getDamages().getDamage()) {
					switch (damage.getType()) {
					case FRAGILE:
						sjDmg.add("Křehké");
						break;
					case TORN:
						sjDmg.add("Roztržené");
						break;
					case MOLD:
						sjDmg.add("Plíseň");
						break;
					case INSECT:
						sjDmg.add("Hmyz");
						break;
					case RODENT:
						sjDmg.add("Hlodavci");
						break;
					case FIRE:
						sjDmg.add("Oheň");
						break;
					case HEAT:
						sjDmg.add("Žár");
						break;
					case WATER:
						sjDmg.add("Voda");
						break;
					case CORROSION:
						sjDmg.add("Koroze");
						break;
					case EMP:
						sjDmg.add("EMP");
						break;
					case DUST:
						sjDmg.add("Prach");
						break;
					case LIGHT:
						sjDmg.add("Světlo");
						break;
					case OTHER:
						sjDmg.add("Jiné");
						break;
					}
					if (StringUtils.isNotBlank(damage.getNote())) {
						sjDmg.add(damage.getNote());
					}
					add = true;
				}
				sj.add(sjDmg.toString());
			}
			if (add) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_PRESERVATION", sj.toString());
			}
		}
	}
	
	private void processEvidenceStatus(NadHeader nadHeader, Part partFundInfo) {
		if (fundProperties.isEvidenceStatus()) {
			var evidenceStatus = nadHeader.getEvidenceStatus();
			if (evidenceStatus != null) {
				String statusName = null;
				switch (evidenceStatus) {
				case DIRECT_CARE:
					statusName = "Archivní soubory v přímé péči instituce, uložené v instituci";
					break;
				case OUTSIDE_INSTITUTION_CONTRACT:
					statusName = "Archivní soubory uložené na základě smlouvy o uložení mimo instituci";
					break;
				case INSIDE_INSTITUTION_CONTRACT:
					statusName = "Archivní soubory uložené v instituci na základě smlouvy o uložení";
					break;
				case EVIDED_ISSUED_TO_OWNER:
					statusName = "Archivní soubory vydané vlastníkům a archivem evidované";
					break;
				case EVIDED:
					statusName = "Archivní soubory archivem pouze evidované";
					break;
				default:
				}
				if (statusName != null) {
					ApuSourceBuilder.addEnum(partFundInfo, "FUND_EVIDENCE_STATUS", evidenceStatus.toString())
							.setVisible(false);
					ApuSourceBuilder.addString(partFundInfo, "FUND_EVIDENCE_STATUS_TEXT", statusName);
				}
			}
		}
	}
	
	private void processAccessibility(NadHeader nadHeader, Part partFundInfo) {
		if (fundProperties.isAccessibility()) {
			var accessibilityUUID = nadHeader.getAccessibility();
			if (StringUtils.isNotBlank(accessibilityUUID)) {
				var accessibility = codeListProvider.getCodeLists().getAccessibilityName(accessibilityUUID);
				if (StringUtils.isNotBlank(accessibility)) {
					ApuSourceBuilder.addEnum(partFundInfo, "ACCESSIBILITY", accessibility);
				}
			}
		}
	}
	
	private void processThematicGroups(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isThematicEvidenceGroups()) {			
			var grps = new TreeSet<String>(COMPARATOR_CS_CZ);			
			for(var groupUUID:nadSheet.getGroups().getGroup()) {
				var thematicGroup = codeListProvider.getCodeLists().getThematicGroup(groupUUID);
				if (thematicGroup != null) {
					ApuSourceBuilder.addEnum(partFundInfo, "FUND_THEMATIC_GROUP", thematicGroup.getName(), false);
					grps.add(thematicGroup.getName());
				}
			}
			if (!grps.isEmpty()) {
				StringJoiner sj = new StringJoiner(", ");
				grps.forEach(l->sj.add(l));
				ApuSourceBuilder.addString(partFundInfo, "FUND_THEMATIC_GROUP_TEXT", sj.toString());
			}
		}
	}

	private String correctString(String original) {
		if (fundProperties.isCorrectLineSeparators()) {
			return Peva2Utils.correctLineSeparators(original);
		} else {
			return original;
		}				
	}
		
    public String getInstitutionCode() {
    	return institutionCode;
    }
    
    public String getNadNumber() {
        return nadNumber;
    }
    
    public List<String> getOriginatorIds() {
    	return originators;
    }
    
    public List<String> getCAMOriginatorIds() {
    	return camOriginators;
    }

	public List<String> getFindingAidIds() {
		return findingAids;
	}	

	public List<String> getGeos() {
		return geos;
	}	

	public interface FundProvider {		
		NadPrimarySheet getFundByUUID(UUID uuid) throws IOException, JAXBException;		
	}

	@SuppressWarnings("serial")
	public static class FundIgnored extends RuntimeException {
		
	}
	
	@SuppressWarnings("serial")
	public static class FundRemoved extends RuntimeException {
		
	}
	
}
