package cz.aron.transfagent.elza;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.CommandProcessor;
import cz.aron.transfagent.service.CommandProcessorContext;
import cz.aron.transfagent.service.CommandProcessorException;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Component
public class ImportArchDescRefreshEntitiesCommand implements CommandProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(ImportArchDescRefreshEntitiesCommand.class);
	
	public static final String PREFIX_DASH = "archdescaprefresh-";
	
	private final ArchDescRepository archDescRepository;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final ArchivalEntityService archivalEntityService;
	
	private final StorageService storageService;
	
	private final DatabaseDataProvider databaseDataProvider;
	
	private final ApTypeService apTypeService;
	
	private final ConfigurationLoader configurationLoader;
	
	private final ConfigElza configElza;
	
	private final ImportApResultBuilder resultBuilder;

	public ImportArchDescRefreshEntitiesCommand(ArchDescRepository archDescRepository,
			ArchivalEntityRepository archivalEntityRepository, ArchivalEntityService archivalEntityService,
			StorageService storageService, DatabaseDataProvider databaseDataProvider, ApTypeService apTypeService,
			ConfigElza configElze, ConfigurationLoader configurationLoader, @Nullable ImportApResultBuilder resultBuilder) {
		this.archDescRepository = archDescRepository;
		this.archivalEntityRepository = archivalEntityRepository;
		this.archivalEntityService = archivalEntityService;
		this.storageService = storageService;
		this.databaseDataProvider = databaseDataProvider;
		this.apTypeService = apTypeService;
		this.configurationLoader = configurationLoader;
		this.configElza = configElze;
		this.resultBuilder = resultBuilder;
	}
	
	@Override
	public void processCommand(Path path, CommandProcessorContext commandContext) {
		
		var fileName = path.getFileName().toString();
		var idStr = fileName.substring(PREFIX_DASH.length());
		int id = -1;
		try {
			id = Integer.parseInt(idStr);
		} catch (NumberFormatException e) {
			log.error("Fail to parse archdesc id {}", idStr, e);
		}		
		refreshAps(id);
		try {
			Files.delete(path);
		} catch (IOException e) {
			log.error("Fail to delete file {}",e);
			throw new CommandProcessorException(e);
		}		

	}

	@Override
	public String getPrefix() {
		return PREFIX_DASH;
	}
	
	
	private void refreshAps(int archdescId) {
		var archDesc = archDescRepository.findByIdWithApuSource(archdescId);
		if (archDesc == null) {
			log.error("ArchDesc id {} not found", archdescId);
			return;
		}
		var dataDir = storageService.getApuDataDir(archDesc.getApuSource().getDataDir());
		Path inputFile = null;
		try (var stream = Files.list(dataDir)) {
			var tmp =  stream.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("archdesc")
					&& f.getFileName().toString().endsWith(".xml")).findFirst();
			if (tmp.isEmpty()) {
				log.warn("Directory not contain archdesc- file. Directory {}", dataDir);
				return;
			} else {
				inputFile = tmp.get();
			}
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		}				
		ElzaXmlReader elzaXmlReader = null;
		try (InputStream is = Files.newInputStream(inputFile)) {
			elzaXmlReader = ElzaXmlReader.read(is);
		} catch (Exception e) {
			log.error("Fail to read archdesc source xml from {}", inputFile, e);
			return;
		}
				
		var relatedEntities = archivalEntityRepository.findRelatedToApuSourceId(archDesc.getApuSource().getId());
		for(var relatedEntity:relatedEntities) {
			if (!relatedEntity.isDownload()) {
				var uuid = relatedEntity.getUuid().toString();
				var accessPoint = elzaXmlReader.findAccessPointByUUID(uuid);
				if (accessPoint!=null) {
					ApuSourceBuilder apuSourceBuilder;
					final var importAp = new ImportAp(apTypeService, new ElzaNameBuilder(apTypeService), resultBuilder, configElza.isOriginatorFlag());
					try {
						apuSourceBuilder = importAp.importAp(elzaXmlReader, relatedEntity.getUuid(), databaseDataProvider);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
			            apuSourceBuilder.build(baos, new ApuValidator(configurationLoader.getConfig()));
			            byte [] newContent = baos.toByteArray();			            
			            var apDataDir = storageService.getApuDataDir(relatedEntity.getApuSource().getDataDir());
			            var apApuSrcXml = apDataDir.resolve("apusrc.xml");			            
			            if (!StorageService.isContentEqual(apApuSrcXml, newContent)) {
			            	archivalEntityService.markEntityForDownloadAndReimport(relatedEntity.getUuid());
			            }			            			            
					} catch (IOException e) {
						log.error("Fail to compare new and existing access point {}", uuid, e);
					} catch (JAXBException e) {
						log.error("Fail to read existing access point {}", uuid, e);
					}	
				}	
			}
		}
	}
	
	public void scheduleRefresh(int archivalEntityId) {		
		var commandPath = storageService.getInputPath().resolve("commands").resolve(PREFIX_DASH + archivalEntityId);
		try {
			Files.createFile(commandPath);
		} catch (IOException e) {
			log.error("Fail to touch command file {}", commandPath, e);
		}
	}

}
