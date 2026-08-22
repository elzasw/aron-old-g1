package cz.aron.transfagent.elza.synchronization;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.transfagent.service.CoreQueueService.PostSentNotify;
import cz.aron.transfagent.service.ImportFailureNotify;

/**
 * Acknowledgement trigger of the ELZA archdesc pull-synchronization.
 * <p>
 * Both ways an archdesc carrying a {@code publicationId} context can end pass through here: uploaded
 * to the ARON core ({@link PostSentNotify}) or rejected for good ({@link ImportFailureNotify}). Each
 * becomes an entry of {@link PublicationReportQueue}, which {@link ReportPublicationStatusCommand}
 * delivers to ELZA. Data from another source carries no {@code publicationId} and is ignored.
 * <p>
 * Active only when {@code elza.url} is configured.
 *
 * @see ReportPublicationStatusCommand
 */
@ConditionalOnProperty(value = "elza.url")
@Component
public class PublicationOutcomeNotify implements PostSentNotify, ImportFailureNotify {

    private static final Logger log = LoggerFactory.getLogger(PublicationOutcomeNotify.class);

    private final PublicationReportQueue reportQueue;

    public PublicationOutcomeNotify(PublicationReportQueue reportQueue) {
        this.reportQueue = reportQueue;
    }

    @Override
    public void sentToAronCore(Map<String, Object> notify) {
        var publicationId = getPublicationId(notify);
        if (publicationId != null) {
            reportQueue.enqueue(publicationId, PublicationReport.ok());
        }
    }

    @Override
    public void importFailed(Map<String, Object> notify, String errorMessage) {
        var publicationId = getPublicationId(notify);
        if (publicationId != null) {
            log.info("Publication id {} failed to import: {}", publicationId, errorMessage);
            reportQueue.enqueue(publicationId, PublicationReport.error(errorMessage));
        }
    }

    /**
     * @return id of the publication the context belongs to, {@code null} when it belongs to no
     *         publication
     */
    private Integer getPublicationId(Map<String, Object> notify) {
        Object publicationId = notify.get("publicationId");
        return publicationId instanceof Number ? ((Number) publicationId).intValue() : null;
    }

}
