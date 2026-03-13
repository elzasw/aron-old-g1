package cz.aron.customizations.generic;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import cz.aron.core.api.rest.model.Result;
import cz.aron.customizations.soaplzen.ImportArchDescResultBuilderImpl.ThumbnailFileStoreI;
import cz.aron.transfagent.common.ConditionalOnAnyProperty;

@Service
@Primary
@ConditionalOnAnyProperty(name = {"result.archdescthumbnail.default-icon", "result.archdescthumbnail.prefixes[0].icon"})
public class ThumbnailBuilderGeneric  implements ThumbnailFileStoreI {
	
	private static final Logger log = LoggerFactory.getLogger(ThumbnailBuilderGeneric.class);
	
	private final List<UrlPrefixIcon> prefixes = new ArrayList<>(); 
	
	public ThumbnailBuilderGeneric(ArchdescThumbnailProperties thumbnailProperties) {
		if (thumbnailProperties.getPrefixes() != null) {
			thumbnailProperties.getPrefixes().forEach(p -> {
				prefixes.add(new UrlPrefixIcon(p.getIcon(), p.getPrefix()));
			});
		}
		if (thumbnailProperties.getDefaultIcon() != null) {
			prefixes.add(new UrlPrefixIconDefault(thumbnailProperties.getDefaultIcon()));
		}
		log.info("ThumbnailBuilderGeneric configured.");
	}

	@Override
	public void fillThumbnailLink(String daoLink, Result result) {
		for (var prefix : prefixes) {
			if (prefix.isPrefix(daoLink)) {
				result.setTnLink(daoLink);
				result.setTn(prefix.getIcon());
				return;
			}
		}
	}
	
	private static class UrlPrefixIcon {

		private final String icon;

		private final String urlPrefix;

		public UrlPrefixIcon(String icon, String urlPrefix) {
			this.icon = icon;
			this.urlPrefix = urlPrefix;
		}

		public boolean isPrefix(String url) {
			return url.startsWith(urlPrefix);
		}

		public String getIcon() {
			return icon;
		}

	}

	private static class UrlPrefixIconDefault extends UrlPrefixIcon {

		public UrlPrefixIconDefault(String icon) {
			super(icon, "");
		}

		@Override
		public boolean isPrefix(String url) {
			return true;
		}

	}
	
	@Component
	@ConfigurationProperties(prefix = "result.archdescthumbnail")
	public static class ArchdescThumbnailProperties {

	    private String defaultIcon;
	    private List<PrefixIcon> prefixes;

	    public String getDefaultIcon() {
	        return defaultIcon;
	    }

	    public void setDefaultIcon(String defaultIcon) {
	        this.defaultIcon = defaultIcon;
	    }

	    public List<PrefixIcon> getPrefixes() {
	        return prefixes;
	    }

	    public void setPrefixes(List<PrefixIcon> prefixes) {
	        this.prefixes = prefixes;
	    }

	    public static class PrefixIcon {

	        private String icon;
	        private String prefix;

	        public String getIcon() {
	            return icon;
	        }

	        public void setIcon(String icon) {
	            this.icon = icon;
	        }

	        public String getPrefix() {
	            return prefix;
	        }

	        public void setPrefix(String prefix) {
	            this.prefix = prefix;
	        }
	    }
	}	

}
