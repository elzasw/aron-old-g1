package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.config.ConfigElzaFs;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.TransformService;

/**
 * DAO importer for Elza filesystem digital repositories: the export delivers
 * {@code <dao><rep>code</rep><doid>relative/path</doid></dao>} per level, and
 * this importer materializes the referenced file or directory from the locally
 * accessible root configured for the repository code ({@code elzafs.repos}).
 *
 * <p>The dao handle is {@code repCode|levelUuid|path} - the level uuid is part
 * of it because a DAO belongs to one APU, so the same path linked from two
 * levels must become two daos. The dao uuid derives deterministically from the
 * handle, so a re-publication reuses it and the APUX reference always matches
 * the dao row.
 *
 * <p>An unreachable repository root (typically an unmounted network share)
 * throws: the import cycle fails and retries later, and the dao stays
 * ACCESSIBLE - never mistaken for a deleted one. A path missing under a
 * reachable root is INACCESSIBLE, as is a directory over {@code elzafs.maxFiles}.
 */
@ConditionalOnProperty(value = "elzafs.repos[0].code")
@Service
public class DaoElzaFsService implements DaoImporter {

    public static final String SOURCE = "elzafs";

    /** The dao.handle column is 255 chars; longer handles are skipped with a WARN. */
    private static final int MAX_HANDLE_LENGTH = 255;

    private static final Logger log = LoggerFactory.getLogger(DaoElzaFsService.class);

    private final TransformService transformService;

    private final ConfigElzaFs config;

    private final Map<String, Path> rootsByCode = new HashMap<>();

    public DaoElzaFsService(TransformService transformService, ConfigElzaFs config) {
        this.transformService = transformService;
        this.config = config;
        for (var repo : config.getRepos()) {
            rootsByCode.put(repo.getCode(), repo.getPath().toAbsolutePath().normalize());
        }
        log.info("Elza filesystem repositories configured: {}", rootsByCode.keySet());
    }

    @Override
    public String getName() {
        return SOURCE;
    }

    /** Whether the repository code of an exported dao is served by this importer. */
    public boolean isConfiguredRepository(String repositoryCode) {
        return rootsByCode.containsKey(repositoryCode);
    }

    /**
     * Handle of one exported dao, or {@code null} when it cannot be stored
     * (over the column limit) - the dao is then skipped, with a WARN.
     */
    public static String createHandle(String repositoryCode, String levelUuid, String path) {
        var handle = repositoryCode + "|" + levelUuid + "|" + path;
        if (handle.length() > MAX_HANDLE_LENGTH) {
            log.warn("Dao handle too long (over {} chars), dao skipped: {}", MAX_HANDLE_LENGTH, handle);
            return null;
        }
        return handle;
    }

    /** Deterministic dao uuid: a re-publication reuses it, keeping published references stable. */
    public static UUID daoUuidOf(String handle) {
        return UUID.nameUUIDFromBytes((SOURCE + "|" + handle).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void importDaoFile(Dao dao, Path daoDir) {
        String[] parts = dao.getHandle().split("\\|", 3);
        if (parts.length != 3) {
            log.error("Malformed elzafs dao handle: {}", dao.getHandle());
            dao.setState(DaoState.INACCESSIBLE);
            return;
        }
        Path root = rootsByCode.get(parts[0]);
        if (root == null) {
            // the repository disappeared from the configuration
            log.warn("Elza filesystem repository '{}' is not configured, dao {} inaccessible.", parts[0],
                    dao.getUuid());
            dao.setState(DaoState.INACCESSIBLE);
            return;
        }
        if (!Files.isDirectory(root)) {
            // the mount is down: fail the import cycle so the dao is retried, never invalidated
            throw new IllegalStateException("Elza filesystem repository root not accessible: " + root);
        }

        Path target = root.resolve(parts[2]).normalize();
        if (!target.startsWith(root)) {
            // the path is data from the export; nothing may escape the root
            log.warn("Dao path escapes its repository root, dao {} inaccessible: {}", dao.getUuid(), parts[2]);
            dao.setState(DaoState.INACCESSIBLE);
            return;
        }

        try {
            if (Files.isRegularFile(target)) {
                // REPLACE_EXISTING: a retried import may find leftovers of the failed attempt
                Files.copy(target, daoDir.resolve(target.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            } else if (Files.isDirectory(target)) {
                if (!copyDirectory(target, daoDir, dao)) {
                    return;
                }
            } else {
                log.warn("Dao path does not exist under its repository, dao {} inaccessible: {}", dao.getUuid(),
                        parts[2]);
                markInaccessible(dao, daoDir);
                return;
            }
            transformService.transform(daoDir, target.getParent() != null ? target.getParent() : root);
            dao.setState(DaoState.READY);
        } catch (Exception e) {
            // includes I/O failures mid-copy (e.g. the mount dying): rethrow so the cycle retries
            throw new IllegalStateException("Fail to import elzafs dao " + dao.getUuid(), e);
        }
    }

    /**
     * Copies a directory link's files into the dao dir: regular files only,
     * hidden ones skipped, ordered by relative path (which becomes the page
     * order), nested names flattened with {@code _}. Over {@code maxFiles} the
     * dao is refused as INACCESSIBLE - a link on a huge directory (typically a
     * mistaken link on the repository root) must not generate thousands of
     * tile pyramids.
     */
    private boolean copyDirectory(Path target, Path daoDir, Dao dao) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(target)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(file -> !file.getFileName().toString().startsWith("."))
                    .sorted()
                    .limit(config.getMaxFiles() + 1L)
                    .collect(Collectors.toList());
        }
        if (files.isEmpty()) {
            log.warn("Dao directory is empty, dao {} inaccessible: {}", dao.getUuid(), target);
            markInaccessible(dao, daoDir);
            return false;
        }
        if (files.size() > config.getMaxFiles()) {
            log.warn("Dao directory has more than elzafs.maxFiles={} files, dao {} inaccessible: {}",
                    config.getMaxFiles(), dao.getUuid(), target);
            markInaccessible(dao, daoDir);
            return false;
        }
        for (Path file : files) {
            String flatName = target.relativize(file).toString().replace('\\', '/').replace('/', '_');
            Files.copy(file, daoDir.resolve(flatName), StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    private static void markInaccessible(Dao dao, Path daoDir) {
        dao.setState(DaoState.INACCESSIBLE);
        try {
            FileSystemUtils.deleteRecursively(daoDir);
        } catch (IOException e) {
            log.warn("Fail to delete directory {}", daoDir);
        }
    }

}
