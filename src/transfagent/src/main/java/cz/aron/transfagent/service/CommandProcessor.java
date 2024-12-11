package cz.aron.transfagent.service;

import java.nio.file.Path;

public interface CommandProcessor {

	public void processCommand(Path path, CommandProcessorContext commandContext);
	
	public String getPrefix();
	
}
