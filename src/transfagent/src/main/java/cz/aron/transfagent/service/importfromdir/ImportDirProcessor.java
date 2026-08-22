package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ImportDirProcessor implements ImportProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(ImportProcessor.class);
	
	protected abstract Path getInputDir();
	
	@PostConstruct
	public void init() {
		Path inputDir = getInputDir();
		try {			
			FileHelper.createDirIfNotExists(inputDir);
		} catch(Exception e) {
			log.error("Failed to prepara input directory: {}", inputDir.toString());
			throw new RuntimeException("Failed to initialize.", e);
		}
	}	

	@Override
	public void importData(ImportContext ic) {
		Path inputDir = getInputDir();
		try {
			List<Path> dirs = FileHelper.getOrderedDirectories(inputDir);
			if (!dirs.isEmpty()) {
				log.info("Files to process {}, processor {}", dirs.size(), this.getClass());
			}

			// one unprocessable directory must not hide the ones behind it: record the failure
			// and carry on, the context still tells FileImportService to back off this cycle
			for (Path dir : dirs) {
				try {
					if (!processDirectory(dir)) {
						ic.setFailed(true);
						log.warn("Fail to process directory {}, processor  {}", dir, this.getClass());
						continue;
					}
					ic.addProcessed();
				} catch (Exception e) {
					log.error("Failed to import data, path: {}", dir, e);
					ic.setFailed(true);
				}
			}
		} catch (Exception e) {
			log.error("Failed to import data, path: {}", inputDir.toString(), e);
			ic.setFailed(true);
		}
	}

	/**
	 * 
	 * @param dir
	 * @return Return true if dir was processed. Return false if not processed.
	 */
	protected abstract boolean processDirectory(Path dir);

}
