package cz.aron.transfagent.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

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
		Objects.requireNonNull(monitoredPath, "Monitored path cannot be null");
		this.monitoredPath = monitoredPath;
		this.checkInterval = 5000;
	}

	public synchronized R getResource() {
		checkModified();
		return resource;
	}

	public void checkModified() {
		if ((System.currentTimeMillis() - lastCheck) > checkInterval) {
			if (!Files.isRegularFile(monitoredPath)) {
				// soubor byl smazan
				resource = null;
				lastModification = null;
			} else {
				try {
					FileTime lastMod = Files.getLastModifiedTime(monitoredPath);
					if (lastModification == null || lastMod.compareTo(lastModification) != 0) {
						resource = reloadResources();
						lastModification = lastMod;
					}
				} catch (IOException ioEx) {
					log.error("Fail to update monitored resource from {}", monitoredPath, ioEx);
				}
			}
			lastCheck = System.currentTimeMillis();
		}
	}

	/**
	 * Nahrat resource z cesty
	 * 
	 * Pokud je zavolano tak soubor jehoz cesta je v monitoredPath existuje
	 */
	public abstract R reloadResources();
	
}
