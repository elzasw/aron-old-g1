package cz.aron.transfagent.service.importfromdir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.tika.Tika;
import org.jsoup.helper.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux.DaoBuilder;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoBundleType;
import cz.aron.transfagent.config.ConfigDao;
import cz.aron.transfagent.config.ConfigDao.SendType;
import cz.aron.transfagent.config.ConfigDspace;
import cz.aron.transfagent.domain.Transform;
import cz.aron.transfagent.domain.TransformState;
import cz.aron.transfagent.repository.TransformRepository;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DSpaceConsts;
import gov.nist.isg.archiver.DirectoryArchiver;
import gov.nist.isg.archiver.FilesArchiver;
import gov.nist.isg.pyramidio.BufferedImageReader;
import gov.nist.isg.pyramidio.PartialImageReader;
import gov.nist.isg.pyramidio.ScalablePyramidBuilder;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class TransformService {

	private static final Logger log = LoggerFactory.getLogger(TransformService.class);

	private static final int DIR_NAME_LENGTH = 2;

	// konstanta zapisovana do databaze pro transormaci deep zoom
	public static String TRANSFORM_DZI = "dzi";

	// konstanta zapisovana do databaze pro transformaci vytvareni nahledu
	public static String TRANSFORM_THUMBNAIL = "thumbnail";
	
	// konstanta zapisovana do databaze pro zachovani uuid zdrojoveho souboru
	// TODO potrebujeme zachovat uuid?
	public static String TRANSFORM_IDENTITY = "identity";

	private final StorageService storageService;

	private final ConfigDspace configDspace;

	private final ConfigDao configDao;

	private final TransformRepository transformRepository;

	private final TransactionTemplate transactionTemplate;

	@Value("${tile.folder:#{NULL}}")
	private String tileFolder;

	@Value("${tile.level:2}")
	private int tileHierarchicalLevel;

	@Value("${tile.async:false}")
	private boolean tileAsync;

	@Value("${thumbnail.folder:#{NULL}}")
	private String thumbnailFolder;

	@Value("${thumbanil.level:2}")
	private int thumbnailHierarchicalLevel;

	@Value("${thumbnail.async:false}")
	private boolean thumbnailAsync;

	public TransformService(StorageService storageService, ConfigDspace configDspace, ConfigDao configDao,
			TransformRepository transformRepository, TransactionTemplate transactionTemplate) {
		this.storageService = storageService;
		this.configDspace = configDspace;
		this.configDao = configDao;
		this.transformRepository = transformRepository;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Vytvori na disku strukturu dao pro odeslani do AronCore
	 * @param daoDir adresar do ktereho se vytvari data k odeslani
	 * @param sourceDir zdrojovy adresar s daty
	 * @return
	 * @throws JAXBException
	 * @throws IOException
	 */
	public boolean transform(Path daoDir, Path sourceDir) throws JAXBException, IOException {

		log.debug("Transforming data, path: {}", daoDir);

		if (!Files.isDirectory(daoDir)) {
			log.error("Directory does not exist {}", daoDir);
			throw new IOException();
		}

		Tika tika = new Tika();

		var daoUuid = daoDir.getFileName().toString();
		var daoUuidXmlFile = daoDir.resolve("dao-" + daoUuid + ".xml");
		var filesDir = daoDir.resolve("files");

		// mazání předchozích souborů
		log.debug("Deleting old files and folders in a directory {}", daoDir);
		FileSystemUtils.deleteRecursively(filesDir);
		Files.deleteIfExists(daoUuidXmlFile);

		log.debug("Preparing files list in directory {}", daoDir);
		List<Path> files = prepareFileList(daoDir);
		if (files.isEmpty()) {
			return false;
		}

		Files.createDirectories(filesDir);

		DaoBuilder daoBuilder = new DaoBuilder();
		daoBuilder.setUuid(daoUuid);

		DaoBundle published = null;
		DaoBundle hiResView = null;
		DaoBundle thumbnail = null;

		if (configDao.getSend() != SendType.none) {
			// budu posilat originaly obrazku
			published = daoBuilder.createDaoBundle(DaoBundleType.PUBLISHED);
		}

		// originalni soubory k presunu, klic je novy nazev
		var filesToMove = new HashMap<String, Path>();

		// mapovani (nazev souboru,typ transformace)->pridelene uuid
		var filesToTransformAsync = getExistingMappings(daoUuid, false);

		var pos = 1;
		for (Path file : files) {
			String mimeType = tika.detect(file);
			log.debug("Processing {} file {}", mimeType, file);
			if (published != null) {
				processPublished(file, published, pos, filesToMove, mimeType, filesToTransformAsync, sourceDir);
			}
			if (mimeType != null && mimeType.startsWith("image/")) {
				log.info("Generating dzi and thumbnail for {}", file);
				if (hiResView == null) {
					hiResView = daoBuilder.createDaoBundle(DaoBundleType.HIGH_RES_VIEW);
				}
				if (thumbnail == null) {
					thumbnail = daoBuilder.createDaoBundle(DaoBundleType.THUMBNAIL);
				}
				processHiResView(file, filesDir, hiResView, pos, filesToTransformAsync);
				processThumbNail(file, filesDir, thumbnail, pos, filesToTransformAsync);
			}
			pos++;
		}

		// vytvoreni dao-uuid.xml
		log.debug("Creating file {}", daoUuidXmlFile);
		Marshaller marshaller = ApuxFactory.createMarshaller();
		try (OutputStream os = Files.newOutputStream(daoUuidXmlFile)) {
			marshaller.marshal(daoBuilder.build(), os);
		}

		// move original files
		log.debug("Moving all files to {}", filesDir);
		for (var entry : filesToMove.entrySet()) {
			Files.move(entry.getValue(), filesDir.resolve(entry.getKey()));
		}
		
		// delete remaining files
		try (Stream<Path> stream = Files.list(daoDir)) {
			stream.forEach(p -> {
				try {
					if (!Files.isDirectory(p) && !Files.isSameFile(p, daoUuidXmlFile)) {
						Files.delete(p);
					}
				} catch (IOException e) {
					log.error("Fail to delete file {}", p);
				}
			});
		}

		scheduleAsyncTransforms(daoUuid, filesToTransformAsync, false);
		return true;
	}
	
	
    public boolean transformHttpUrls(Path daoDir, List<String> urls) throws JAXBException, IOException {

        if (urls.isEmpty()) {
            return false;
        }

        var daoUuid = daoDir.getFileName().toString();
        var daoUuidXmlFile = daoDir.resolve("dao-" + daoUuid + ".xml");
        var filesDir = daoDir.resolve("files");
        Files.createDirectories(filesDir);

        // mapovani (nazev souboru,typ transformace)->pridelene uuid
        var filesToTransformAsync = getExistingMappings(daoUuid, true);

        DaoBuilder daoBuilder = new DaoBuilder();
        daoBuilder.setUuid(daoUuid);
        DaoBundle hiResView = daoBuilder.createDaoBundle(DaoBundleType.HIGH_RES_VIEW);

        var pos = 1;
        for (String url : urls) {
            processHttp(url,filesDir, hiResView, pos, filesToTransformAsync);
            pos++;
        }

        // vytvoreni dao-uuid.xml
        log.debug("Creating file {}", daoUuidXmlFile);
        Marshaller marshaller = ApuxFactory.createMarshaller();
        try (OutputStream os = Files.newOutputStream(daoUuidXmlFile)) {
            marshaller.marshal(daoBuilder.build(), os);
        }

        scheduleAsyncTransforms(daoUuid, filesToTransformAsync, true);
        return true;
    }

    private void processHttp(String url, Path filesDir, DaoBundle hiResView, int pos,
                             Map<FileTransformKey, String> filesToTransformAsync) {
        if (url == null) {
            throw new IllegalStateException(
                    "Source dir cannot be empty whe sending reaferences to digital object file.");
        }

        var name = url;
        var lastSlash = url.lastIndexOf('/');
        if (lastSlash!=-1&&url.length()>lastSlash+1) {
            name = url.substring(lastSlash+1);
        }        
        
        var existingUuid = filesToTransformAsync.get(new FileTransformKey(url, TRANSFORM_IDENTITY));
        var daoFile = DaoBuilder.createDaoFile(name, 0l, pos, "text/plain", existingUuid);
        var uuid = daoFile.getUuid();
        DaoBuilder.addReferenceFlag(daoFile);
        DaoBuilder.addPath(daoFile, url);
        hiResView.getFile().add(daoFile);
        if (existingUuid==null) {
            filesToTransformAsync.put(new FileTransformKey(url, TRANSFORM_IDENTITY), uuid);
        }
    }

	private void scheduleAsyncTransforms(String daoUuid, Map<FileTransformKey, String> filesToTransformAsync, boolean forceGetMappings) {
		// plan async transforms
		if (!filesToTransformAsync.isEmpty()) {
			transactionTemplate.executeWithoutResult(c -> {
				var existingTransforms = getExistingMappings(daoUuid, forceGetMappings);
				for (var entry : filesToTransformAsync.entrySet()) {
					var existing = existingTransforms.get(entry.getKey());
					if (existing == null) {
						// pokud mapovani neexistuje tako ho ulozim do databaze
						var transform = new Transform();
						transform.setDaoUuid(UUID.fromString(daoUuid));
						transform.setFile(entry.getKey().getFileName());
						transform.setFileUuid(UUID.fromString(entry.getValue()));
						if (TransformService.TRANSFORM_IDENTITY.equals(entry.getKey().getTransformation())) {
							// identity transformaci nebudu provadet
							transform.setState(TransformState.TRANSFORMED);
						} else {
							transform.setState(TransformState.READY);
						}
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
	}

	private static class FileTransformKey {
		private final String fileName;
		private final String transformation;

		public FileTransformKey(String fileName, String transformation) {
			this.fileName = fileName;
			this.transformation = transformation;
		}

		public String getFileName() {
			return fileName;
		}

		public String getTransformation() {
			return transformation;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fileName, transformation);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileTransformKey other = (FileTransformKey) obj;
			return Objects.equals(fileName, other.fileName) && Objects.equals(transformation, other.transformation);
		}

	}

	/**
	 * Precte z databaze existujici mapovani (file,transform_type)->uuid
	 * 
	 * @param daoUuid
	 * @param force pokud je true vrati vzdy jinak pouze pokud je neco async
	 * @return Map<FileTransformKey, String>
	 */
	private Map<FileTransformKey, String> getExistingMappings(String daoUuid, boolean force) {
		// mapovani nazev souboru->pridelene uuid
		var filesToTransformAsync = new HashMap<FileTransformKey, String>();
		if (tileAsync || thumbnailAsync || force) {
			var transforms = transformRepository.findAllByDaoUuid(UUID.fromString(daoUuid));
			for (var transform : transforms) {
				filesToTransformAsync.put(new FileTransformKey(transform.getFile(), transform.getType()),
						transform.getFileUuid().toString());
			}
		}
		return filesToTransformAsync;
	}

	/**
	 * Vrati seznam zdrojovych souboru k transformaci, soubory jsou serazeny v
	 * poradi v jakem maji byt zpracovany
	 * 
	 * @param dir adresar obsahujici zdrojove soubory
	 * @return List<Path>
	 */
	private List<Path> prepareFileList(Path dir) {
		List<Path> files;
		var bitstreamJson = dir.resolve(DSpaceConsts.BITSTREAM_JSON);
		if (Files.exists(bitstreamJson)) {
			files = readBitstreamJson(dir, bitstreamJson);
			try (Stream<Path> stream = Files.list(dir)) {
				stream.filter(f -> Files.isRegularFile(f) && !files.contains(f)).map(f -> f.toFile())
						.forEach(File::delete);
			} catch (IOException e) {
				log.error("Error getting file list from {} ", dir, e);
				throw new RuntimeException(e);
			}
		} else {
			try (Stream<Path> stream = Files.list(dir)) {
				files = stream.filter(f -> Files.isRegularFile(f)).collect(Collectors.toList());
			} catch (IOException e) {
				log.error("Error getting file list from {} ", dir, e);
				throw new RuntimeException(e);
			}
			files.sort((p1, p2) -> p1.getFileName().compareTo(p2.getFileName()));
		}
		return files;
	}

	private List<Path> readBitstreamJson(Path dir, Path bitstreamJson) {
		var filterBundle = configDspace.getBundleName();
		Map<Integer, Path> filesMap = new HashMap<>();
		try {
			var jsonReader = Json.createReader(Files.newInputStream(bitstreamJson));
			var jsonValue = jsonReader.readValue();
			for (var value : jsonValue.asJsonArray()) {
				var object = value.asJsonObject();

				if (filterBundle == null || filterBundle.equals(object.getString(DSpaceConsts.BUNDLE_NAME))) {
					var name = object.getString(DSpaceConsts.NAME);
					var id = object.getInt(DSpaceConsts.SEQUENCE_ID);

					Validate.notNull(name, "Název souboru nesmí být prázdný");
					Validate.notNull(id, "SequenceId nesmí být prázdný");

					filesMap.put(id, dir.resolve(name));
				}
			}
		} catch (IOException e) {
			log.error("Error reading file {} ", bitstreamJson, e);
			throw new RuntimeException(e);
		}
		return filesMap.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	private void processThumbNail(Path file, Path filesDir, DaoBundle thumbnails, int pos,
			Map<FileTransformKey, String> filesToTransformAsync) {

		var fileName = file.getFileName().toString();
		// pokud jiz bylo uuid prirazeno, tak ho zrecykluju
		var existingUuid = filesToTransformAsync.get(new FileTransformKey(fileName, TRANSFORM_THUMBNAIL));
		var daoFile = DaoBuilder.createDaoFile(pos, "image/jpeg", existingUuid);
		var uuid = daoFile.getUuid();
		thumbnails.getFile().add(daoFile);
		if (thumbnailAsync) {
			// vytori se asynchrone a predava se reference na soubor
			if (thumbnailFolder == null) {
				throw new IllegalStateException("Pri asynchronnim vytvareni thumbnailu musi byt zadany adresar");
			}
			DaoBuilder.addReferenceFlag(daoFile);
			DaoBuilder.addPath(daoFile, getThumbnailPath(uuid).toString());
			filesToTransformAsync.put(new FileTransformKey(fileName, TRANSFORM_THUMBNAIL), uuid);
		} else {
			Path thumbnailsFile = null;
			if (thumbnailFolder != null) {
				// rovnou vytvorim do adresare a predam referenci
				thumbnailsFile = getThumbnailPath(uuid);
				DaoBuilder.addReferenceFlag(daoFile);
				DaoBuilder.addPath(daoFile, thumbnailsFile.toString());
			} else {
				// vytvorim data odesilana do arona
				thumbnailsFile = filesDir.resolve("file-" + uuid);
			}
			log.debug("Creating thumbnails file {}", thumbnailsFile);

			try (OutputStream os = Files.newOutputStream(thumbnailsFile)) {
				Thumbnails.of(file.toFile()).outputFormat("jpg").size(120, 120).toOutputStream(os);
			} catch (IOException e) {
				log.error("Error creating thumbnails src={}, target={} ", file, thumbnailsFile, e);
				throw new RuntimeException(e);
			}
		}
	}

	private void processHiResView(Path file, Path filesDir, DaoBundle hiResView, int pos,
			Map<FileTransformKey, String> filesToTransformAsync) {

		var fileName = file.getFileName().toString();
		var existingUuid = filesToTransformAsync.get(new FileTransformKey(fileName, TRANSFORM_DZI));

		var daoFile = DaoBuilder.createDaoFile(pos, "application/octetstream", existingUuid);
		var uuid = daoFile.getUuid();
		hiResView.getFile().add(daoFile);
		if (tileFolder != null) {
			if (tileAsync) {
				filesToTransformAsync.put(new FileTransformKey(file.getFileName().toString(), TRANSFORM_DZI), uuid);
			} else {
				createDziOut(file, uuid);
			}
			DaoBuilder.addReferenceFlag(daoFile);
		} else {
			createDzi(file, filesDir.resolve("file-" + uuid));
		}
	}

	private void processPublished(Path file, DaoBundle published, int pos, Map<String, Path> filesToMove,
			String mimeType, Map<FileTransformKey, String> filesToTransformAsync, Path sourceDir) {
		var fileName = file.getFileName().toString();
		long fileSize;
		try {
			fileSize = Files.size(file);
		} catch (IOException e) {
			log.error("Failed to read file size, path: {} ", file, e);
			throw new RuntimeException(e);
		}
		String existingUuid = filesToTransformAsync.get(new FileTransformKey(fileName, TRANSFORM_IDENTITY));
		var daoFile = DaoBuilder.createDaoFile(fileName, fileSize, pos, mimeType, existingUuid);
		var uuid = daoFile.getUuid();
		if (configDao.getSend() == SendType.data) {
			filesToMove.put("file-" + uuid, file);
		} else if (configDao.getSend() == SendType.reference) {
			if (sourceDir==null) {
				throw new IllegalStateException("Source dir cannot be empty whe sending reaferences to digital object file.");
			}
			Path sourceFile = sourceDir.resolve(file.getFileName());
			DaoBuilder.addReferenceFlag(daoFile);
			DaoBuilder.addPath(daoFile, sourceFile.toString());
		}
		published.getFile().add(daoFile);
	}

	private void createDzi(Path sourceImage, Path targetFile) {
		log.debug("Creating hiResView file {}", targetFile);

		var prefix = "dzi_" + sourceImage.getFileName().toString() + "_";
		Path tempDir;
		try {
			tempDir = storageService.createTempDir(prefix);
		} catch (IOException e) {
			log.error("Error creating tempDir prefix={} ", prefix, e);
			throw new RuntimeException(e);
		}

		boolean deleteCreated = true;
		try {
			ScalablePyramidBuilder spb = new ScalablePyramidBuilder(254, 1, "jpg", "dzi");
			FilesArchiver archiver = new DirectoryArchiver(tempDir.toFile());
			PartialImageReader pir = new BufferedImageReader(sourceImage.toFile());
			spb.buildPyramid(pir, "image", archiver, Runtime.getRuntime().availableProcessors());
			try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(targetFile))) {
				Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
						try {
							StringJoiner sj = new StringJoiner("/");
							Path targetFile = tempDir.relativize(file);
							targetFile.forEach(p -> sj.add(p.toString()));
							outputStream.putNextEntry(new ZipEntry(sj.toString()));
							Files.copy(file, outputStream);
							outputStream.closeEntry();
						} catch (IOException e) {
							log.error("Error in copying file, source: {}", file, e);
							throw new RuntimeException(e);
						}
						return FileVisitResult.CONTINUE;
					}
				});
			}
			deleteCreated = false;
		} catch (IOException e) {
			log.error("Error in creating ZIP file {}", targetFile, e);
			throw new RuntimeException(e);
		} finally {
			try {
				log.debug("Deleting directory {}", tempDir);
				if (!FileSystemUtils.deleteRecursively(tempDir)) {
					log.warn("Fail to delete temp directory");
				}
				if (deleteCreated) {
					log.debug("Deleting file {}", tempDir);
					Files.deleteIfExists(targetFile);
				}
			} catch (IOException e) {
				log.error("Error deleting unused file(s) ", e);
				throw new RuntimeException(e);
			}
		}
	}

	public void createThumbnailOut(Path sourceImage, String uuid) {		
		Path thumbnailsFile = getThumbnailPath(uuid);
		try {
			Files.createDirectories(thumbnailsFile.getParent());
		} catch (IOException e) {
			log.error("Error creating dir prefix={} ", thumbnailsFile.getParent(), e);
			throw new RuntimeException(e);
		}
		try (OutputStream os = Files.newOutputStream(thumbnailsFile)) {
			Thumbnails.of(sourceImage.toFile()).outputFormat("jpg").size(120, 120).toOutputStream(os);
		} catch (IOException e) {
			log.error("Error creating thumbnails src={}, target={} ", sourceImage, thumbnailsFile, e);
			throw new UntransformableEntityException(e);
		}
	}

	public void createDziOut(Path sourceImage, String id) {
		log.debug("Creating hiResView file {}", id);

		Path path = getTileDir(id);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			log.error("Error creating dir prefix={} ", path, e);
			throw new RuntimeException(e);
		}

		boolean deleteCreated = true;
		try {
			ScalablePyramidBuilder spb = new ScalablePyramidBuilder(254, 1, "jpg", "dzi");
			FilesArchiver archiver = new DirectoryArchiver(path.toFile());
			PartialImageReader pir = new BufferedImageReader(sourceImage.toFile());
			spb.buildPyramid(pir, "image", archiver, Runtime.getRuntime().availableProcessors());
			deleteCreated = false;
			log.info("Dzi created for id {}", id);
		} catch (IOException e) {
			log.error("Error in creating dzi {}", path, e);
			throw new UntransformableEntityException(e);
		} finally {
			try {
				if (deleteCreated) {
					log.debug("Deleting file {}", path);
					if (!FileSystemUtils.deleteRecursively(path)) {
						log.warn("Fail to delete dzi directory", path);
					}
				}
			} catch (IOException e) {
				log.error("Error deleting unused file(s) ", e);
				throw new RuntimeException(e);
			}
		}
	}

	private Path getPath(String id, int hierarchicalLevel, String folder) {
		String[] path = new String[hierarchicalLevel + 1];
		path[hierarchicalLevel] = id;
		String uuid = id.replaceAll("-", "");
		for (int i = 0; i < hierarchicalLevel; i++) {
			path[i] = uuid.substring(i * DIR_NAME_LENGTH, i * DIR_NAME_LENGTH + DIR_NAME_LENGTH);
		}
		return Paths.get(folder, path);
	}

	public Path getTileDir(String id) {
		return getPath(id, tileHierarchicalLevel, tileFolder);
	}

	public Path getThumbnailPath(String uuid) {
		return getPath(uuid+".jpg", thumbnailHierarchicalLevel, thumbnailFolder);
	}
	
	public static class UntransformableEntityException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public UntransformableEntityException(Throwable t) {
			super(t);
		}
		
	}

	
    public static void main(String[] args) throws Exception {
        
        var img = ImageIO.read(new File(
                "D:\\projects\\aron\\data\\transfagent\\input_folder\\input\\attachments\\213000010\\AS\\20143\\soap-pn_ap0226_20081_vs-merklin_0010.jp2"));        
        ImageIO.write(img, "jpg", new File("D:\\out.jpg"));
        

        ScalablePyramidBuilder spb = new ScalablePyramidBuilder(254, 1, "jpg", "dzi");
        FilesArchiver archiver = new DirectoryArchiver(new File("D:\\out.dzi"));
        PartialImageReader pir = new BufferedImageReader(new File(
                "D:\\projects\\aron\\data\\transfagent\\input_folder\\input\\attachments\\213000010\\AS\\20143\\soap-pn_ap0226_20081_vs-merklin_0010.jp2"));
        spb.buildPyramid(pir, "image", archiver, Runtime.getRuntime().availableProcessors());

    }
	
}
