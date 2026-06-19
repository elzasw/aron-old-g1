package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.transfagent.service.CommandProcessor;
import cz.aron.transfagent.service.CommandProcessorContext;
import cz.aron.transfagent.service.CommandProcessorException;
import cz.aron.transfagent.service.StorageService;
import cz.tacr.elza.ApiException;
import cz.tacr.elza.client.PublicationApi;
import cz.tacr.elza.client.vo.PublicationReportStatus;
import cz.tacr.elza.client.vo.PublicationStatusReport;

/**
 * Acknowledgement stage of the ELZA archdesc pull-synchronization.
 * <p>
 * Consumes {@code publication_notify-<id>} command files produced by the core upload step and reports
 * the publication status back to ELZA, telling it the archdesc is now published in ARON. This closes
 * the loop opened by {@link ElzaUpdateArchDescProcessor}.
 */
@ConditionalOnProperty(value = "elza.url")
@Component
public class NotifyPublicationCommand implements CommandProcessor {

	private static final Logger log = LoggerFactory.getLogger(NotifyPublicationCommand.class);

	private static final String PREFIX_DASH = "publication_notify-";

	private final StorageService storageService;

	private final ApiClientEx apiClient;

	private final PublicationApi publicationApi;

	public NotifyPublicationCommand(StorageService storageService, ApiClientEx apiClient) {
		this.storageService = storageService;
		this.apiClient = apiClient;
		this.publicationApi = new PublicationApi(apiClient);
	}

	/**
	 * Report the publication status back to ELZA for a {@code publication_notify-<id>} command file.
	 * <p>
	 * Parses the publication id from the file name, logs in if needed, and reports status {@code OK} to
	 * ELZA. On success the command file is deleted. A malformed file name or a {@code 404} (publication
	 * no longer exists in ELZA) moves the command to the error directory and continues with the next
	 * command; any other failure clears the session and rethrows so the command is retried.
	 *
	 * @param commandFile the {@code publication_notify-<id>} command file
	 * @param commandContext command processing context
	 */
	@Override
	public void processCommand(Path commandFile, CommandProcessorContext commandContext) {

		var fileName = commandFile.getFileName().toString();
		// get publication id from filename
		int id;
		try {
			id = Integer.parseInt(fileName.substring(PREFIX_DASH.length()));
		} catch (NumberFormatException e) {
			log.error("Invalid publication notify command file name {}", commandFile);
			moveToError(commandFile, "invalid publication id in file name");
			throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
		}

		loginIfNeed();

		PublicationStatusReport statusReport = new PublicationStatusReport();
		statusReport.setStatus(PublicationReportStatus.OK);
		statusReport.setPublishedAt(OffsetDateTime.now());
		try {
			publicationApi.publicationReportPublicationStatus(id, statusReport);
			log.info("Publication id {} status reported to Elza", id);
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				log.error("Publication id {} not exists", id);
				moveToError(commandFile, "publicationId: " + id + " not exists");
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			log.error("Fail to report publication id {} status to Elza", id, e);
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
	 * @return the command file name prefix this processor handles ({@code publication_notify-})
	 */
	@Override
	public String getPrefix() {
		return PREFIX_DASH;
	}

}
