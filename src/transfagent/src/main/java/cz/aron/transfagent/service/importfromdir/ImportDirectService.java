package cz.aron.transfagent.service.importfromdir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux._2020.ApuSource;
import cz.aron.apux._2020.Daos;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.DaoRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;

@Service
public class ImportDirectService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportDirectService.class);

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final ApuSourceRepository apuSourceRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final DaoRepository daoRepository;

    private final TransactionTemplate transactionTemplate;

    private final FileImportService fileImportService;

    final private String DIRECT_DIR = "direct";

    public ImportDirectService(ReimportService reimportService, StorageService storageService,
            ApuSourceRepository apuSourceRepository, CoreQueueRepository coreQueueRepository,
            DaoRepository daoFileRepository, TransactionTemplate transactionTemplate,
            final FileImportService fileImportService) {
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.apuSourceRepository = apuSourceRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.daoRepository = daoFileRepository;
        this.transactionTemplate = transactionTemplate;
        this.fileImportService = fileImportService;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }

    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(DIRECT_DIR);
    }
    
    @Override
    public int getPriority() { return 10; }    

    @Override
    public boolean processDirectory(Path dir) {

        File[] files = dir.toFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // process apux.xml or apux-XY.xml files
                if (name.startsWith("apux") && name.endsWith("xml")) {
                    return true;
                }
                return false;
            }
        });

        if (files.length == 0 || files.length > 1) {
            Path storedToDir;
            try {
                storedToDir = storageService.moveToErrorDir(dir);
            } catch (IOException e) {
                log.error("Fail to move {} to error directory.", dir, e);
                throw new UncheckedIOException(e);
            }
            log.error("Folder {} doesn't contains apu.xml file, moved to error directory {}", dir.getFileName(),
                      storedToDir);
            return false;
        }

        byte[] xml;
        try {
            xml = Files.readAllBytes(files[0].toPath());
        } catch (IOException e2) {
            log.error("Fail to read apusource {}", files[0]);
            throw new UncheckedIOException(e2);
        }

        ApuSource apux;
        try {
            apux = unmarshalApuSourceFromXml(xml);
        } catch (JAXBException | IOException e1) {
            Path storedToDir;
            try {
                storedToDir = storageService.moveToErrorDir(dir);
            } catch (IOException e) {
                log.error("Fail to move {} to error directory.", dir, e);
                throw new UncheckedIOException(e);
            }
            log.error("Fail to parse apu.xml. Dir {} moved to error directory {}", dir.getFileName(), storedToDir);
            return false;
        }

        var daos = apux.getApus().getApu().get(0).getDaos();
        var daosUuids = daos.getUuid().stream().map(uuidStr -> UUID.fromString(uuidStr)).collect(Collectors.toList());
        // TODO: vyresit v pripade vetsiho mnozstvi DAO (>1000)
        // TODO: udelat samostatnou service na spravu Dao a vytvorit v ni jako metody
        //       nebo jako ImportBase (predek pro Import...)
        var dbDaos = daoRepository.findAllByUuidIn(daosUuids);

        // pokud existuji, tak musi byt z jednoho apusrc
        var apuSrcIds = dbDaos.stream().map(dbDao -> dbDao.getApuSource().getId()).distinct()
                .collect(Collectors.toList());
        if (apuSrcIds.size() > 1) {
            try {
                storageService.moveToErrorDir(dir);
            } catch (IOException e) {
                log.error("Fail to move {} to error directory.", dir, e);
                throw new UncheckedIOException(e);
            }
            log.error("Daos from different apu sources. ApuSource ids={}", apuSrcIds);
            return false;
        }

        var apuSource = apuSourceRepository.findByUuid(UUID.fromString(apux.getUuid()));

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
            log.error("Fail to move {} to data dir.", dir, e);
            throw new UncheckedIOException(e);
        }

        if (apuSource.isPresent()) {
            // aktualizace            
            updateApuSource(apuSource.get(), dataDir, dir.getFileName().toString(), apux, daosUuids, dbDaos);
        } else {
            // nova apusrc
            createApuSource(dataDir, dir.getFileName().toString(), apux, daosUuids);
        }
        return true;
    }

    /**
     * Vytváření objektů na základě XML souboru
     * 
     * @param xml
     * @return cz.aron.apux._2020.ApuSource
     * @throws JAXBException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private ApuSource unmarshalApuSourceFromXml(byte[] xml) throws JAXBException, IOException {
        ApuSource apuSource = null;
        try (InputStream is = new ByteArrayInputStream(xml)) {
            Unmarshaller unmarshaller = ApuxFactory.createUnmarshaller();
            apuSource = ((JAXBElement<ApuSource>) unmarshaller.unmarshal(is)).getValue();
        }
        return apuSource;
    }

    private void createApuSource(Path dataDir, String origDir, ApuSource apux, List<UUID> daoUuids) {
        transactionTemplate.execute(t -> {
            var apuSource = new cz.aron.transfagent.domain.ApuSource();
            apuSource.setOrigDir(origDir);
            apuSource.setDataDir(dataDir.toString());
            apuSource.setSourceType(SourceType.DIRECT);
            apuSource.setUuid(UUID.fromString(apux.getUuid()));
            apuSource.setDeleted(false);
            apuSource.setDateImported(ZonedDateTime.now());
            var storedApuSource = apuSourceRepository.save(apuSource);

            daoUuids.forEach(daoUuid -> {
                Dao daoFile = new Dao();
                daoFile.setApuSource(storedApuSource);
                daoFile.setDataDir("");
                daoFile.setLastModified(0l);
                daoFile.setSource("");
                daoFile.setState(DaoState.ACCESSIBLE);
                daoFile.setTransferred(false);
                daoFile.setUuid(daoUuid);
                daoRepository.save(daoFile);
            });

            CoreQueue coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("Direct imported uuid={}, directory={}", apux.getUuid(), origDir);
    }

    private void updateApuSource(cz.aron.transfagent.domain.ApuSource existingApuSource, Path dataDir, String origDir,
                                 ApuSource apux, List<UUID> daoUuids, List<Dao> existingDaos) {

        var daoUuidsSet = new HashSet<UUID>(daoUuids);
        var toDelete = new ArrayList<Dao>();
        var toUpdate = new ArrayList<Dao>();
        for (Dao existingDao : existingDaos) {
            if (daoUuidsSet.remove(existingDao.getUuid())) {
                toUpdate.add(existingDao);
            } else {
                toDelete.add(existingDao);
            }
        }

        transactionTemplate.execute(t -> {
            existingApuSource.setDataDir(dataDir.toString());
            existingApuSource.setOrigDir(origDir);
            var updatedApuSource = apuSourceRepository.save(existingApuSource);

            daoUuidsSet.stream().forEach(daoUuid -> {
                Dao daoFile = new Dao();
                daoFile.setApuSource(updatedApuSource);
                daoFile.setLastModified(0l);
                daoFile.setSource("");
                daoFile.setState(DaoState.ACCESSIBLE);
                daoFile.setTransferred(false);
                daoFile.setUuid(daoUuid);
                daoRepository.save(daoFile);
            });

            toDelete.forEach(dao -> {
                daoRepository.delete(dao);
            });

            toUpdate.forEach(dao -> {
                dao.setDataDir(null);
                dao.setState(DaoState.ACCESSIBLE);
                dao.setTransferred(false);
                daoRepository.save(dao);
            });

            CoreQueue coreQueue = new CoreQueue();
            coreQueue.setApuSource(existingApuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("Direct updated uuid={}, directory={}, file={}", apux.getUuid(), dataDir, origDir);
    }

    @Override
    public Result reimport(cz.aron.transfagent.domain.ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.DIRECT)
            return Result.UNSUPPORTED;

        List<Dao> daoFiles = daoRepository.findByApuSource(apuSource);
        if (daoFiles.isEmpty()) {
            log.error("Missing dao file(s): {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }
        for (Dao daoFile: daoFiles) {
            if (daoFile.getState() != DaoState.ACCESSIBLE) {
                log.warn("Dao file(s) {} cannot be reimported, status: {}", apuSource.getId(), daoFile.getState());
                return Result.UNSUPPORTED;
            }
        }

        Path dataDir = storageService.getApuDataDir(apuSource.getDataDir());
        String code = apuSource.getOrigDir().substring("direct-".length());
        String fileName = "apux-" + code + ".xml";
        try {
            byte[] xml = Files.readAllBytes(dataDir.resolve(fileName));
            ApuSource apux = unmarshalApuSourceFromXml(xml);
            Daos daos = apux.getApus().getApu().get(0).getDaos();
            List<UUID> daosUuids = daos.getUuid().stream()
                    .map(uuidStr -> UUID.fromString(uuidStr))
                    .collect(Collectors.toList());;
            List<Dao> dbDaos = daoRepository.findAllByUuidIn(daosUuids);

            updateApuSource(apuSource, dataDir, fileName, apux, daosUuids, dbDaos);
        } catch (Exception e) {
            log.error("Fail to reimport, dir={}", dataDir, e);
            return Result.FAILED;
        }
        return Result.REIMPORTED;
    }

}
