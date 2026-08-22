package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.service.CommandProcessor;
import cz.aron.transfagent.service.CommandProcessorContext;
import cz.aron.transfagent.service.CommandProcessorException;
import cz.aron.transfagent.service.StorageService;
import cz.tacr.elza.ApiException;

/**
 * Download stage of the ELZA archdesc pull-synchronization.
 * <p>
 * Consumes {@code publication-<publicationId>} command files (produced by
 * {@link ElzaUpdateArchDescProcessor}), downloads the corresponding publication XML from ELZA, and
 * stages it into the standard {@code archdesc} import directory so the regular
 * {@code ImportArchDescService} picks it up. Alongside the XML it writes a {@code notify.json}
 * carrying the publication id, which later drives {@link ReportPublicationStatusCommand} once the archdesc
 * has been uploaded to the ARON core.
 * <p>
 * The whole feature is active only when {@code elza.url} is configured.
 *
 * @see ElzaUpdateArchDescProcessor
 * @see ReportPublicationStatusCommand
 */
@ConditionalOnProperty(value = "elza.url")
@Component
public class ImportPublicationCommand implements CommandProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportPublicationCommand.class);

    private static final String PREFIX_DASH = "publication-";

    private final StorageService storageService;

    private final ApiClientEx apiClient;
    
    private final ObjectMapper objectMapper;

    public ImportPublicationCommand(StorageService storageService, ApiClientEx apiClient, ObjectMapper objectMapper) {
        this.storageService = storageService;
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Download the publication referenced by a {@code publication-<id>} command file and stage it for import.
     * <p>
     * Parses the publication id from the file name, logs in if needed, downloads the publication XML to a
     * temporary file, and hands it to {@link #storeResult}. On success the command file is deleted. A
     * malformed file name or a {@code 404} (publication no longer exists in ELZA) moves the command to the
     * error directory and continues with the next command; any other failure clears the session and
     * rethrows so the command is retried.
     *
     * @param commandFile the {@code publication-<id>} command file
     * @param context command processing context
     */
    @Override
    public void processCommand(Path commandFile, CommandProcessorContext context) {

        var fileName = commandFile.getFileName().toString();

        // get publication id from filename
        int id;
        try {
            id = Integer.parseInt(fileName.substring(PREFIX_DASH.length()));
        } catch (NumberFormatException e) {
            log.error("Invalid publication command file name {}", commandFile);
            moveToError(commandFile, "invalid publication id in file name");
            throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
        }

        var tmp = commandFile.getParent().resolve("tmp_" + commandFile.getFileName().toString());

        loginIfNeed();

        try {
            var publicationFile = apiClient.publicationDownloadPublication(id, tmp);
            log.info("Publication id {} downloaded from Elza to {}", id, publicationFile);
            storeResult(tmp, commandFile, id);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.error("Publication id {} not exists", id);
                moveToError(commandFile, "publicationId: " + id + " not exists");
                throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
            }
            log.error("Fail to download publication id {} from Elza", id, e);
            apiClient.clearCookie();
            throw new CommandProcessorException(e);
        }

        try {
            Files.delete(commandFile);
        } catch (IOException e) {
            log.error("Fail to delete file {}", commandFile, e);
            throw new CommandProcessorException(e);
        }
    }

    /**
     * Ensure the Elza API client is logged in, clearing the session and aborting the command on failure.
     */
    private void loginIfNeed() {
        try {
            apiClient.loginIfNeed();
        } catch (Exception e) {
            apiClient.clearCookie();
            throw new CommandProcessorException(e);
        }
    }

    /**
     * Move command file to error directory, store error message.
     *
     * @param commandFile command file
     * @param message error message
     */
    private void moveToError(Path commandFile, String message) {
        try {
            storageService.moveToErrorDirWithErrMessage(commandFile, message);
            log.info("Command {} moved to error directory", commandFile);
        } catch (IOException e) {
            log.error("Fail to move to error directory {}", commandFile);
            throw new CommandProcessorException(e);
        }
    }

    /**
     * @return the command file name prefix this processor handles ({@code publication-})
     */
    @Override
    public String getPrefix() {
        return PREFIX_DASH;
    }

    /**
     * Build the command file name for a publication, used by the producer
     * ({@link ElzaUpdateArchDescProcessor}) to enqueue a download.
     *
     * @param publicationId id of the publication
     * @return command file name {@code publication-<publicationId>}
     */
    public static String createCommandFileName(long publicationId) {
        return PREFIX_DASH + publicationId;
    }

	/**
	 * Parse the downloaded publication XML and stage it into the {@code archdesc} import directory.
	 * <p>
	 * Reads the {@code ZP2015} section, derives the directory name {@code archdesc-<institution>-<fundCode>},
	 * creates it, writes {@code notify.json} (carrying the publication id) and moves the XML in as
	 * {@code <name>.xml}. If the XML lacks the section or the fund number the command is moved to the error
	 * directory and processing continues; on any other failure the temporary download and a directory this
	 * call created are cleaned up and the command is retried.
	 * <p>
	 * A previous publication of the same fund that is still waiting to be imported is left alone and the
	 * command is retried instead: the importer moves that directory away within a cycle or two, and
	 * replacing it here would destroy data the import thread may be reading at that moment.
	 *
	 * @param srcXml downloaded publication XML (temporary file, consumed on success)
	 * @param commandFile the originating command file
	 * @param publicationId id of the publication, stored into {@code notify.json}
	 */
	private void storeResult(Path srcXml, Path commandFile, Integer publicationId) {
		var archDescPath = storageService.getInputPath().resolve("archdesc");
		Path createdDir = null;
		try (InputStream is = Files.newInputStream(srcXml)) {
			ElzaXmlReader reader = ElzaXmlReader.read(is);
			var edx = reader.getEdx();
			var section = edx.getFs().getS().stream().filter(s -> "ZP2015".equals(s.getRule())).findFirst()
					.orElse(null);
			if (section == null) {
				log.error("xml {} doesn't contains section.", srcXml);
				moveToError(commandFile, "xml doesn't contains section");
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			var fundInfo = section.getFi();
			var institution = fundInfo.getIc();
			if (fundInfo.getNum()==null) {
				log.error("xml {} doesn't contains fund num.", srcXml);
				moveToError(commandFile, "xml doesn't contains fund num");
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			var fundCode = fundInfo.getNum().toString();
			var name = "archdesc-" + institution + "-" + fundCode;
			var targetDir = archDescPath.resolve(name);
			if (Files.exists(targetDir)) {
				log.info("Publication id {} not staged yet, {} is still waiting to be imported", publicationId,
						targetDir);
				deleteQuietly(srcXml);
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			Files.createDirectory(targetDir);
			createdDir = targetDir;

			Map<String, Integer> notify = new HashMap<>();
			notify.put("publicationId", publicationId);
			try (var os = Files.newOutputStream(targetDir.resolve("notify.json"))) {
				objectMapper.writeValue(os, notify);
			}
			Files.move(srcXml, targetDir.resolve(name + ".xml"));
		} catch (CommandProcessorException e) {
			// command file already moved to error, or the staging is postponed: drop the temp download
			deleteQuietly(srcXml);
			deleteQuietly(createdDir);
			throw e;
		} catch (Exception e) {
			deleteQuietly(srcXml);
			deleteQuietly(createdDir);
			log.error("Fail to store archdescs",e);
			throw new CommandProcessorException(e);
		}
	}

	/**
	 * Recursively delete a file or directory, logging but not propagating any failure.
	 */
	private void deleteQuietly(Path path) {
		try {			
			FileSystemUtils.deleteRecursively(path);
		} catch (IOException e) {
			log.error("Fail to delete {}", path, e);
		}
	}

}
