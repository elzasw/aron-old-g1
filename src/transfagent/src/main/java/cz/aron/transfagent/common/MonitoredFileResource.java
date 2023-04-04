package cz.aron.transfagent.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trida pro monitorovani souboru s konfiguraci na disku.
 * Pri vyzadani resource se zjistuje jestli se soubor nezmenil na disku
 * 
 * @param <R> resource
 * @param <K> key
 */
public abstract class MonitoredFileResource<R> {

	private static final Logger log = LoggerFactory.getLogger(MonitoredFileResource.class);

	// cesta k souboru
	protected final Path monitoredPath;

	// interval po kterem se kontroluje na disku, milisekundy
	private final long checkInterval;

	private FileTime lastModification = null;

	private long lastCheck = 0;
	
	private R resource = null;

	public MonitoredFileResource(Path monitoredPath) {
		this.monitoredPath = monitoredPath;
		this.checkInterval = 5000;
	}

	public synchronized R getResource() {
		checkModified();
		return resource;
	}

	public void checkModified() {
		if (monitoredPath != null && (System.currentTimeMillis() - lastCheck) > checkInterval) {			
			Path mappingFile = monitoredPath;
			if (!Files.isRegularFile(mappingFile)) {
				// soubor byl smazan
				resource = null;
				lastCheck = System.currentTimeMillis();
				return;
			}
			try {
				FileTime lastMod = Files.getLastModifiedTime(mappingFile);
				if (lastModification == null || lastMod.compareTo(lastModification) != 0) {
					resource = reloadResources();
				}
			} catch (IOException ioEx) {
				log.error("Fail to update monitored resource from {}", monitoredPath, ioEx);
			}
			lastCheck = System.currentTimeMillis();
		}
	}

	/**
	 * Nahrat resource z cesty
	 */
	public abstract R reloadResources();
	
}
