package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.transfagent.service.CommandProcessor;
import cz.aron.transfagent.service.CommandProcessorContext;
import cz.aron.transfagent.service.CommandProcessorException;
import cz.aron.transfagent.service.StorageService;
import cz.tacr.elza.ApiException;
import cz.tacr.elza.client.PublicationApi;
import cz.tacr.elza.client.vo.PublicationStatusReport;

/**
 * Acknowledgement stage of the ELZA archdesc pull-synchronization.
 * <p>
 * Consumes {@code publication_report-<id>} command files enqueued by {@link PublicationReportQueue}
 * and reports the outcome the file carries back to ELZA - {@code OK} for an archdesc that reached
 * the ARON core, {@code ERROR} with a message for one that will never get there. This closes the
 * loop opened by {@link ElzaUpdateArchDescProcessor} in both directions.
 */
@ConditionalOnProperty(value = "elza.url")
@Component
public class ReportPublicationStatusCommand implements CommandProcessor {

	private static final Logger log = LoggerFactory.getLogger(ReportPublicationStatusCommand.class);

	private static final String PREFIX_DASH = "publication_report-";

	private final StorageService storageService;

	private final ApiClientEx apiClient;

	private final PublicationApi publicationApi;

	private final ObjectMapper objectMapper;

	public ReportPublicationStatusCommand(StorageService storageService, ApiClientEx apiClient,
			ObjectMapper objectMapper) {
		this.storageService = storageService;
		this.apiClient = apiClient;
		this.publicationApi = new PublicationApi(apiClient);
		this.objectMapper = objectMapper;
	}

	/**
	 * Report the outcome carried by a {@code publication_report-<id>} command file to ELZA.
	 * <p>
	 * Parses the publication id from the file name and the outcome from the file body, logs in if
	 * needed, and reports it. On success the command file is deleted. A malformed file name, an
	 * unreadable body or a {@code 404} (publication no longer exists in ELZA) moves the command to the
	 * error directory and continues with the next command; any other failure clears the session and
	 * rethrows so the command is retried.
	 *
	 * @param commandFile the {@code publication_report-<id>} command file
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
			log.error("Invalid publication report command file name {}", commandFile);
			moveToError(commandFile, "invalid publication id in file name");
			throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
		}

		PublicationReport report;
		try (InputStream is = Files.newInputStream(commandFile)) {
			report = objectMapper.readValue(is, PublicationReport.class);
		} catch (IOException e) {
			log.error("Fail to read publication report {}", commandFile, e);
			moveToError(commandFile, "unreadable publication report: " + e.getMessage());
			throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
		}
		if (report == null || report.getStatus() == null) {
			log.error("Publication report {} has no status", commandFile);
			moveToError(commandFile, "publication report has no status");
			throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
		}

		loginIfNeed();

		var statusReport = new PublicationStatusReport();
		statusReport.setStatus(report.getStatus());
		statusReport.setPublishedAt(report.getOccurredAt() != null ? report.getOccurredAt() : OffsetDateTime.now());
		statusReport.setErrorMessage(report.getErrorMessage());
		try {
			publicationApi.publicationReportPublicationStatus(id, statusReport);
			log.info("Publication id {} status {} reported to Elza", id, report.getStatus());
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
	 * @return the command file name prefix this processor handles ({@code publication_report-})
	 */
	@Override
	public String getPrefix() {
		return PREFIX_DASH;
	}

	/**
	 * Build the command file name for a publication report, used by {@link PublicationReportQueue}.
	 *
	 * @param publicationId id of the publication
	 * @return command file name {@code publication_report-<publicationId>}
	 */
	public static String createCommandFileName(int publicationId) {
		return PREFIX_DASH + publicationId;
	}

}
