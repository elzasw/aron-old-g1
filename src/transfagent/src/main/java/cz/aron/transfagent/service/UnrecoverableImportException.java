package cz.aron.transfagent.service;

/**
 * Import failure that cannot succeed on a retry.
 * <p>
 * Thrown by the data-level guards of an importer: a referenced fund or institution that does not
 * exist, an unparseable or incomplete input file, data rejected by the APUX validator. Retrying such
 * an import repeats the same failure forever, so the caller parks the data in the error directory
 * instead and reports the outcome to whoever supplied it.
 * <p>
 * Infrastructure failures (database, disk, unreachable Elza or ARON core) are deliberately
 * <em>not</em> signalled this way - they are recoverable and must keep being retried.
 * <p>
 * The message is read by operators and forwarded to the source system, so it has to say what is
 * missing, not where the code failed.
 */
public class UnrecoverableImportException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnrecoverableImportException(String message) {
		super(message);
	}

	public UnrecoverableImportException(String message, Throwable cause) {
		super(message, cause);
	}

}
