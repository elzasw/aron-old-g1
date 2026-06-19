package cz.aron.transfagent.elza.synchronization;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.transfagent.config.ConfigElza;
import cz.tacr.elza.ApiClient;
import cz.tacr.elza.ApiException;
import cz.tacr.elza.ApiResponse;

@ConditionalOnProperty(value = "elza.url")
@Component
public class ApiClientEx extends ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClientEx.class);

    private static final String EXPORT_SERVICE_SUFFIX = "/services/ExportService";

    private final CookieManager cookieManager = new CookieManager();

    private final String username;

    private final String password;

    private final URI loginUri;

    private HttpClient created = null;

    /**
     * Spring managed constructor, derives login and api uri from the configured Elza url.
     *
     * @param configElza Elza configuration
     */
    @Autowired
    public ApiClientEx(ConfigElza configElza) {
        this(configElza.getUsername(), configElza.getPassword(), elzaBaseUrl(configElza.getUrl()) + "/login",
                elzaBaseUrl(configElza.getUrl()) + "/api/v1");
    }

    public ApiClientEx(String username, String password, String loginUri, String baseUri) {
        super();
        this.username = username;
        this.password = password;
        this.loginUri = URI.create(loginUri);
        updateBaseUri(baseUri);
        var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).cookieHandler(cookieManager);
        setHttpClientBuilder(builder);
    }

    private static String elzaBaseUrl(String url) {
        return url.substring(0, url.indexOf(EXPORT_SERVICE_SUFFIX));
    }

    @Override
    public HttpClient getHttpClient() {
        if (created != null) {
            return created;
        }
        return created = super.getHttpClient();
    }

    public synchronized void loginIfNeed() throws ApiException {
        if (!cookieManager.getCookieStore().getCookies().isEmpty()) {
            return;
        }
        login();
    }

    public void login() throws ApiException {
        // Form parameters
        Map<Object, Object> data = Map.of("username", username, "password", password);
        boolean success = false;
        try {
            // Encode as application/x-www-form-urlencoded
            String form = data.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8) + "="
                            + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            var request = HttpRequest.newBuilder().uri(loginUri)
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();

            try {
                var response = created.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("Fail to login to Elza, status code {}, response {}", response.statusCode(),
                            response.body());
                    throw new ApiException();
                }
                log.info("Login to Elza successful");
                success = true;
            } catch (IOException e) {
                log.error("Fail to login to elza", e);
                throw new ApiException();
            } catch (InterruptedException e) {
                log.error("Fail to login to elza", e);
                throw new ApiException(e);
            }
        } finally {
            if (!success) {
                clearCookie();
            }
        }
    }

    public void clearCookie() {
        cookieManager.getCookieStore().removeAll();
    }

    /**
     * Create request for getting export file
     * @param requestId id of request
     * @return {@code HttpRequest.Builder}
     * @throws ApiException
     *
     * TODO: this method exist because of fail to generate from openapi
     */
    private HttpRequest.Builder ioGetExportFileRequestBuilder(Integer requestId) throws ApiException {
        // verify the required parameter 'requestId' is set
        if (requestId == null) {
            throw new ApiException(400, "Missing the required parameter 'requestId' when calling ioGetExportFile");
        }

        HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

        String localVarPath = "/io/file/{requestId}".replace("{requestId}", ApiClient.urlEncode(requestId.toString()));

        localVarRequestBuilder.uri(URI.create(getBaseUri() + localVarPath));

        localVarRequestBuilder.header("Accept", "*/*");

        localVarRequestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());
        if (getReadTimeout() != null) {
            localVarRequestBuilder.timeout(getReadTimeout());
        }
        return localVarRequestBuilder;
    }

    /**
     * Download export file from Elza
     *
     * @param requestId id of requesy
     * @param path where to donload file
     * @return {@code ApiResponse<Path>}
     * @throws ApiException
     *
     * TODO: this method exist because of fail to generate from openapi
     */
    public ApiResponse<Path> ioGetExportFileWithHttpInfo(Integer requestId, Path path) throws ApiException {
        HttpRequest.Builder localVarRequestBuilder = ioGetExportFileRequestBuilder(requestId);
        boolean deleteFile = true; // delete downloaded file when fail
        try {
            HttpResponse<InputStream> localVarResponse = created.send(localVarRequestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try {
                if (localVarResponse.statusCode() / 100 != 2) {
                    log.error("Fail to get export file, status code {}", localVarResponse.statusCode());
                    throw new ApiException(localVarResponse.statusCode(), "");
                }
                Files.copy(localVarResponse.body(), path, StandardCopyOption.REPLACE_EXISTING);
                deleteFile = false;
                return new ApiResponse<Path>(localVarResponse.statusCode(), localVarResponse.headers().map(),
                        path
                );
            } finally {
                try {
                    localVarResponse.body().close();
                } catch (IOException ioEx) {
                    log.warn("Fail to close response body", ioEx);
                }
            }
        } catch (IOException e) {
            throw new ApiException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(e);
        } finally {
            if (deleteFile) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Create request for getting export file
     * @param publicationId id of request
     * @return {@code HttpRequest.Builder}
     * @throws ApiException
     *
     * TODO: this method exist because of fail to generate from openapi
     */
    private HttpRequest.Builder publicationDownloadPublicationRequestBuilder(Integer publicationId) throws ApiException {
        // verify the required parameter 'requestId' is set
        if (publicationId == null) {
            throw new ApiException(400, "Missing the required parameter 'publicationId' when calling publicationDownloadPublication");
        }

        HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

        String localVarPath = "/publications/download/{id}".replace("{id}", ApiClient.urlEncode(publicationId.toString()));

        localVarRequestBuilder.uri(URI.create(getBaseUri() + localVarPath));

        localVarRequestBuilder.header("Accept", "*/*");

        localVarRequestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());
        if (getReadTimeout() != null) {
            localVarRequestBuilder.timeout(getReadTimeout());
        }
        return localVarRequestBuilder;
    }

    /**
     * Download publication file from Elza
     *
     * @param publicationId id of publication
     * @param path where to donload file
     * @return {@code ApiResponse<Path>}
     * @throws ApiException
     *
     * TODO: this method exist because of fail to generate from openapi
     */
    public ApiResponse<Path> publicationDownloadPublication(Integer publicationId, Path path) throws ApiException {
    	 HttpRequest.Builder localVarRequestBuilder = publicationDownloadPublicationRequestBuilder(publicationId);
         boolean deleteFile = true; // delete downloaded file when fail
         try {
             HttpResponse<InputStream> localVarResponse = created.send(localVarRequestBuilder.build(),
                     HttpResponse.BodyHandlers.ofInputStream());
             try {
                 if (localVarResponse.statusCode() / 100 != 2) {
                     log.error("Fail to get export file, status code {}", localVarResponse.statusCode());
                     throw new ApiException(localVarResponse.statusCode(), "");
                 }
                 Files.copy(localVarResponse.body(), path, StandardCopyOption.REPLACE_EXISTING);
                 deleteFile = false;
                 return new ApiResponse<Path>(localVarResponse.statusCode(), localVarResponse.headers().map(),
                         path
                 );
             } finally {
                 try {
                     localVarResponse.body().close();
                 } catch (IOException ioEx) {
                     log.warn("Fail to close response body", ioEx);
                 }
             }
         } catch (IOException e) {
             throw new ApiException(e);
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             throw new ApiException(e);
         } finally {
             if (deleteFile) {
                 try {
                     Files.deleteIfExists(path);
                 } catch (IOException e) {
                     // ignore
                 }
             }
         }
    }

}
