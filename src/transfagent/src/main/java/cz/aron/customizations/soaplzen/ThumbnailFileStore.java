package cz.aron.customizations.soaplzen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.common.MonitoredFileResource;
import cz.aron.transfagent.config.ConfigDaoFileStore5;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ThumbnailFileStore {
	
	private static final Logger log = LoggerFactory.getLogger(ThumbnailFileStore.class);
	
	private final Path path;
	
	private final Path webPath;
	
	private final ImagesLink imagesLink;
	
	public ThumbnailFileStore(ConfigDaoFileStore5 config) {
		this.path = config.getPath();
		this.webPath = path.resolve("web").resolve("public");
		this.imagesLink = new ImagesLink(path.resolve("web").resolve("aron").resolve("alias2digi.csv"));
	}

	public String getThumbnailLink(String daoLink) {		
		if (!daoLink.startsWith("https://www.portafontium.eu/")) {
			return null;
		}
		var alias = daoLink.substring("https://www.portafontium.eu/".length());				
		var thumbnailLinks= imagesLink.getResource();
		var imagesLink = thumbnailLinks.get(alias);
		if (imagesLink != null) {
			if (imagesLink.getThumbnail() != null) {
				return daoLink + "/" + imagesLink.getThumbnail()+".jp2";
			} else {
				var digiDir = webPath.resolve(webPath.resolve(imagesLink.getDigi()));
				var files = new ArrayList<String>();
				try (var stream = Files.list(digiDir)) {
					stream.forEach(p->{
						if (Files.isRegularFile(p)) {
							files.add(p.getFileName().toString());
						}
					});
				} catch (NoSuchFileException e) {
					log.warn("Directory not exist {}", digiDir);
					return null;
				} catch (IOException e) {
					log.error("Fail to read directory", e);
					return null;
				}				
				Collections.sort(files);
				if (!files.isEmpty()) {
					return daoLink + "/" + files.get(0);
				}
			}
		}		
		return null;
	}
	
	private class ImagesLink extends MonitoredFileResource<Map<String, ImageUrl>> {

		private ImagesLink(Path path) {
			super(path);
		}

		@Override
		public Map<String, ImageUrl> reloadResources() {
			if (monitoredPath == null || !Files.isRegularFile(monitoredPath)) {
				log.info("ThunbnailLinks path not exist {}", monitoredPath);
				return Collections.emptyMap();
			}

			var ret = new HashMap<String, ImageUrl>();
			try (Reader in = new FileReader(monitoredPath.toFile());) {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').setTrim(true).build()
						.parse(in);
				for (CSVRecord record : records) {
					var alias = record.get(0);
					var digi = record.get(1);
					var thumbnail = StringUtils.defaultIfBlank(record.get(2), null);
					ret.put(alias, new ImageUrl(digi, thumbnail));
				}
			} catch (FileNotFoundException e) {
				log.error("ThumbnailLinks fail to read {}", monitoredPath);
			} catch (IOException e) {
				log.error("ThumbnailLinks fail to read {}", monitoredPath);
			}
			if (ret.isEmpty()) {
				log.warn("ThumbnailLinks links loaded from path {} NO DATA!!!", monitoredPath);
			} else {
				log.info("ThumbnailLinks links loaded from path {}", monitoredPath);
			}
			return ret;
		}

	}	
	
	private static class ImageUrl {
		
		private final String digi;
		
		private final String thumbnail;

		public ImageUrl(String digi, String thumbnail) {
			this.digi = digi;
			this.thumbnail = thumbnail;
		}

		public String getDigi() {
			return digi;
		}

		public String getThumbnail() {
			return thumbnail;
		}
		
	} 
	
}
