package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import gov.nist.isg.archiver.DirectoryArchiver;
import gov.nist.isg.archiver.FilesArchiver;
import gov.nist.isg.archiver.ZipMemoryArchiver;
import gov.nist.isg.pyramidio.BufferedImageReader;
import gov.nist.isg.pyramidio.PartialImageReader;
import gov.nist.isg.pyramidio.ScalablePyramidBuilder;

public class DziUtils {
	
	private static final Logger log = LoggerFactory.getLogger(DziUtils.class);
	
	public static void createDziZip(Path sourceImage, Path targetPath, String id) throws IOException {		
		boolean deleteCreated = true;
		try(var archiver = new ZipMemoryArchiver(targetPath)) {
			createDziInternal(sourceImage, targetPath, id, archiver);
			deleteCreated = false;
		} finally {
			if (deleteCreated) {
				try {
					Files.deleteIfExists(targetPath);
				} catch (IOException e) {
					log.warn("Fail to delete dzi zip file", targetPath);
				}
			}
		}
	}

	public static void createDziDir(Path sourceImage, Path targetPath, String id) throws IOException {
		log.debug("Creating hiResView file {}", id);
		boolean deleteCreated = true;
		try(var archiver = new DirectoryArchiver(targetPath.toFile())) {
			createDziInternal(sourceImage, targetPath, id, archiver);
			deleteCreated = false;
		} finally {
			try {
				if (deleteCreated) {		
					if (!FileSystemUtils.deleteRecursively(targetPath)) {
						log.warn("Fail to delete dzi directory", targetPath);
					}
				}
			} catch (IOException e) {
				log.warn("Fail to delete dzi directory", targetPath);
			}
		}
	}

	private static void createDziInternal(Path sourceImage, Path path, String id, FilesArchiver archiver) throws IOException {
		ScalablePyramidBuilder spb = new ScalablePyramidBuilder(254, 1, "jpg", "dzi");
		PartialImageReader pir = new BufferedImageReader(sourceImage.toFile());
		spb.buildPyramid(pir, "image", archiver, Runtime.getRuntime().availableProcessors());
	}

}
