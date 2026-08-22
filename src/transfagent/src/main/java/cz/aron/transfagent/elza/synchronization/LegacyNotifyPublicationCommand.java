package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.transfagent.service.CommandProcessor;
import cz.aron.transfagent.service.CommandProcessorContext;
import cz.aron.transfagent.service.CommandProcessorException;
import cz.aron.transfagent.service.StorageService;

/**
 * Drains {@code publication_notify-<id>} markers left in the commands directory by an earlier
 * transfagent, converting each into an {@code OK} entry of {@link PublicationReportQueue}.
 * <p>
 * Success used to be signalled by an empty marker under this name; it now travels as a report body
 * under {@code publication_report-}. Without this the pending markers would match no processor, and
 * archdescs already published in the ARON core would stay {@code FETCHED} in ELZA forever. Nothing
 * writes the old name any more, so this can be dropped once every deployment has been through one
 * upgrade.
 */
@ConditionalOnProperty(value = "elza.url")
@Component
public class LegacyNotifyPublicationCommand implements CommandProcessor {

	private static final Logger log = LoggerFactory.getLogger(LegacyNotifyPublicationCommand.class);

	private static final String PREFIX_DASH = "publication_notify-";

	private final StorageService storageService;

	private final PublicationReportQueue reportQueue;

	public LegacyNotifyPublicationCommand(StorageService storageService, PublicationReportQueue reportQueue) {
		this.storageService = storageService;
		this.reportQueue = reportQueue;
	}

	@Override
	public void processCommand(Path commandFile, CommandProcessorContext commandContext) {

		var fileName = commandFile.getFileName().toString();
		int id;
		try {
			id = Integer.parseInt(fileName.substring(PREFIX_DASH.length()));
		} catch (NumberFormatException e) {
			log.error("Invalid publication notify command file name {}", commandFile);
			moveToError(commandFile, "invalid publication id in file name");
			throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
		}

		reportQueue.enqueue(id, PublicationReport.ok());
		log.info("Legacy notify command {} converted to a publication report", commandFile);

		try {
			Files.delete(commandFile);
		} catch (IOException e) {
			log.error("Fail to delete file {}", commandFile, e);
			throw new CommandProcessorException(e);
		}
	}

	private void moveToError(Path commandFile, String message) {
		try {
			storageService.moveToErrorDirWithErrMessage(commandFile, message);
			log.info("Command {} moved to error directory", commandFile);
		} catch (IOException e) {
			log.error("Fail to move to error directory {}", commandFile);
			throw new CommandProcessorException(e);
		}
	}

	@Override
	public String getPrefix() {
		return PREFIX_DASH;
	}

}
