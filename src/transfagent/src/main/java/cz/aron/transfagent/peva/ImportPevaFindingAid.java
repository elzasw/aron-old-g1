package cz.aron.transfagent.peva;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.FindingAidAuthor;
import cz.aron.peva2.wsdl.FindingAidCopy;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.peva.ImportPevaFindingAidInfo.PevaFundNotExist;
import cz.aron.transfagent.peva.dao.FindingAidDaoImporter;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.DaoFileStore3Service;
import cz.aron.transfagent.service.DaoImportService;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.ImportProtocol;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.DaoImportService.DaoSource;
import cz.aron.transfagent.service.DaoImportService.DaoSourceRef;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.service.importfromdir.ImportFindingAidService.FindingAidImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class ImportPevaFindingAid implements FindingAidImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaFindingAid.class);
	
	private final String PEVA_FINDING_AID = "pevafa";
	   
	private final String PEVA_FINDING_AID_DASH = PEVA_FINDING_AID + "-";
	
	private final StorageService storageService;
	
	private final FindingAidService findingAidService;
	
	private final FindingAidRepository findingAidRepository;
	
	private final FundRepository fundRepository;
	
	private final InstitutionRepository institutionRepository;
	
	private final DatabaseDataProvider databaseDataProvider;
	
	private final ConfigurationLoader configurationLoader;
	
	private final TransactionTemplate transactionTemplate;
	
	private final Peva2CodeListProvider codeListProvider;
	
	private final ConfigPeva2 configPeva2;
	
	private final DaoImportService daoImportService;
	
	private final DaoFileStore3Service daoFileStore3Service;
	
	private final List<FindingAidDaoImporter> daoImporters;
	
	public ImportPevaFindingAid(StorageService storageService, FindingAidService findingAidService,
			FindingAidRepository findingAidRepository, FundRepository fundRepository,
			InstitutionRepository institutionRepository, DatabaseDataProvider databaseDataProvider,
			ConfigurationLoader configurationLoader, TransactionTemplate transactionTemplate,
			Peva2CodeListProvider codeListProvider, ConfigPeva2 configPeva2,DaoImportService daoImportService,
            @Nullable DaoFileStore3Service daoFileStore3Service,
            List<FindingAidDaoImporter> daoImporters) {
		this.storageService = storageService;
		this.findingAidService = findingAidService;
		this.findingAidRepository = findingAidRepository;
		this.fundRepository = fundRepository;
		this.institutionRepository = institutionRepository;
		this.databaseDataProvider = databaseDataProvider;
		this.configurationLoader = configurationLoader;
		this.transactionTemplate = transactionTemplate;
		this.codeListProvider = codeListProvider;
		this.configPeva2 = configPeva2;
		this.daoImportService = daoImportService;
		this.daoFileStore3Service = daoFileStore3Service;
		this.daoImporters = daoImporters;
	}

	@Override
	public ImportResult processPath(Path path) {

		if (!path.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)) {
			return ImportResult.UNSUPPORTED;
		}

		List<Path> xmls;
		try (var stream = Files.list(path)) {
			xmls = stream
					.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)
							&& f.getFileName().toString().endsWith(".xml"))
					.collect(Collectors.toList());
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		}

		var pevaFindingAidXml = xmls.stream().filter(p -> p.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)
				&& p.getFileName().toString().endsWith(".xml")).findFirst();

		if (pevaFindingAidXml.isEmpty()) {
			log.warn("Directory not contail pevafund-* file. Directory {}", path);
			return ImportResult.FAIL;
		}

		var findingaidXmlPath = pevaFindingAidXml.get();
		var fileName = findingaidXmlPath.getFileName().toString();
		var tmp = fileName.substring(PEVA_FINDING_AID_DASH.length());
		var findingAidCode = tmp.substring(0, tmp.length() - ".xml".length());

		if (transactionTemplate.execute(t -> processFindingAidPeva(path, pevaFindingAidXml.get(), findingAidCode))) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.UNSUPPORTED;
		}
	}	
	
	private boolean processFindingAidPeva(Path dir, Path findingAidXml, String findingAidCode) {
		
		var protocol = new ImportProtocol(dir);
        protocol.add("Zahájení importu");
		
        var ifai = new ImportPevaFindingAidInfo(codeListProvider.getCodeLists(), configPeva2.getFindingAidProperties());
        ApuSourceBuilder builder;
        try {
            builder = ifai.importFindingAidInfo(findingAidXml, databaseDataProvider);
        } catch (IOException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        } catch (PevaFundNotExist pfneEx) {        	
        	if (configPeva2.getFindingAidProperties().isImportMissingFund()) {
        		createFundCommands(pfneEx);        		
        	}        	
        	throw new IllegalStateException(pfneEx);
        }
        
        var institutionCode = ifai.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
            protocol.add("The entry Institution code={" + institutionCode + "} must exist.");
            throw new IllegalStateException("The entry Institution code={" + institutionCode + "} must exist.");
        }

        var funds = new ArrayList<Fund>();
        for (var fundUUID:ifai.getFundUUIDs()) {
        	var fund = fundRepository.findByUuidAndInstitution(fundUUID, institution);
        	if (fund == null) {
                protocol.add("The entry Fund code={" + findingAidCode + "} must exist.");
                //throw new IllegalStateException("The entry Fund code={" + findingAidCode + "} must exist.");
            }
        	funds.add(fund);
        }
        var findingAid = findingAidRepository.findByUuid(ifai.getFindingAidUUID());
        UUID findingaidUuid, apusourceUuid;
        if (findingAid != null) {
            findingaidUuid = findingAid.getUuid();
            apusourceUuid = findingAid.getApuSource().getUuid();
        } else {
            findingaidUuid = ifai.getFindingAidUUID();
            apusourceUuid = UUID.randomUUID();
        }

        processFindingAidCopies(builder, findingaidUuid);
        processFindingAidAuthors(builder, ifai.getAuthorUUIDs());
        List<Path> attachments = readAllAttachments(dir, builder);
        Collection<DaoSource> daos = addDaos(builder,ifai.getInstitutionCode(),ifai.getFindingAidCode(),ifai.getFindingAidPartNumber());
        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
            builder.setUuid(apusourceUuid);
            builder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
            protocol.add("Chyba " + ioEx.getMessage());
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

		Path dataDir;
		List<Path> relativeAttachments;
		try {
			dataDir = storageService.moveToDataDir(dir);
			protocol.setLogPath(storageService.getDataPath().resolve(dataDir));
			// change directory of attachments
			relativeAttachments = createRelativeAttachmentPaths(attachments, dataDir);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

        boolean send = !configPeva2.getFindingAidProperties().isDontSend();
        transactionTemplate.executeWithoutResult(t -> {
            FindingAid fa;
            if (findingAid == null) {
                fa = findingAidService.createFindingAid(findingAidCode, funds, institution, dataDir, dir, builder,
                                                   relativeAttachments,
                                                   configPeva2.getFundProperties().isAggregateAttachments(), send);
            } else {
                findingAidService.updateFindingAid(findingAid, dataDir, dir, builder, relativeAttachments,
                                                   configPeva2.getFundProperties().isAggregateAttachments(), send);
                fa = findingAid;
            }
            if (daoFileStore3Service != null || !daoImporters.isEmpty()) {
                daoImportService.updateDaos(fa.getApuSource(), daos);
            }
        });
    	return true;
	}
	
	
	private List<Path> createRelativeAttachmentPaths(List<Path> attachments, Path dataDir) {
		if (attachments != null) {
			return attachments.stream().map(p -> dataDir.resolve(p.getFileName())).collect(Collectors.toList());
		} else {
			return attachments;
		}
	}
	
	/*
	 * Prida nazvy stejnopisu
	 */
	private void processFindingAidCopies(ApuSourceBuilder builder, UUID findingaidUuid) {
		var findingAidCopies = readFindingAidCopies(findingaidUuid.toString());
		if (!findingAidCopies.isEmpty()) {
			var apu = builder.getMainApu();
			Part partInfo = ApuSourceBuilder.getFirstPart(apu, CoreTypes.PT_FINDINGAID_INFO);			
			for (var findingAidCopy : findingAidCopies) {
				var institutionInfo = databaseDataProvider.getInstitutionApu(findingAidCopy.getInstitution().getExternalId());				
				ApuSourceBuilder.addString(partInfo, "FINDINGAID_COPY", institutionInfo.getName() + ": " + findingAidCopy.getEvidenceNumber());
			}
		}
	}
	
	/**
	 * Prida jmena autoru pomucky jako text
	 */
	private void processFindingAidAuthors(ApuSourceBuilder builder, List<String> authorUUIDs) {
		if (!authorUUIDs.isEmpty()) {
			var apu = builder.getMainApu();
			Part partInfo = ApuSourceBuilder.getFirstPart(apu, CoreTypes.PT_FINDINGAID_INFO);
			StringJoiner sj = new StringJoiner(", ");
			for(var faAuthor:readFindingAidAuthors(authorUUIDs)) {
				sj.add(faAuthor.getName());
			}
			if (sj.length()>0) {				
				ApuSourceBuilder.addString(partInfo, "FINDINGAID_AUTHOR_TEXT", sj.toString());
			}
		}		
	}
	
	private List<Path> readAllAttachments(Path dir, ApuSourceBuilder builder) {
		// get root apu
		var apu = builder.getMainApu();
		List<Path> attachments = new ArrayList<>();

		try (var stream = Files.list(dir)) {
			stream.forEach(f -> {
				if (Files.isRegularFile(f) && !f.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)
						&& !"protokol.txt".equals(f.getFileName().toString())
						&& !StorageService.APUSRC_XML.equals(f.getFileName().toString())) {

					String mimetype = null;
					try {
						mimetype = this.storageService.detectMimetype(f);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					builder.addAttachment(apu, f.getFileName().toString(), mimetype);
					attachments.add(f);
				}
			});
		} catch (IOException e) {
			log.error("Fail to read attachments from directory {}", dir);
			throw new UncheckedIOException(e);
		}
		return attachments;
	}

	private List<FindingAidCopy> readFindingAidCopies(String findingAidId) {
		var ret = new ArrayList<FindingAidCopy>();
		var dir = storageService.getInputPath().resolve("facopies").resolve(findingAidId);
		if (Files.isDirectory(dir)) {
			try (var stream = Files.list(dir)) {
				stream.forEach(f -> {
					if (Files.isRegularFile(f)) {
						try {
							var facResp = Peva2XmlReader.unmarshalGetFindingAidCopyResponse(f);
							ret.add(facResp.getFindingAidCopy());
						} catch (IOException | JAXBException e) {
							log.error("Fail to deserialize finding aid copy {}", f, e);
							throw new IllegalStateException(e);
						}
					}
				});
			} catch (IOException e) {
				// ignore
				log.error("Fail to read directory containing finding aid copies {}", dir, e);
			}
		}
		return ret;
	}
	
	private List<FindingAidAuthor> readFindingAidAuthors(List<String> authorUUIDs) {
		var ret = new ArrayList<FindingAidAuthor>();
		var dir = storageService.getInputPath().resolve("faauthors");
		for (var authorUUID : authorUUIDs) {
			try {
				var faaResp = Peva2XmlReader.unmarshalGetFindingAidAuthorResponse(dir.resolve(authorUUID + ".xml"));
				ret.add(faaResp.getFindingAidAuthor());
			} catch (FileNotFoundException|NoSuchFileException fnfEx) {
				// ignore
				log.warn("Missing author {}", authorUUID);
			} catch (IOException | JAXBException e) {
				log.error("Fail to read author {}", authorUUID, e);
			}
		}
		return ret;
	}	

	@Override
	public Result reimport(ApuSource apuSource, FindingAid findingAid, Path apuPath) {

		if (!apuPath.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)) {
			return ReimportProcessor.Result.UNSUPPORTED;
		}

		var relativeDataDir = Paths.get(apuSource.getDataDir());
		var fileName = PEVA_FINDING_AID_DASH + findingAid.getCode() + ".xml";
		ApuSourceBuilder builder;
		try {
			var ifai = new ImportPevaFindingAidInfo(codeListProvider.getCodeLists(),
					configPeva2.getFindingAidProperties());
			builder = ifai.importFindingAidInfo(apuPath.resolve(fileName), databaseDataProvider);
			processFindingAidCopies(builder, findingAid.getUuid());
			processFindingAidAuthors(builder, ifai.getAuthorUUIDs());
			List<Path> attachments = createRelativeAttachmentPaths(readAllAttachments(apuPath, builder),
					relativeDataDir);
			Collection<DaoSource> daos = addDaos(builder,ifai.getInstitutionCode(),ifai.getFindingAidCode(),ifai.getFindingAidPartNumber());

			// compare original apusrc.xml and newly generated
			var apuSrcXmlPath = apuPath.resolve(StorageService.APUSRC_XML);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			builder.build(baos, new ApuValidator(configurationLoader.getConfig()));
			byte[] newContent = baos.toByteArray();
			if (StorageService.isContentEqual(apuSrcXmlPath, newContent)) {
				return ReimportProcessor.Result.NOCHANGES;
			}
			Files.write(apuSrcXmlPath, newContent);

			boolean send = !configPeva2.getFindingAidProperties().isDontSend();
			transactionTemplate.executeWithoutResult(c -> {
				findingAidService.updateFindingAid(findingAid, relativeDataDir, apuPath, builder, attachments,
						configPeva2.getFundProperties().isAggregateAttachments(), send);
				if (daoFileStore3Service != null||!daoImporters.isEmpty()) {
				    this.daoImportService.updateDaos(apuSource, daos);
				}
			});
		} catch (Exception e) {
			log.error("Fail to process downloaded {}, dir={}", fileName, apuPath, e);
			return ReimportProcessor.Result.FAILED;
		}

		log.info("FindingAid id={}, uuid={} reimported", findingAid.getId(), findingAid.getUuid());
		return ReimportProcessor.Result.REIMPORTED;
	}

	private void createFundCommands(PevaFundNotExist fne) {
    	Path commandsInputDir = storageService.getInputPath().resolve("commands");
		for (var fundId : fne.getFundIds()) {
    		var commandFile = commandsInputDir.resolve(Peva2ImportFunds.createCommand(fne.getInstitutionUUID(), fundId));
    		if (!Files.isRegularFile(commandFile)) {
    			try {
    				Files.createFile(commandFile);
    			} catch (IOException e) {
    				log.error("Fail to create command file {}",commandFile,e);
    			}
    		}
    	}    	
    }

    private Collection<DaoSource> addDaos(ApuSourceBuilder apuSourceBuilder, String archiveCode, String findingAidCode, String findingAidPartNumber) {
        if (daoFileStore3Service != null) {
            var handle = daoFileStore3Service.getFindingAidDaoHandle(archiveCode, findingAidCode);
            if (handle != null) {
                var mainUUID = UUID.fromString(apuSourceBuilder.getMainApu().getUuid());
                var daoRefs = new HashSet<DaoSourceRef>();                
                daoRefs.add(new DaoSourceRef(mainUUID,handle));                
                var daoSource = new DaoSource(daoFileStore3Service.getName(),daoRefs);
                ApuSourceBuilder.addDao(apuSourceBuilder.getMainApu(), mainUUID);
                var part = ApuSourceBuilder.getFirstPart(apuSourceBuilder.getMainApu(), CoreTypes.PT_FINDINGAID_INFO);
                ApuSourceBuilder.addEnum(part, "DIGITAL", "Ano", false);
                return Collections.singletonList(daoSource);
            }
        }

        // TODO funguje pouze pro jedno DAO
        for(var importer:daoImporters) {
        	var handle = importer.getFindingAidDaoHandle(archiveCode, findingAidCode, findingAidPartNumber);
        	if (handle != null) {
        		var mainUUID = UUID.fromString(apuSourceBuilder.getMainApu().getUuid());
                var daoRefs = new HashSet<DaoSourceRef>();                
                daoRefs.add(new DaoSourceRef(mainUUID,handle));                
                var daoSource = new DaoSource(importer.getName(),daoRefs);
                ApuSourceBuilder.addDao(apuSourceBuilder.getMainApu(), mainUUID);
                var part = ApuSourceBuilder.getFirstPart(apuSourceBuilder.getMainApu(), CoreTypes.PT_FINDINGAID_INFO);
                ApuSourceBuilder.addEnum(part, "DIGITAL", "Ano", false);
                return Collections.singletonList(daoSource);
        	}
        }        
        return Collections.emptyList();
    }

}
