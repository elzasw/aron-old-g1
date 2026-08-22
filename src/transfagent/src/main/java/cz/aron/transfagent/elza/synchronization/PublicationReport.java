package cz.aron.transfagent.elza.synchronization;

import java.time.OffsetDateTime;

import cz.tacr.elza.client.vo.PublicationReportStatus;

/**
 * Outcome of one publication, waiting in the commands directory to be reported to ELZA.
 * <p>
 * Written by {@link PublicationReportQueue} as the body of a {@code publication_report-<id>} command
 * file and read back by {@link ReportPublicationStatusCommand}, so it is a persisted format: keep it
 * additive and tolerant of fields an older transfagent did not write.
 */
public class PublicationReport {

	private PublicationReportStatus status;

	private OffsetDateTime occurredAt;

	private String errorMessage;

	public PublicationReport() {
		// for deserialization
	}

	private PublicationReport(PublicationReportStatus status, OffsetDateTime occurredAt, String errorMessage) {
		this.status = status;
		this.occurredAt = occurredAt;
		this.errorMessage = errorMessage;
	}

	/**
	 * The archdesc reached the ARON core and is published.
	 */
	public static PublicationReport ok() {
		return new PublicationReport(PublicationReportStatus.OK, OffsetDateTime.now(), null);
	}

	/**
	 * The archdesc will never be published, for the stated reason.
	 *
	 * @param errorMessage what is missing or wrong, in operator-readable form
	 */
	public static PublicationReport error(String errorMessage) {
		return new PublicationReport(PublicationReportStatus.ERROR, OffsetDateTime.now(), errorMessage);
	}

	public PublicationReportStatus getStatus() {
		return status;
	}

	public void setStatus(PublicationReportStatus status) {
		this.status = status;
	}

	public OffsetDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
