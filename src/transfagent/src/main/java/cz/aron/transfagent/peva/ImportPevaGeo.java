package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigPeva2GeoProperties;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportGeoService.GeoImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class ImportPevaGeo implements GeoImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaOriginator.class);
	
	public static final String ENTITY_CLASS = "pevageo";
	
	public static final String PREFIX_DASH = ENTITY_CLASS + "-";
	
	private final ApTypeService apTypeService;
	
	private final Peva2CodeListProvider codeLists;
	
	private final DatabaseDataProvider contextDataProvider;
	
	private final ConfigurationLoader configurationLoader;
	
	private final StorageService storageService;
		
	private final ArchivalEntityService archivalEntityService;
	
	private final boolean sendToAron;
	
	public ImportPevaGeo(ApTypeService apTypeService, Peva2CodeListProvider codeLists,
			DatabaseDataProvider contextDataProvider, ConfigurationLoader configurationLoader,
			StorageService storageService, ArchivalEntityService archivalEntityService,
			ConfigPeva2 config) {
		this.apTypeService = apTypeService;
		this.codeLists = codeLists;
		this.contextDataProvider = contextDataProvider;
		this.configurationLoader = configurationLoader;
		this.storageService = storageService;
		this.archivalEntityService = archivalEntityService;
		ConfigPeva2GeoProperties originatorProperties = config.getGeoProperties(); 
		sendToAron = originatorProperties!=null?!originatorProperties.isDontSend():true;		
		log.info("PevaGeo sendToAron={}", sendToAron);
	}

	@Override
	public ImportResult processPath(Path path) {
		if (!path.getFileName().toString().startsWith(PREFIX_DASH)) {
			return ImportResult.UNSUPPORTED;
		}
		
		List<Path> xmls;
		try (var stream = Files.list(path)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) &&  f.getFileName().toString().startsWith(PREFIX_DASH)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
		
		var originatorXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIX_DASH)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();
		
		if (originatorXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", path);
            return ImportResult.FAIL;
		}
		
		if (processGeo(path,originatorXml.get())) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.UNSUPPORTED;	
		}
	}
	
	private boolean processGeo(Path dir, Path originatorXml) {
		ApuSourceBuilder apuSourceBuilder = null;
		var ipfi = new ImportPevaGeoInfo(apTypeService, codeLists);
		try {
			apuSourceBuilder = ipfi.importGeo(originatorXml, contextDataProvider);
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

	@Override
	public Result reimport(ApuSource apuSource) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
