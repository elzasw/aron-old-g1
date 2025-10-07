package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.peva2.wsdl.GetGeoObjectRequest;
import cz.aron.peva2.wsdl.GetGeoObjectResponse;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.IdProjection;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.service.ArchivalEntityImportService.ArchivalEntityImporter;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2GeoEntityImporter implements ArchivalEntityImporter {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2GeoEntityImporter.class);
	
	private final PEvA2Connection peva2;
	
	private final StorageService storageService;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final ArchivalEntityService archivalEntityService;
	
	private final Peva2CodeListProvider codeListProvider;
	
	private final ApTypeService apTypeService;
	
	private final ConfigurationLoader configurationLoader;
	
	private final DatabaseDataProvider databaseDataProvider;
	
	private final boolean insertToCoreQueue;

	public Peva2GeoEntityImporter(PEvA2Connection peva2, StorageService storageService,
			ArchivalEntityRepository archivalEntityRepository, ArchivalEntityService archivalEntityService,
			Peva2CodeListProvider codeListProvider, ApTypeService apTypeService,
			ConfigurationLoader configurationLoader, DatabaseDataProvider databaseDataProvider,
			ConfigPeva2 configPeva2) {
		super();
		this.peva2 = peva2;
		this.storageService = storageService;
		this.archivalEntityRepository = archivalEntityRepository;
		this.archivalEntityService = archivalEntityService;
		this.codeListProvider = codeListProvider;
		this.apTypeService = apTypeService;
		this.configurationLoader = configurationLoader;
		this.databaseDataProvider = databaseDataProvider;
		if (configPeva2.getGeoProperties()!=null&&configPeva2.getGeoProperties().isDontSend()) {
			insertToCoreQueue = false;
		} else {
			insertToCoreQueue = true;
		}
	}

	@Override
	public List<String> importedClasses() {
		return Collections.singletonList(ImportPevaGeo.ENTITY_CLASS);
	}

	@Override
	public boolean importEntity(IdProjection id) {
		var ret = new MutableBoolean();
		var archivalEntityId = id.getId();
		var archivalEntity = archivalEntityRepository.findById(archivalEntityId);
		archivalEntity.ifPresentOrElse(ae -> {
			downloadEntity(ae.getUuid());
			ret.setTrue();
		}, () -> {
			ret.setFalse();
		});		
		return ret.getValue();
	}
	
	private void downloadEntity(UUID uuid) {
		Path tempDir = null;
		try {
			tempDir = storageService.createTempDir(ImportPevaGeo.PREFIX_DASH + uuid.toString());
			var goResp = downloadEntity(uuid, tempDir);
			ApuSourceBuilder apuSourceBuilder = null;
			var ipfi = new ImportPevaGeoInfo(apTypeService, codeListProvider);
			try {
				apuSourceBuilder = ipfi.importGeo(goResp, databaseDataProvider);
				try (var os = Files.newOutputStream(tempDir.resolve("apusrc.xml"))) {
					apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
				}
				var dataDir = storageService.moveToDataDir(tempDir);
				archivalEntityService.createOrUpdateArchivalEntity(dataDir, tempDir,
						UUID.fromString(apuSourceBuilder.getMainApu().getUuid()), ImportPevaGeo.ENTITY_CLASS, insertToCoreQueue);
			} catch (SOAPFaultException sfEx) {
				deleteTempDir(tempDir);
				if (Peva2Constants.DELETED_OBJECT.equals(sfEx.getMessage())) {
					log.info("Geo entity {} is in deleted state", uuid);
					archivalEntityService.entityNotAvailable(uuid);
				} else {
					log.error("Fail to download geo entity, uuid={}", uuid, sfEx);
				}
			} catch (Exception e) {
				log.error("Fail to import geo entity", e);
				throw new IllegalStateException(e);
			}
		} catch (Exception e) {
			deleteTempDir(tempDir);
			log.error("Fail to download geo entity, uuid={}", uuid, e);
			throw new IllegalStateException(e);
		}
	}
	
	private void deleteTempDir(Path tempDir) {
		try {
			if (tempDir != null) {
				FileSystemUtils.deleteRecursively(tempDir);
			}
		} catch (IOException e1) {
			log.error("Fail to delete directory", e1);
		}	
	}
	
	private GetGeoObjectResponse downloadEntity(UUID uuid, Path tempDir) {		
		var ggoReq = new GetGeoObjectRequest();
		ggoReq.setId(uuid.toString());				
		var ggoResp = peva2.getPeva().getGeoObject(ggoReq);
		try {
			Peva2XmlReader.marshalGetGeoObjectResponse(ggoResp, tempDir.resolve(ImportPevaGeo.PREFIX_DASH+uuid+".xml"));
		} catch (JAXBException e) {
			log.error("Fail to store geo object xml to path={}, uuid={}", tempDir, uuid);
			throw new IllegalStateException();
		}
		return ggoResp;
	}

	@Override
	public Result reimport(ApuSource apuSource) {
		return Result.UNSUPPORTED;
	}

}
