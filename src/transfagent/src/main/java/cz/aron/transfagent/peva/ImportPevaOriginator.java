package cz.aron.transfagent.peva;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigPeva2OriginatorProperties;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.service.importfromdir.ImportOriginatorService.OriginatorImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class ImportPevaOriginator implements OriginatorImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaOriginator.class);
	
	public static final String ENTITY_CLASS = "pevaoriginator";
	
	public static final String PREFIX_DASH = ENTITY_CLASS + "-";
	
	private final ApTypeService apTypeService;
	
	private final Peva2CodeListProvider codeLists;
	
	private final DatabaseDataProvider contextDataProvider;
	
	private final ConfigurationLoader configurationLoader;
	
	private final StorageService storageService;
		
	private final ArchivalEntityService archivalEntityService;
	
	private final boolean sendToAron;

	public ImportPevaOriginator(ApTypeService apTypeService, Peva2CodeListProvider codeLists,
			DatabaseDataProvider contextDataProvider, ConfigurationLoader configurationLoader,
			StorageService storageService, ArchivalEntityService archivalEntityService, ConfigPeva2 config) {
		this.apTypeService = apTypeService;
		this.codeLists = codeLists;
		this.contextDataProvider = contextDataProvider;
		this.configurationLoader = configurationLoader;
		this.storageService = storageService;
		this.archivalEntityService = archivalEntityService;		
		ConfigPeva2OriginatorProperties originatorProperties = config.getOriginatorProperties(); 
		sendToAron = originatorProperties!=null?!originatorProperties.isDontSend():true;		
		log.info("PevaOriginator sendToAron={}", sendToAron);
	}

	@Override
	public ImportResult processPath(Path path) {

		if (!path.getFileName().toString().startsWith(PREFIX_DASH)) {
			return ImportResult.UNSUPPORTED;
		}

		var originatorXml = getPevaOriginatorFile(path);
		if (originatorXml.isEmpty()) {
			log.warn("Directory not contail pevaoriginator- file. Directory {}", path);
			return ImportResult.FAIL;
		}

		if (processOriginator(path, originatorXml.get())) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.UNSUPPORTED;
		}
	}

	/**
	 * Najde v adresari soubor zacinajici pevaoriginator- a koncici .xml
	 * @param path cesta k adresari
	 * @return POptional<Path>
	 */
	public static Optional<Path> getPevaOriginatorFile(Path path) {
		List<Path> xmls;
		try (var stream = Files.list(path)) {
			xmls = stream.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith(PREFIX_DASH)
					&& f.getFileName().toString().endsWith(".xml")).collect(Collectors.toList());
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		}

		return xmls.stream().filter(
				p -> p.getFileName().toString().startsWith(PREFIX_DASH) && p.getFileName().toString().endsWith(".xml"))
				.findFirst();
	}

	private boolean processOriginator(Path dir, Path originatorXml) {
		ApuSourceBuilder apuSourceBuilder = null;
		var ipfi = new ImportPevaOriginatorInfo(apTypeService, codeLists);
		try {
			apuSourceBuilder = ipfi.importOriginator(originatorXml, contextDataProvider);
			try (var os = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
				apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
			}
			var dataDir = storageService.moveToDataDir(dir);
			
			boolean send = sendToAron;			
			archivalEntityService.createOrUpdateArchivalEntity(dataDir, dir,
					UUID.fromString(apuSourceBuilder.getMainApu().getUuid()), ENTITY_CLASS, send);
		} catch (Exception e) {
			log.error("Fail to import originator", e);
			throw new IllegalStateException(e);
		}
		return true;
	}

	/**
	 * Reimportuje puvodce z existujicich dat v adresari (serializovana odpoved z pevy)
	 */
	@Override
	public Result reimport(ApuSource apuSource, ArchivalEntity archivalEntity, Path apuPath) {
		if (!apuPath.getFileName().toString().startsWith(PREFIX_DASH)) {
			return ReimportProcessor.Result.UNSUPPORTED;
		}
		var fileName = PREFIX_DASH + apuSource.getUuid() + ".xml";
		var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
		ApuSourceBuilder apuSourceBuilder;
		var ipoi = new ImportPevaOriginatorInfo(apTypeService, codeLists);
		try {			
			apuSourceBuilder = ipoi.importOriginator(apuPath.resolve(fileName), contextDataProvider);
			
			// compare original apusrc.xml and newly generated
			var apuSrcXmlPath = apuPath.resolve(StorageService.APUSRC_XML);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			apuSourceBuilder.build(baos, new ApuValidator(configurationLoader.getConfig()));
			byte [] newContent = baos.toByteArray();
			if (StorageService.isContentEqual(apuSrcXmlPath, newContent)) {
				return ReimportProcessor.Result.NOCHANGES;
			}
			Files.write(apuSrcXmlPath, newContent);			
			boolean send = sendToAron;
			archivalEntityService.createOrUpdateArchivalEntity(apuPath, apuPath,
					UUID.fromString(apuSourceBuilder.getMainApu().getUuid()), ENTITY_CLASS, send);
		} catch (Exception e) {
			log.error("Fail to reimport {}, dir={}", fileName, apuDir, e);
			return ReimportProcessor.Result.FAILED;
		}
		log.info("Originator id={}, uuid={} reimported", archivalEntity.getId(), archivalEntity.getUuid());
		return ReimportProcessor.Result.REIMPORTED;
	}

}
