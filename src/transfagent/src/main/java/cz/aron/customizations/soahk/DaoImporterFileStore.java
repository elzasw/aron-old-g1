package cz.aron.customizations.soahk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuxFactory;
import cz.aron.transfagent.config.ConfigDaoFileStore;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.DaoPresentationBuilderService;
import cz.aron.transfagent.service.importfromdir.TransformService;

@ConditionalOnProperty(value="filestore.path")
@Service
public class DaoImporterFileStore implements DaoImporter {
	
	private static final Logger log = LoggerFactory.getLogger(DaoImporterFileStore.class);
	
	private final ConfigDaoFileStore config;
	
	private final DaoFileStoreService fileStore;
	
	private final TransformService transformService;
	
	private final DaoPresentationBuilderService presentationBuilder;

	public DaoImporterFileStore(ConfigDaoFileStore config, DaoFileStoreService fileStore,
			TransformService transformService, DaoPresentationBuilderService presentationBuilder) {
		this.config = config;
		this.fileStore = fileStore;
		this.transformService = transformService;
		this.presentationBuilder = presentationBuilder;
	}

	@Override
	public String getName() {
		return DaoFileStoreService.SERVICE_NAME;
	}

	@Override
	public void importDaoFile(Dao dao, Path daoDir) {
		if (config.getPath()!=null&&config.getDaoUrlPrefix()!=null) {
			importDaoFileDaoBuilder(dao, daoDir);				
		} else {
			importDaoFileOrig(dao, daoDir);			
		}		
	}
	

	public void importDaoFileOrig(Dao dao, Path daoDir) {
		Path dataDir = fileStore.getDaoDir(dao.getUuid().toString());				
		try {
			FileSystemUtils.copyRecursively(dataDir, daoDir);
			if (!transformService.transform(daoDir, dataDir)) {
				// delete empty dir
			}
			dao.setState(DaoState.READY);
		} catch (Exception e) {
		    if (!config.isCheckExistency()&&!Files.isDirectory(dataDir)) {
		        dao.setState(DaoState.INACCESSIBLE);
		        try {
                    FileSystemUtils.deleteRecursively(daoDir);
                } catch (IOException e1) {
                    log.warn("Fail to delete directory {}", daoDir);
                }
		        log.warn("Dao handle={} not exist.", dao.getHandle());
		    } else {
		        log.error("Fail to import dao {}", dao.getUuid(), e);
		    }
		}		
	}
	
	private void importDaoFileDaoBuilder(Dao dao, Path daoDir) {

		var filesDir = daoDir.resolve("files");
		try {
			Files.createDirectories(filesDir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		var daoUuid = dao.getUuid().toString();
		var daoUuidXmlFile = daoDir.resolve("dao-" + daoUuid + ".xml");

		Path dataDir = fileStore.getDaoDir(dao.getHandle());		
		List<Path> daoPaths = new ArrayList<>();
		try(var stream = Files.list(dataDir)) {
			stream.filter(p->Files.isReadable(p)).forEach(p-> daoPaths.add(p));
		} catch (IOException e1) {
			log.error("Fail to import dao {}", dao.getUuid(), e1);
			throw new UncheckedIOException(e1);
		}

		daoPaths.sort((p1, p2) -> p1.getFileName().compareTo(p2.getFileName()));
		var daoElement = presentationBuilder.buildDaoPresentation(daoPaths, dao.getUuid().toString(),
				config.getDaoPath(), config.getDaoUrlPrefix(), null, config.isRegenerate());
		try {
			var marshaller = ApuxFactory.createMarshaller();
			try (var os = Files.newOutputStream(daoUuidXmlFile)) {
				marshaller.marshal(daoElement, os);
			}
			dao.setState(DaoState.READY);
		} catch (Exception e) {
		}
	}

}
