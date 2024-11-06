package cz.aron.transfagent.service;

public class CommandProcessorException extends RuntimeException {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;
	
	public enum Result {
		UNKNOWN,
		CONTINUE_PROCESSING,
	}
	
	private final Result result;
	
	public CommandProcessorException() {
		result = Result.UNKNOWN;
	}
	
	public CommandProcessorException(Result result) {
		this.result = result;
	}
		
	public CommandProcessorException(Exception e) {
		super(e);
		result = Result.UNKNOWN;
	}
	
	public CommandProcessorException(Result result, Exception e) {
		super(e);
		this.result = result;
	}

	public Result getResult() {
		return result;
	}


}
