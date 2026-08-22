package cz.aron.transfagent.elza;

import cz.aron.transfagent.service.UnrecoverableImportException;

/**
 * The archival description names a fund this transfagent does not have.
 * <p>
 * Unrecoverable like any other missing reference, but recoverable by the portal itself: the received
 * export carries the whole fund info, so the caller may create the fund from it instead of rejecting
 * the description. {@link #getFundCode()} is the identifier to create it under - it has to be the one
 * {@link ImportArchDesc} will look the fund up by next time, or every attempt creates another fund.
 */
public class MissingFundException extends UnrecoverableImportException {

	private static final long serialVersionUID = 1L;

	private final String fundCode;

	/**
	 * @param fundCode identifier to create the fund under, {@code null} when the export identifies the
	 *            fund by neither its own code nor a NAD number
	 * @param message operator-readable reason, naming the institution too
	 */
	public MissingFundException(String fundCode, String message) {
		super(message);
		this.fundCode = fundCode;
	}

	public String getFundCode() {
		return fundCode;
	}

}
