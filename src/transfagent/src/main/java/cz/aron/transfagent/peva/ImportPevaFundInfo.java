package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import cz.aron.peva2.wsdl.DigitalUnitType;
import cz.aron.peva2.wsdl.EvidenceUnit;
import cz.aron.peva2.wsdl.EvidenceUnitProcedure;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.LengthUnitType;
import cz.aron.peva2.wsdl.NadHeader;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.NadSubsheet;
import cz.aron.peva2.wsdl.Quantity;
import cz.aron.transfagent.config.ConfigPeva2FundProperties;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.peva.codelist.Peva2ThematicEvidenceGroup;
import cz.aron.transfagent.peva.fund.Peva2FundResultBuilder;
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
	
	private static final Locale CZ_LOCALE = new Locale("cs", "CZ");
	
	private static NumberFormat DECIMAL_FORMAT = DecimalFormat.getInstance(CZ_LOCALE);
		
	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
    
    private ContextDataProvider dataProvider;
    
    private Peva2CodeListProvider codeListProvider;
    
    private final ConfigPeva2FundProperties fundProperties;
    
    private final ApTypeService apTypeService;
    
    private final StorageService storageService;
    
    private String institutionCode;
    
    private String nadNumber;
    
    private UUID archDescRoot;
    
    private String primarySheetNumber;
    
    private Integer subSheetNumber;
    
    private final List<String> originators = new ArrayList<>();
    
    private final List<String> camOriginators = new ArrayList<>();
    
    private final List<String> findingAids = new ArrayList<>();
    
    private final List<String> geos = new ArrayList<>();
    
    private final List<String> camGeos = new ArrayList<>();
    
    private final StringJoiner nameSearchComponent = new StringJoiner(" ");
    
    private final Peva2FundResultBuilder resultBuilder;
            
    public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
        ImportPevaFundInfo ifi = new ImportPevaFundInfo(new ConfigPeva2FundProperties(),null, null, null);
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
    
	public ImportPevaFundInfo(ConfigPeva2FundProperties fundProperties, ApTypeService apTypeService,
			StorageService storageService, Peva2FundResultBuilder resultBuilder) {
		if (fundProperties != null) {
			this.fundProperties = fundProperties;
		} else {
			this.fundProperties = new ConfigPeva2FundProperties();
		}
		this.apTypeService = apTypeService;
		this.storageService = storageService;
		this.resultBuilder = resultBuilder;
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
		processInstitution(subsheet, partFundInfo, instInfo);
		processArchDesc(partFundInfo);
		processEvidenceNumber(primarySheet, subsheet, partFundInfo);
		processNadHeaderCommon(nadHeader, partFundInfo);
		processNadSheetSubNadSheetCommon(subsheet, partFundInfo);
		generateResult(subsheet, instInfo);
		
		if (nameSearchComponent.length()>0) {
			ApuSourceBuilder.addString(partFundInfo, CoreTypes.INT_NAME_INDEX, nameSearchComponent.toString()).setVisible(false);;			
		}
	}

	private void importPrimarySheet(NadPrimarySheet primarySheet) {
		var instInfo = getInstitutionInfo(primarySheet);
		var nadHeader = primarySheet.getHeader();
		var fullFundName = createFundName(instInfo, primarySheet);

		Apu apu = apusBuilder.createApu(fullFundName, ApuType.FUND, UUID.fromString(primarySheet.getId()));
		createTitlePart(apu, nadHeader);

		Part partFundInfo = ApuSourceBuilder.addPart(apu, CoreTypes.PT_FUND_INFO);
		processInstitution(primarySheet, partFundInfo, instInfo);
		processArchDesc(partFundInfo);
		processEvidenceNumber(primarySheet, null, partFundInfo);
		processNadHeaderCommon(nadHeader, partFundInfo);
		processNadSheetSubNadSheetCommon(primarySheet, partFundInfo);
		generateResult(primarySheet, instInfo);
		
		if (nameSearchComponent.length()>0) {
			ApuSourceBuilder.addString(partFundInfo, CoreTypes.INT_NAME_INDEX, nameSearchComponent.toString()).setVisible(false);			
		}
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
		if (fundProperties.isNameSearch("name")) {
			nameSearchComponent.add(nadSheet.getHeader().getName());
		}		
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
	
	/**
	 * Vytvori referenci na instituci a department
	 */
	private void processInstitution(NadSheet sheet, Part partFundInfo, InstitutionInfo instInfo) {
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instInfo.getUuid());
		if (fundProperties.isDepartments()) {
			var department = sheet.getDepartment();
			if (department != null) {
				var p2Department = codeListProvider.getCodeLists().getDepartment(department);
				if (p2Department!=null) {
					var text = p2Department.getNumber();
					var mapping = fundProperties.getDepartmentsMapping();
					if (mapping != null) {
						for(var map : mapping) {
							if (Objects.equals(map.getCode(), text)) {
								text = map.getText();
								break;
							}
						}
					}
					ApuSourceBuilder.addEnum(partFundInfo,"DEPARTMENT", text);
				}
			}
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
			primarySheetNumber = primarySheet.getEvidenceNumber();
			if (subsheet != null && subsheet.getNumber() != null) {
				subSheetNumber = subsheet.getNumber();
				ApuSourceBuilder.addString(partFundInfo, "CISLO_NAD",
						primarySheet.getEvidenceNumber() + "/" + subsheet.getNumber());
				nadNumber = primarySheet.getEvidenceNumber() + "_" + subsheet.getNumber();
				if (fundProperties.isNameSearch("CISLO_NAD")) {
					nameSearchComponent.add(primarySheet.getEvidenceNumber() + "/" + subsheet.getNumber());
				}
			} else {
				ApuSourceBuilder.addString(partFundInfo, "CISLO_NAD", primarySheet.getEvidenceNumber());
				nadNumber = primarySheet.getEvidenceNumber();
				if (fundProperties.isNameSearch("CISLO_NAD")) {
					nameSearchComponent.add(primarySheet.getEvidenceNumber());
				}
			}
		}
	}
	
    private void processNadHeaderCommon(NadHeader nadHeader, Part partFundInfo) {
    	var mark = nadHeader.getMark();
        if(mark!=null) {
        	ApuSourceBuilder.addString(partFundInfo, "FUND_MARK", mark);
			if (fundProperties.isNameSearch("FUND_MARK")) {
				nameSearchComponent.add(mark);
			}
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
	
	private void generateResult(NadSheet nadSheet, InstitutionInfo institutionInfo) {		
		if (resultBuilder!=null) {
			apusBuilder.getMainApu().setResult(resultBuilder.buildResult(nadSheet, institutionInfo));
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
			boolean faReferenced = false;
			for (var faUuid : findingAids.getFindingAid()) {
				if (fillFindingAids) {
					apusBuilder.addApuRef(partFundInfo, "FINDINGAID_REF", UUID.fromString(faUuid));
					faReferenced = true;
				}
				this.findingAids.add(faUuid);
			}
			if (faReferenced) {								
				if (fundProperties.isFindingAidFlag()||fundProperties.isNameSearch("ap.name")) {
					List<UUID> uuids = new ArrayList<>();
					// get all valid referenced finding aid uuids
					var faUuids = dataProvider.getValidFindingAids(UUID.fromString(nadSheet.getId()));					
					for (var faUuidStr : findingAids.getFindingAid()) {
						var faUuid = UUID.fromString(faUuidStr);
						for (var validFaUuid : faUuids) {
							if (faUuid.equals(validFaUuid)) {
								uuids.add(faUuid);								
							}
						}
					}
					
					if (fundProperties.isFindingAidFlag()&&!uuids.isEmpty()) {
						ApuSourceBuilder.addEnum(partFundInfo, "FINDINGAID_FLAG", "Ano", false);
					}
					
					if (fundProperties.isNameSearch("ap.name")) {
						for(var uuid:uuids) {
							var faInfo = dataProvider.getFindingAidInfo(uuid);
							if (faInfo != null) {
								nameSearchComponent.add(faInfo.getName());
							}	
						}												
					}					

				}
			}
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
							if (fundProperties.isTextOriginators()) {
								var existingEntities = dataProvider
										.getArchivalEntitiesByUuid(UUID.fromString(ipoi.getCamUUID()));
								// check existency of non peva entity in available state
								var exist = false;
								for (var entity : existingEntities) {
									if (entity.getEntityClass() != null
											&& !ImportPevaOriginator.ENTITY_CLASS.equals(entity.getEntityClass())
											&& entity.getStatus() == EntityStatus.AVAILABLE) {
										exist = true;
										break;
									}
								}
								if (!exist) {
									// elza entity not exist, generate text originator from peva
									fillTextOriginator(ipoi, partFundInfo);
									continue;
								}
							}
							apusBuilder.addApuRef(partFundInfo, CoreTypes.ORIGINATOR_REF,
									UUID.fromString(ipoi.getCamUUID()));
							camOriginators.add(ipoi.getCamUUID());
						} else {
							if (fundProperties.isTextOriginators()) {
								fillTextOriginator(ipoi, partFundInfo);
							}
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
	
	private void fillTextOriginator(ImportPevaOriginatorInfo ipoi, Part partFundInfo) {
		// add text description of originator
		var entityApu = ipoi.getApusBuilder().getMainApu();
		var text = entityApu.getName();
		if (StringUtils.isNotBlank(entityApu.getDesc())) {
			text += ", " + entityApu.getDesc();
		}
		ApuSourceBuilder.addString(partFundInfo, "ORIGINATOR_TEXT", text);
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
	
	// kvuli zpetne kompatibilite
	@Deprecated
	private void processEvidenceUnitProcedureOld(String name, EvidenceUnitProcedure evidenceUnitProcedure,
			StringJoiner sj) {
		if (evidenceUnitProcedure == null) {
			return;
		}
		StringJoiner sjInt = new StringJoiner(", ", name, "");
		String val = null;		
		val = processQuantity(evidenceUnitProcedure.getDamaged(), "Poškozeno: ");
		if (val != null) {
			sjInt.add(val);
		}
		val = processQuantity(evidenceUnitProcedure.getInventory(), "Inventarizováno: ");
		if (val != null) {
			sjInt.add(val);
		}
		val = processQuantity(evidenceUnitProcedure.getNotProcessed(), "Nezpracováno: ");
		if (val != null) {
			sjInt.add(val);
		}
		val = processQuantity(evidenceUnitProcedure.getProcessed(), "Zpracováno: ");
		if (val != null) {
			sjInt.add(val);
		}
		if (val != null) {
			sj.add(sjInt.toString());
		}
	}
	
	private void processEvidenceUnitProcedure(String name, EvidenceUnitProcedure evidenceUnitProcedure,
			StringJoiner sj) {
		if (evidenceUnitProcedure == null) {
			return;
		}
		if (fundProperties.isLengthOld()) {
			processEvidenceUnitProcedureOld(name, evidenceUnitProcedure, sj);
			return;
		}
		String val = null;
		StringJoiner sjInt = new StringJoiner(", ", name, "");
		val = processQuantity(evidenceUnitProcedure.getDamaged(), "Poškozeno: ");
		if (val != null) {
			sjInt.add(val);
		}
		val = processQuantity(evidenceUnitProcedure.getNotProcessed(), "Nezpracováno: ");
		if (val != null) {
			sjInt.add(val);
		}
		val = processQuantity(evidenceUnitProcedure.getProcessed(), "Zpracováno: ");
		if (val != null) {
			String val2 = processQuantity(evidenceUnitProcedure.getInventory(), " (z toho Inventarizováno: ");
			if (val2 != null) {
				val = val + val2 + ")";
			}
			sjInt.add(val);
		}
		if (val != null) {
			sj.add(sjInt.toString());
		}
	}
	
	private String processQuantity(Quantity quantity, String name) {
		if (quantity != null && quantity.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			return (name + DECIMAL_FORMAT.format(quantity.getAmount()) + getUnit(quantity));
		}
		return null;
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
			return " " + convertDigitalUnit(quantity.getDigitalUnit());
		}
		if (quantity.getLengthUnit() != null) {
			return " " + convertLengthUnit(quantity.getLengthUnit());
		}
		return "";
	}
	
	private String convertLengthUnit(LengthUnitType lengthUnit) {
		switch (lengthUnit) {
		case M:
			return "bm";
		case CM:
			return "cm";
		case MM:
			return "mm";
		case KM:
			return "km";
		case IN:
			return "in";
		case FT:
			return "ft";
		case YD:
			return "yd";
		case MI:
			return "mi";
		default:
			throw new IllegalArgumentException("Unknown LenghthUnitType "+lengthUnit);
		}
	}
	
	private String convertDigitalUnit(DigitalUnitType digitalUnit) {
		switch (digitalUnit) {
		case BYTE:
			return "B";
		case KB:
			return "kB";
		case MB:
			return "MB";
		case GB:
			return "GB";
		case TB:
			return "TB";
		default:
			throw new IllegalArgumentException("Unknown LenghthUnitType "+digitalUnit);
		}		
	}
	
	
	private void processPlacesOfOrigin(NadSheet nadSheet, Part partFundInfo) {
		if (!fundProperties.isPlacesOfOrigin()) {
			return;
		}
		var placesOfOrigin = nadSheet.getPlacesOfOrigin();
		if (fundProperties.isCamPlacesOfOrigin()) {
			List<UUID> uuids = placesOfOrigin.getPlaceOfOrigin().stream()
					.map(placeOfOrigin -> UUID.fromString(placeOfOrigin.getPlace())).collect(Collectors.toList());
			var unusedUuids = placesOfOrigin.getPlaceOfOrigin().stream().map(placeOfOrigin -> placeOfOrigin.getPlace())
					.collect(Collectors.toCollection(HashSet::new));
			var archEntitySources = this.dataProvider.getArchEntityApuSources(uuids, ImportPevaGeo.ENTITY_CLASS);
			for (var archEntitySource : archEntitySources) {
				var entityUuid = archEntitySource.getUuid().toString();
				unusedUuids.remove(entityUuid);
				geos.add(entityUuid);
				if ((archEntitySource.getStatus() != EntityStatus.ACCESSIBLE
						&& archEntitySource.getStatus() != EntityStatus.AVAILABLE)) {
					// entita neni dostupna nebo neni urcena k publikaci
					continue;
				}
				var dataDir = storageService.getApuDataDir(archEntitySource.getDataDir());
				var ipgi = readPevaGeo(dataDir);
				if (ipgi.getCamUUID() != null) {
					apusBuilder.addApuRef(partFundInfo, "ORIG_PLACES_REF", UUID.fromString(ipgi.getCamUUID()));
					camGeos.add(ipgi.getCamUUID());
				}
			}			
			geos.addAll(unusedUuids);
		} else {
			placesOfOrigin.getPlaceOfOrigin().forEach(po -> {
				var placeId = po.getPlace();
				apusBuilder.addApuRef(partFundInfo, "ORIG_PLACES_REF", UUID.fromString(placeId));
				geos.add(placeId);
			});
		}
	}

	private ImportPevaGeoInfo readPevaGeo(Path dataDir) {		
		var ipgi = new ImportPevaGeoInfo(apTypeService, codeListProvider);		
		var geoPath = ImportPevaGeo.getPevaGeoFile(dataDir);
		if (geoPath.isEmpty()) {
			log.error("Fail to read peva originator, not exist, path={}", dataDir);
			throw new IllegalStateException();
		}
		try {
			ipgi.importGeo(geoPath.get(), dataProvider);
		} catch (IOException e) {
			log.error("Fail to read peva originator", e);
			throw new IllegalStateException();
		} catch (JAXBException e) {
			log.error("Fail to read peva originator", e);
			throw new IllegalStateException();
		}
		return ipgi;
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
		
		if (fundProperties.isThematicEvidenceGroupsVirtual()) {			
			var roots = new HashSet<Peva2ThematicEvidenceGroup>();
			for(var groupUUID:nadSheet.getGroups().getGroup()) {
				var virtualGroup = codeListProvider.getCodeLists().getThematicGroupProvider().getVirtualGroup(groupUUID);
				if (virtualGroup!=null) {
					roots.add(virtualGroup);
				}				
			}
			for(var root:roots) {
				ApuSourceBuilder.addEnum(partFundInfo, "FUND_THEMATIC_GROUP_VIRT", root.getName(), false);	
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

	public List<String> getGeoIds() {
		return geos;
	}
	
	public List<String> getCAMGeoIds() {
		return camGeos;
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

	public String getPrimarySheetNumber() {
		return primarySheetNumber;
	}

	public Integer getSubSheetNumber() {
		return subSheetNumber;
	}
	
	
	
}
