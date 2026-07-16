package cz.aron.customizations.soaplzen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.base.Objects;

import cz.aron.core.api.rest.model.Result;
import cz.aron.customizations.soaplzen.ImportArchDescResultBuilderImpl.ThumbnailFileStoreI;
import cz.aron.transfagent.common.MonitoredFileResource;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ThumbnailFileStore implements ThumbnailFileStoreI {
	
	private static final Logger log = LoggerFactory.getLogger(ThumbnailFileStore.class);
	
	private final Path path;
	
	private final Path webPath;
	
	private final ImagesLink imagesLink;
	
	public ThumbnailFileStore(ConfigDaoFileStore5 config) {
		this.path = config.getPath();
		this.webPath = path.resolve("web").resolve("public");
		this.imagesLink = new ImagesLink(path.resolve("web").resolve("aron").resolve("alias2digi.csv"));
	}

	public void fillThumbnailLink(String daoLink, Result result) {		
		if (!daoLink.startsWith("https://www.portafontium.eu/")) {
			return;
		}
		// link do portafontium
		result.setTnLink(daoLink);
		
		var alias = daoLink.substring("https://www.portafontium.eu/".length());				
		var thumbnailLinks= imagesLink.getResource();
		var imagesLink = thumbnailLinks.getAlias2ImageUrl().get(alias);
		if (imagesLink != null) {
			if (imagesLink.getThumbnail() != null) {
				result.setTn(createPfImageServerLink(imagesLink.getDigi() + "/" + imagesLink.getThumbnail()+".jp2"));
				return;
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
					result.setTn("pf.svg");					
					return;
				} catch (IOException e) {
					log.error("Fail to read directory", e);
					return;
				}																
				Collections.sort(files);
				if (!files.isEmpty()) {
					result.setTn(createPfImageServerLink(imagesLink.getDigi() + "/" + files.get(0)));
					return;
				}
			}
		} else {
			// defaultni obrazek pontafontium
			result.setTn("pf.svg");
		}
	}	
	
	@Override
	public boolean fillDaoIdLink(String daoId, Result result) {
		var daoIdLink = getDaoIdLink(daoId);
		if (daoIdLink == null) {
			return false;
		}
		result.setTn(daoIdLink.getThumbnail());
		// link do portafontium
		result.setTnLink(daoIdLink.getLink());
		return true;
	}

	/**
	 * Vytvori odkaz do portafontium a nahled pro dany daoId.
	 * @param daoId identifikator dao ve formatu {@code digi;preview=thumbnail}
	 * @return odkaz do portafontium spolu s nahledem nebo {@code null} pokud odkaz neexistuje
	 */
	public DaoIdLink getDaoIdLink(String daoId) {
		var splitted = daoId.split(";");
		if (splitted.length != 2) {
			return null;
		}
		if (!splitted[1].startsWith("preview=")) {
			return null;
		}
		var thumbnail = splitted[1].substring("preview=".length());
		var digi = splitted[0];

		var thumbnailLinks = imagesLink.getResource().getDigi2ImageUrl();
		var imagesLink = thumbnailLinks.get(digi);
		if (imagesLink == null || imagesLink.getThumbnail() == null
				|| !Objects.equal(imagesLink.getThumbnail(), thumbnail)) {
			return null;
		}
		var link = "https://www.portafontium.eu/iipimage/" + imagesLink.getNid() + "/" + imagesLink.getThumbnail();
		var tn = createPfImageServerLink(imagesLink.getDigi() + "/" + imagesLink.getThumbnail() + ".jp2");
		return new DaoIdLink(link, tn);
	}

	/**
	 * Odkaz do portafontium ({@code link}) a odkaz na nahled ({@code thumbnail}).
	 */
	public static class DaoIdLink {

		private final String link;

		private final String thumbnail;

		public DaoIdLink(String link, String thumbnail) {
			this.link = link;
			this.thumbnail = thumbnail;
		}

		public String getLink() {
			return link;
		}

		public String getThumbnail() {
			return thumbnail;
		}

	}

	private String createPfImageServerLink(String path) {		
		StringBuilder sb = new StringBuilder();
		sb.append("https://www.portafontium.eu/fcgi-bin/iipsrv.fcgi?FIF=");
		sb.append(path);
		sb.append("&wid=400&rot=0&rfm=0&cvt=jpeg");
		return sb.toString();				
	}
	
	private class ImagesLink extends MonitoredFileResource<ImageUrls> {

		private ImagesLink(Path path) {
			super(path);
		}

		@Override
		public ImageUrls reloadResources() {
			if (monitoredPath == null || !Files.isRegularFile(monitoredPath)) {
				log.info("ThunbnailLinks path not exist {}", monitoredPath);
				return new ImageUrls(Collections.emptyMap(), Collections.emptyMap());
			}
			var alias2ImageUrl = new HashMap<String, ImageUrl>();
			var digi2ImageUrl = new HashMap<String, ImageUrl>();
			try (var is = new FileInputStream(monitoredPath.toFile());
					var bis = new BOMInputStream(is);
					Reader in = new InputStreamReader(bis);) {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').setTrim(true).build()
						.parse(in);
				for (CSVRecord record : records) {
					var alias = record.get(1);
					var digi = record.get(2);
					var thumbnail = StringUtils.defaultIfBlank(record.get(3), null);					
					var imageUrl = new ImageUrl(digi, thumbnail, record.get(0));					
					alias2ImageUrl.put(alias, imageUrl);
					digi2ImageUrl.put(digi, imageUrl);
				}
			} catch (FileNotFoundException e) {
				log.error("ThumbnailLinks fail to read {}", monitoredPath);
			} catch (IOException e) {
				log.error("ThumbnailLinks fail to read {}", monitoredPath);
			}
			if (alias2ImageUrl.isEmpty()) {
				log.warn("ThumbnailLinks links loaded from path {} NO DATA!!!", monitoredPath);
			} else {
				log.info("ThumbnailLinks links loaded from path {}", monitoredPath);
			}
			return new ImageUrls(alias2ImageUrl, digi2ImageUrl);
		}

	}
	
	
	private static class ImageUrls {
		
		private final Map<String, ImageUrl> alias2ImageUrl;
		
		private final Map<String, ImageUrl> digi2ImageUrl;

		public ImageUrls(Map<String, ImageUrl> alias2ImageUrl, Map<String, ImageUrl> digi2ImageUrl) {
			this.alias2ImageUrl = alias2ImageUrl;
			this.digi2ImageUrl = digi2ImageUrl;
		}

		public Map<String, ImageUrl> getAlias2ImageUrl() {
			return alias2ImageUrl;
		}

		public Map<String, ImageUrl> getDigi2ImageUrl() {
			return digi2ImageUrl;
		}
				
	}


	private static class ImageUrl {
		
		private final String digi;
		
		private final String thumbnail;
		
		private final String nid;

		public ImageUrl(String digi, String thumbnail, String nid) {
			this.digi = digi;
			this.thumbnail = thumbnail;
			this.nid = nid; 
		}

		public String getDigi() {
			return digi;
		}

		public String getThumbnail() {
			return thumbnail;
		}

		public String getNid() {
			return nid;
		}

	} 

}
