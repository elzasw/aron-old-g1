package cz.aron.customizations.lapnp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuxFactory;
import cz.aron.customizations.lapnp.DaoFileStore2Service.ArchiveFundDao;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.DaoPresentationBuilderService;
import cz.aron.transfagent.service.importfromdir.TransformService;

@ConditionalOnProperty(value="filestore2.path")
@Service
public class DaoImporterFileStore2 implements DaoImporter {
	
	private static final Logger log = LoggerFactory.getLogger(DaoImporterFileStore2.class);
	
	private final DaoFileStore2Service fileStore;
	
	private final ConfigDaoFileStore2 config;
	
	private final TransformService transformService;
	
	private final DaoPresentationBuilderService presentationBuilder;

	public DaoImporterFileStore2(ConfigDaoFileStore2 config, TransformService transformService,
			DaoPresentationBuilderService daoPresentationBuilderService, DaoFileStore2Service daoFileStore2Service) {
		this.config = config;
		this.transformService = transformService;
		this.presentationBuilder = daoPresentationBuilderService;
		this.fileStore = daoFileStore2Service;
	}

	@Override
	public String getName() {
		return fileStore.getName();
	}

	@Override
	public void importDaoFile(Dao dao, Path daoDir) {		
		if (config.getDaoPath()!=null&&config.getDaoUrlPrefix()!=null) {
			importDaoFileDaoBuilder(dao, daoDir);				
		} else {
			importDaoFileOrig(dao, daoDir);			
		}				
	}
	
	private void importDaoFileOrig(Dao dao, Path daoDir) {
		var afd = ArchiveFundDao.fromString(dao.getHandle());
		List<Path> daoPaths;
		try {
			daoPaths = fileStore.getDaos(afd);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}
		try {
			for (Path daoPath : daoPaths) {
				Files.copy(daoPath, daoDir.resolve(daoPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			if (!transformService.transform(daoDir, null)) {
				// delete empty dir
			}
			dao.setState(DaoState.READY);
		} catch (Exception e) {
			log.error("Fail to import dao {}", dao.getUuid(), e);
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

		var afd = ArchiveFundDao.fromString(dao.getHandle());
		List<Path> daoPaths;
		try {
			daoPaths = fileStore.getDaos(afd);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}
		daoPaths.sort((p1, p2) -> p1.getFileName().compareTo(p2.getFileName()));
		var daoElement = presentationBuilder.buildDaoPresentation(daoPaths, dao.getUuid().toString(),
				config.getDaoPath(), config.getDaoUrlPrefix());
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
