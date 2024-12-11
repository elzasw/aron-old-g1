package cz.aron.transfagent.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
public class CommandProcessorExecutor implements  SmartLifecycle, CommandProcessorContext {
	
	private static final Logger log = LoggerFactory.getLogger(CommandProcessorExecutor.class);
	
	private final StorageService storageService;
	
	private final List<CommandProcessor> commandProcessors;
	
	private ThreadStatus status;

	private long importInterval = 5000;
	
	public CommandProcessorExecutor(StorageService storageService, List<CommandProcessor> commandProcessors) {
		this.storageService = storageService;
		this.commandProcessors = commandProcessors;
	}

	private void processCommands() {
		Path commandsPath = storageService.getInputPath().resolve("commands");
		if (Files.isDirectory(commandsPath)) {
			try (Stream<Path> stream = Files.list(commandsPath);) {
				stream.forEach(f -> {
					if (Files.isRegularFile(f)) {
						for (var commandProcessor : commandProcessors) {
							if (f.getFileName().toString().startsWith(commandProcessor.getPrefix())) {
								try {
									commandProcessor.processCommand(f, this);
								} catch (CommandProcessorException e) {
									if (e.getResult() == CommandProcessorException.Result.CONTINUE_PROCESSING) {
										// continue processing
									} else {
										throw e;
									}
								}
							}
						}
					}
				});
			} catch (IOException ioEx) {
				log.error("Fail to read commands directory {}", commandsPath, ioEx);
				throw new UncheckedIOException(ioEx);
			}
		}
	}
	
	private void run() {
		log.info("Command processor service is running.");
        
        while (status == ThreadStatus.RUNNING) {
            try {
            	processCommands();
                Thread.sleep(importInterval);
            } catch (Exception e) {
                log.error("Error in processing command. ", e);
                try {
					Thread.sleep(importInterval);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					return;
				}
            }
        }
        log.info("CommandProcessor service stopped.");
        status = ThreadStatus.STOPPED;		
	}
	
	@Override
	public void start() {
		 status = ThreadStatus.RUNNING;
	        new Thread(() -> {
	            run();
	        }).start();
	}

	@Override
	public void stop() {
		status = ThreadStatus.STOP_REQUEST;		
	}

	@Override
	public boolean isRunning() {
		return status == ThreadStatus.RUNNING;
	}
	
}

