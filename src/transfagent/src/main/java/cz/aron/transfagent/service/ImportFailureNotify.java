package cz.aron.transfagent.service;

import java.util.Map;

import cz.aron.transfagent.service.CoreQueueService.PostSentNotify;

/**
 * Callback invoked when imported data carrying a context cannot be imported at all.
 * <p>
 * The counterpart of {@link PostSentNotify}: that one closes the loop after the data reached the
 * ARON core, this one closes it when the data never will. Implementations registered as Spring beans
 * receive the same deserialized context map ({@code notify.json} of the imported directory) plus the
 * reason, and report it back to the system the data came from.
 * <p>
 * Called only for {@link UnrecoverableImportException} - a recoverable failure is retried and must
 * not be reported.
 */
public interface ImportFailureNotify {

	/**
	 * Report that the imported data was rejected for good.
	 *
	 * @param notify the deserialized context map, never {@code null}
	 * @param errorMessage what is missing or wrong, in operator-readable form
	 */
	void importFailed(Map<String, Object> notify, String errorMessage);

}
