package cz.aron.transfagent.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Elza filesystem digital repositories ({@code elzafs.*}). Elza's export names
 * a DAO only by repository code and repository-relative path; this mapping
 * says under which locally accessible root (a mount of the repository) each
 * configured code is found. A repository not listed here is not imported.
 */
@Configuration
@ConfigurationProperties(prefix = "elzafs")
public class ConfigElzaFs {

    private List<FsRepo> repos = new ArrayList<>();

    /**
     * Nejvyssi pocet souboru importovanych z jednoho adresaroveho odkazu.
     * Odkaz na vetsi adresar (typicky omylem na koren repozitare) se oznaci
     * jako INACCESSIBLE misto aby se generovaly tisice dlazdic.
     */
    private int maxFiles = 200;

    public List<FsRepo> getRepos() {
        return repos;
    }

    public void setRepos(List<FsRepo> repos) {
        this.repos = repos;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public static class FsRepo {

        /** Kod externiho systemu (digitalniho repozitare) v Elze. */
        private String code;

        /** Lokalne dostupny koren repozitare (typicky mount). */
        private Path path;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }
    }

}
