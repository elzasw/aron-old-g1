package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.domain.Property;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.tacr.elza.ApiException;
import cz.tacr.elza.client.PublicationApi;

/**
 * Discovery stage of the ELZA archdesc pull-synchronization.
 * <p>
 * Registered into the {@link FileImportService} processor chain with a negative
 * {@link #getPriority() priority}, so it runs only after the regular (positive-priority)
 * importers are idle. On each polling cycle it asks the remote ELZA <em>Publication</em> API for
 * archdescs published since the last seen transaction and, for every new publication, touches a
 * {@code publication-<id>} marker file in {@code input/commands/}. Those markers are later picked
 * up by {@link ImportPublicationCommand}, which downloads the data into the standard {@code archdesc}
 * import directory.
 * <p>
 * Synchronization is incremental: the id of the last processed transaction is persisted in the
 * {@code Property} table under {@link #ELZA_LAST_ARCHDESC_TRANSACTION}, so it survives restarts and
 * resumes from where it left off. The whole feature is active only when {@code elza.url} is configured.
 *
 * @see ImportPublicationCommand
 * @see ReportPublicationStatusCommand
 * @see ApiClientEx
 */
@ConditionalOnProperty(value = "elza.syncpublication")
@Service
public class ElzaUpdateArchDescProcessor implements ImportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ElzaUpdateArchDescProcessor.class);

    /** Name of the {@code Property} row holding the id of the last processed ELZA transaction (sync watermark). */
    public static final String ELZA_LAST_ARCHDESC_TRANSACTION = "ELZA_LAST_ARCHDESC_TRANSACTION";

    private final FileImportService importService;

    private final PropertyRepository propertyRepository;
    
    private final ApiClientEx apiClient;

    private final PublicationApi publicationApi;

    private final ConfigElza configElza;

    private final StorageService storageService;

    public ElzaUpdateArchDescProcessor(FileImportService importService, PropertyRepository propertyRepository,
            ApiClientEx apiClient, ConfigElza configElza, StorageService storageService) {
        this.importService = importService;
        this.propertyRepository = propertyRepository;
        this.apiClient = apiClient;
        this.publicationApi = new PublicationApi(apiClient);
        this.configElza = configElza;
        this.storageService = storageService;
    }

    /**
     * Touch a command file named {@code publication-${publicationId}} in the commands directory.
     *
     * @param publicationId id of the publication
     * @return {@code true} if a new command file was created, {@code false} if it already existed
     */
    private boolean createPublicationCommand(String publicationId) {
        var commandsPath = storageService.getInputPath().resolve("commands");
        try {
            Path commandFile = commandsPath.resolve("publication-" + publicationId);
            if (!Files.exists(commandFile)) {
                Files.createFile(commandFile);
                log.info("Created publication command {}", commandFile);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Fail to create publication command for publicationId {}", publicationId, e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Register this processor into the {@link FileImportService} chain after the bean is constructed.
     */
    @PostConstruct
    void init() {
        importService.registerImportProcessor(this);
    }

    /**
     * Store last processed transaction to the property table.
     *
     * @param transaction last processed transaction
     */
    private void storeLastTransaction(String transaction) {
        var property = propertyRepository.findByName(ELZA_LAST_ARCHDESC_TRANSACTION);
        if (property == null) {
            property = new Property();
            property.setName(ELZA_LAST_ARCHDESC_TRANSACTION);
        }
        property.setValue(transaction);
        propertyRepository.save(property);
        log.info("Publication next transaction stored {}", transaction);
    }

    /**
     * Negative priority, so this processor runs after the regular importers in the
     * {@link FileImportService} chain (higher priority runs first).
     */
    @Override
    public int getPriority() {
        return -50;
    }

    /**
     * Poll ELZA for newly published archdescs and enqueue a {@code publication-<id>} command for each.
     * <p>
     * Logs in if needed, reads the {@link #ELZA_LAST_ARCHDESC_TRANSACTION} watermark, queries the
     * available publications since that transaction, creates a command marker for every publication not
     * already pending, and advances the watermark when it changed. The {@link ImportContext} is marked
     * {@link ImportContext#addProcessed() processed} for each newly created marker, and
     * {@link ImportContext#setFailed(boolean) failed} (without advancing the watermark) if login or the
     * ELZA call fails, so {@link FileImportService} backs off and retries the same window next cycle.
     * A {@code 401} answer means the Elza session expired, so the cached session is dropped and the
     * call is retried once with a fresh login.
     *
     * @param ic shared import context for this polling cycle
     */
    @Override
    public void importData(ImportContext ic) {

        var lastTransactionProp = propertyRepository.findByName(ELZA_LAST_ARCHDESC_TRANSACTION);
        var lastTransaction = lastTransactionProp == null ? null : lastTransactionProp.getValue();
        log.info("Elza archdesc synchronization initialized, last transaction: {}", lastTransaction);

        // Elza authenticates by a session cookie. When the session dies on the server side (timeout,
        // restart) the cookie is still cached here, loginIfNeed() is a no-op and every following call
        // keeps failing with 401 until restart. So on 401 drop the session and log in again once.
        for (int attempt = 0; attempt < 2; attempt++) {
            if (!loginIfNeed()) {
                ic.setFailed(true);
                return;
            }
            try {
                var availablePublications = publicationApi
                        .publicationGetAvailablePublications(configElza.getSystemName(), lastTransaction);
                for (var availablePublication : availablePublications.getItems()) {
                    var publicationId = availablePublication.getPublicationId();
                    if (createPublicationCommand(publicationId.toString())) {
                        ic.addProcessed();
                    }
                }
                if (!Objects.equals(lastTransaction, availablePublications.getNextTransaction())) {
                    storeLastTransaction(availablePublications.getNextTransaction());
                }
                return;
            } catch (ApiException e) {
                apiClient.clearCookie();
                if (e.getCode() == 401 && attempt == 0) {
                    log.info("Elza session expired, logging in again");
                    continue;
                }
                log.error("Fail to get available publications from Elza, last transaction: {}", lastTransaction, e);
                ic.setFailed(true);
                return;
            }
        }
    }
    
    /**
     * Ensure the Elza API client is logged in.
     *
     * @return {@code true} if logged in, {@code false} if the login failed
     */
    private boolean loginIfNeed() {
        try {
            apiClient.loginIfNeed();
            return true;
        } catch (Exception e) {
            log.error("Fail to login to Elza", e);
            apiClient.clearCookie();
            return false;
        }
    }

}
