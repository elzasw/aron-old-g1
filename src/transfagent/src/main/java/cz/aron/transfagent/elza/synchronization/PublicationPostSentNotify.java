package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.transfagent.service.CoreQueueService.PostSentNotify;
import cz.aron.transfagent.service.StorageService;

/**
 * Acknowledgement trigger of the ELZA archdesc pull-synchronization.
 * <p>
 * Invoked after an archdesc carrying a {@code publicationId} context has been uploaded to the ARON
 * core, it touches a {@code publication_notify-<id>} command file in {@code input/commands/}. That
 * marker is later consumed by {@link NotifyPublicationCommand}, which reports the published status
 * back to ELZA. Active only when {@code elza.url} is configured.
 *
 * @see NotifyPublicationCommand
 */
@ConditionalOnProperty(value = "elza.url")
@Component
public class PublicationPostSentNotify implements PostSentNotify {

    private static final Logger log = LoggerFactory.getLogger(PublicationPostSentNotify.class);

    private final StorageService storageService;

    public PublicationPostSentNotify(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void sentToAronCore(Map<String, Object> notify) {
        Object publicationId = notify.get("publicationId");
        if (publicationId instanceof Number) {
            var id = ((Number) publicationId).intValue();
            var path = storageService.getInputPath().resolve("commands").resolve("publication_notify-" + id);
            try {
                Files.createFile(path);
                log.info("Created publication notify command {}", path);
            } catch (IOException e) {
                log.error("Fail to create publication notify command for publicationId {}", id, e);
                throw new UncheckedIOException(e);
            }
        }
    }

}
