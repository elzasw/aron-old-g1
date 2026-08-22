package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.transfagent.service.StorageService;

/**
 * The single way an outcome of a publication leaves transfagent towards ELZA.
 * <p>
 * Both outcomes travel the same queue: {@link PublicationOutcomeNotify} enqueues
 * {@link PublicationReport#ok()} once the archdesc reached the ARON core and
 * {@link PublicationReport#error(String)} when it never will.
 * {@link ReportPublicationStatusCommand} drains the queue on the command thread, so a
 * report survives a restart and an unreachable ELZA without blocking whoever produced it.
 * <p>
 * A newer report for the same publication overwrites a pending older one, which is what ELZA's
 * last-writer-wins contract for {@code POST /publications/status/{id}} expects.
 * <p>
 * Active only when {@code elza.url} is configured.
 */
@ConditionalOnProperty(value = "elza.url")
@Service
public class PublicationReportQueue {

	private static final Logger log = LoggerFactory.getLogger(PublicationReportQueue.class);

	private final StorageService storageService;

	private final ObjectMapper objectMapper;

	public PublicationReportQueue(StorageService storageService, ObjectMapper objectMapper) {
		this.storageService = storageService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Enqueue the outcome of a publication for delivery to ELZA.
	 * <p>
	 * The report is written to a temporary file first and moved into place, so the command thread
	 * never reads a half-written body.
	 *
	 * @param publicationId id of the publication in ELZA
	 * @param report outcome to report
	 */
	public void enqueue(int publicationId, PublicationReport report) {
		var commandsPath = storageService.getInputPath().resolve("commands");
		var commandFile = commandsPath.resolve(ReportPublicationStatusCommand.createCommandFileName(publicationId));
		var tmpFile = commandsPath.resolve("tmp_" + commandFile.getFileName().toString());
		try {
			Files.createDirectories(commandsPath);
			try (var os = Files.newOutputStream(tmpFile)) {
				objectMapper.writeValue(os, report);
			}
			Files.move(tmpFile, commandFile, StandardCopyOption.REPLACE_EXISTING);
			log.info("Publication id {} report {} enqueued", publicationId, report.getStatus());
		} catch (IOException e) {
			deleteQuietly(tmpFile);
			log.error("Fail to enqueue report for publication id {}", publicationId, e);
			throw new UncheckedIOException(e);
		}
	}

	private void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.error("Fail to delete {}", path, e);
		}
	}

}
