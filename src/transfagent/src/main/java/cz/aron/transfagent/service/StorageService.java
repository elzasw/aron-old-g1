package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StorageService {

	private static Logger log = LoggerFactory.getLogger(StorageService.class);

	public static final String APUSRC_XML = "apusrc.xml";

	private final Path workDir;

	private final Path inputPath;

	private final Path dataPath;

	private final Path errorPath;

	private final Path daoPath;

	private final AtomicLong counter = new AtomicLong(0);

	private final Tika tika = new Tika();

	public StorageService(@Value("${aron.workDir}") String workDirStr, @Value("${dao.dir}") String daoFolderStr) {
		this.workDir = Path.of(workDirStr);
		this.inputPath = workDir.resolve("input");
		this.dataPath = workDir.resolve("data");
		this.errorPath = workDir.resolve("error");
		this.daoPath = Path.of(daoFolderStr);
	}

	public Path getInputPath() {
		return inputPath;
	}

	public Path getDataPath() {
		return dataPath;
	}

	public Path getErrorPath() {
		return errorPath;
	}

	public Path getDaoPath() {
		return daoPath;
	}

	/**
	 * Move directory to data directory
	 * 
	 * @param sourceDir dir to move
	 * @return {@link Path} relative to data directory
	 * @throws IOException
	 */
	public Path moveToDataDir(Path sourceDir) throws IOException {
		return moveToDir(dataPath, sourceDir);
	}

	public Path moveToErrorDir(Path sourceDir) throws IOException {
		return moveToDir(errorPath, sourceDir);
	}
	
	/**
	 * Move to error directory and create error file with suffix .err 
	 * @param sourceFile file to be moved to error directory
	 * @param message error message
	 * @return path relativized to error directory
	 * @throws IOException
	 */
	public Path moveToErrorDirWithErrMessage(Path sourceFile, String message) throws IOException {
		var movedFile = moveToDir(errorPath, sourceFile);
		var messageFile = dataPath.resolve(movedFile.getParent().resolve(movedFile.getFileName().toString() + ".err"));
		Files.writeString(messageFile, message);
		return movedFile;
	}

	public Path moveToProcessed(Path sourceDir) throws IOException {
		return moveToDir(inputPath.resolve("processed"), sourceDir);
	}

	private Path moveToDir(Path targetDir, Path movedDir) throws IOException {
		Path parentDir = targetDir.resolve(String.format("%1$tY%1$tm%1$td", new Date()));
		Files.createDirectories(parentDir);
		String dirName = movedDir.getFileName().toString();
		Path dirPath = parentDir.resolve(dirName);
		int count = 0;
		while (Files.exists(dirPath)) {
			dirPath = parentDir.resolve(dirName + "_" + counter.incrementAndGet());
			count++;
			if (count > 1000) {
				throw new IOException("Fail to create data directory for source " + dirName);
			}
		}
		log.debug("Moving directory {} to directory {}", movedDir, dirPath);
		Files.move(movedDir, dirPath);
		return dataPath.relativize(dirPath);
	}

	/**
	 * Return absolute path to apu datadir
	 * 
	 * @param dataDir relative path of directory
	 * @return {@link Path}
	 */
	public Path getApuDataDir(String dataDir) {
		return dataPath.resolve(dataDir);
	}

	public Path createTempDir(String prefix) throws IOException {
		var dailyTmpDir = workDir.resolve("tmp").resolve(String.format("%1$tY%1$tm%1$td", new Date()));
		Files.createDirectories(dailyTmpDir);
		var tmpDir = dailyTmpDir.resolve(prefix);
		int count = 0;
		while (Files.exists(tmpDir)) {
			tmpDir = dailyTmpDir.resolve(prefix + "_" + counter.incrementAndGet());
			count++;
			if (count > 1000) {
				throw new IOException("Fail to create data directory for source " + prefix);
			}
		}
		log.debug("Creating a temporary directory {}", tmpDir);
		Files.createDirectory(tmpDir);
		return tmpDir;
	}

	/**
	 * Detekuje mimetype souboru
	 * 
	 * @param file soubor k detekci
	 * @return String s mimetype
	 * @throws IOException
	 */
	public String detectMimetype(Path file) throws IOException {
		return tika.detect(file);
	}

	/**
	 * Porovna obsah souboru se zadanym polem
	 * @param existingFile cesta k souboru
	 * @param content obsah k porovnani
	 * @return true - jsou shodne, false - jsou rozdilne nebo se nepodarilo soubor nacist
	 */
	public static final boolean isContentEqual(Path existingFile, byte [] content) {		
		byte [] existingFileContent = null;
		try {
			existingFileContent = Files.readAllBytes(existingFile);			
		} catch (Exception e) {
			log.warn("Fail to read file {} to compare content.",existingFile,e);
			return false;
		}	
		return Arrays.equals(existingFileContent, content);
	}
	
}
