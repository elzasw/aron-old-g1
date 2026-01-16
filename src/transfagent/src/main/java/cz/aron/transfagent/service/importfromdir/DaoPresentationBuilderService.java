package cz.aron.transfagent.service.importfromdir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBElement;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.DaoBuilder;
import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoBundleType;
import cz.aron.transfagent.domain.Transform;
import cz.aron.transfagent.domain.TransformState;
import cz.aron.transfagent.repository.TransformRepository;
import cz.aron.transfagent.service.importfromdir.TransformService.FileTransformKey;
import net.coobird.thumbnailator.Thumbnails;

/**
 * Class for generating dao presentation on disc
 * 
 * Generate dao to following directory
 * 
 * ${daoUuid}{0,1}/${daoUuid}{2,3}/${daoUuid}/
 * 
 * Directory contains
 * 
 * 00000000-0000-0000-0000-000000000000.zip containing thumbnails
 * 
 * ${fileUuid}.dzi for each source file containing dzi image 
 * 
 */
@Service
public class DaoPresentationBuilderService {

	private static final Logger log = LoggerFactory.getLogger(DaoPresentationBuilderService.class);
	
	private static final int DIR_NAME_LENGTH = 2;
	
	private final String zeroUuid = new UUID(0L, 0L).toString();
	
	private final TransformRepository transformRepository;
	
	private final TransactionTemplate transactionTemplate;
	
	public DaoPresentationBuilderService(TransformRepository transformRepository,
			TransactionTemplate transactionTemplate) {
		this.transformRepository = transformRepository;
		this.transactionTemplate = transactionTemplate;
	}
	
	public JAXBElement<Dao> buildDaoPresentation(List<Path> sourceFiles, String daoUuid, Path daosDir, String urlPrefix) {
		
		var daoBuilder = new DaoBuilder();
        daoBuilder.setUuid(daoUuid);
        var hiResView = daoBuilder.createDaoBundle(DaoBundleType.HIGH_RES_VIEW);
        var thumbnailView  = daoBuilder.createDaoBundle(DaoBundleType.THUMBNAIL);
		
		var tika = new Tika();		
		var daoDir = getDaoDir(daoUuid, daosDir);
		var daoUrl = urlPrefix + "/" + daoUuid;
		
		var toDelete = new ArrayList<Path>();
		
		try {
			Files.createDirectories(daoDir);			
			var mappings = getExistingMappings(daoUuid);					
			try(var zos = new ZipOutputStream(Files.newOutputStream(daoDir.resolve(zeroUuid+".zip")))) {
				int pos = 1;
				for(var sourceFile: sourceFiles) {
					var fileName = sourceFile.getFileName().toString();
					var mimeType = tika.detect(sourceFile);										
					if (mimeType != null && mimeType.startsWith("image/")) {																	
						buildDzi(sourceFile, fileName, daoUrl, hiResView, mappings, pos, daoDir, toDelete);						
						buildThumbnail(sourceFile, fileName, daoUrl, thumbnailView, mappings, pos, zos);												
						pos++;
					}
				}	
			}			
			synchronizeMappings(daoUuid, mappings);			
			toDelete.clear();						
			return daoBuilder.build();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (!toDelete.isEmpty()) {
				// delete new files in case of fail
				for(var file:toDelete) {
					try {
						Files.deleteIfExists(file);
					} catch (IOException e) {
						log.warn("Fail to delete file {}", file);
					}
				}
			}
		}

	}
	
	
	private void buildDzi(Path sourceFile, String fileName, String daoUrl, DaoBundle hiResView,
			Map<FileTransformKey, String> mappings, int pos, Path daoDir, List<Path> toDelete) throws IOException {
		var dziKey = new FileTransformKey(fileName + ".dzi", TransformService.TRANSFORM_IDENTITY);
		var dziUuid = mappings.get(dziKey);
		if (dziUuid == null) {
			// assign new uuid
			dziUuid = UUID.randomUUID().toString();
			mappings.put(dziKey, dziUuid);
			toDelete.add(daoDir.resolve(dziUuid + ".zip"));
		}
		DziUtils.createDziZip(sourceFile, daoDir.resolve(dziUuid + ".zip"), dziUuid);
		processHttp(daoUrl + "/" + dziUuid + "/image.dzi", hiResView, pos, false, null, "application/xml", dziUuid);
	}
	
	private void buildThumbnail(Path sourceFile, String fileName, String daoUrl, DaoBundle thumbnailView,
			Map<FileTransformKey, String> mappings, int pos, ZipOutputStream zos) throws IOException {

		var tnKey = new FileTransformKey(fileName, TransformService.TRANSFORM_IDENTITY);
		var tnUuid = mappings.get(tnKey);
		if (tnUuid == null) {
			// assign new uuid
			tnUuid = UUID.randomUUID().toString();
			mappings.put(tnKey, tnUuid);
		}
		var baos = new ByteArrayOutputStream();
		Thumbnails.of(sourceFile.toFile()).outputFormat("jpg").size(120, 120).toOutputStream(baos);
		var thumbnailData = baos.toByteArray();
		var crc = new CRC32();
		crc.update(thumbnailData);
		var ze = new ZipEntry(tnUuid + ".jpg");
		ze.setMethod(ZipEntry.STORED);
		ze.setSize(thumbnailData.length);
		ze.setCompressedSize(thumbnailData.length);
		ze.setCrc(crc.getValue());
		zos.putNextEntry(ze);
		zos.write(thumbnailData);
		zos.closeEntry();
		processHttp(daoUrl + "/" + zeroUuid + "/" + tnUuid + ".jpg", thumbnailView, pos, false, null, "image/jpg",
				tnUuid);
	}
	
	private Map<FileTransformKey, String> getExistingMappings(String daoUuid) {
		var mappings = new HashMap<FileTransformKey, String>();
		var transforms = transformRepository.findAllByDaoUuid(UUID.fromString(daoUuid));
		for (var transform : transforms) {
			mappings.put(new FileTransformKey(transform.getFile(), transform.getType()),
					transform.getFileUuid().toString());
		}
		return mappings;
	}
	
	private void synchronizeMappings(String daoUuid, Map<FileTransformKey, String> mappings) {
		transactionTemplate.executeWithoutResult(c -> {
			var existingMappings = getExistingMappings(daoUuid);
			for (var entry : mappings.entrySet()) {
				var existing = existingMappings.get(entry.getKey());
				if (existing == null) {
					// pokud mapovani neexistuje tako ho ulozim do databaze
					var transform = new Transform();
					transform.setDaoUuid(UUID.fromString(daoUuid));
					transform.setFile(entry.getKey().getFileName());
					transform.setFileUuid(UUID.fromString(entry.getValue()));
					transform.setState(TransformState.TRANSFORMED); // identity transform					
					transform.setType(entry.getKey().getTransformation());
					transformRepository.save(transform);
				} else {
					// pro jistotu porovnam uuid
					if (!existing.equals(entry.getValue())) {
						log.warn("Different uuid={}, file={}, transformation={}, uuid2={}", existing,
								entry.getKey().getFileName(), entry.getKey().getTransformation(), entry.getValue());
					}
				}
			}
		});
	}

	private String [] getPath(String id, int hierarchicalLevel) {
		var path = new String[hierarchicalLevel + 1];
		path[hierarchicalLevel] = id;
		var uuid = id.replaceAll("-", "");
		for (int i = 0; i < hierarchicalLevel; i++) {
			path[i] = uuid.substring(i * DIR_NAME_LENGTH, i * DIR_NAME_LENGTH + DIR_NAME_LENGTH);
		}					
		return path;
	}

	public Path getDaoDir(String daoUuid, Path daosDir) {
		return daosDir.resolve(Paths.get("", getPath(daoUuid, DIR_NAME_LENGTH)));
	}

	
	private void processHttp(String url, DaoBundle bundle, int pos, boolean selected, String permaLink, String mimeType, String uuid) {
		
		if (url == null) {
			throw new IllegalStateException(
					"Source dir cannot be empty whe sending reaferences to digital object file.");
		}

		var name = url;
		var lastSlash = url.lastIndexOf('/');
		if (lastSlash != -1 && url.length() > lastSlash + 1) {
			name = url.substring(lastSlash + 1);
		}
		
		var daoFile = DaoBuilder.createDaoFile(name, 0l, pos, mimeType, uuid);
		DaoBuilder.addReferenceFlag(daoFile);
		DaoBuilder.addPath(daoFile, url);
		if (selected) {
			DaoBuilder.addSelectedFlag(daoFile);
		}
		if (permaLink != null) {
			daoFile.setPrmLnk(permaLink);
		}
		bundle.getFile().add(daoFile);
	}
	
}
