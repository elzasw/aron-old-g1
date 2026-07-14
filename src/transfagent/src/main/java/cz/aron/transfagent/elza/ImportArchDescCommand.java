package cz.aron.transfagent.elza;

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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.service.CommandProcessor;
import cz.aron.transfagent.service.CommandProcessorContext;
import cz.aron.transfagent.service.CommandProcessorException;
import cz.aron.transfagent.service.StorageService;
import cz.tacr.elza.ApiClient;
import cz.tacr.elza.ApiException;
import cz.tacr.elza.ApiResponse;
import cz.tacr.elza.client.IoApi;
import cz.tacr.elza.client.vo.ExportParams;
import cz.tacr.elza.client.vo.FundSections;
import cz.tacr.elza.client.vo.RequestProcessState;

/**
 * Command for import archdesc from elza
 * 
 * Command file format:
 * 
 * elzaarchdesc-${fundVersion} 
 * 
 */
@Component
public class ImportArchDescCommand implements CommandProcessor {

	private static final Logger log = LoggerFactory.getLogger(ImportArchDescCommand.class);

	private static final String PREFIX_DASH = "elzaarchdesc-";

	private final StorageService storageService;
	
	private final ApiClientEx apiClient;

	private final IoApi ioApi;
	
	private final String exportFilter;

	public ImportArchDescCommand(StorageService storageService, ConfigElza configElza,
			ConfigElzaArchDesc configElzaArchDesc) {
		var elzaUrl = configElza.getUrl().substring(0, configElza.getUrl().indexOf("/services/ExportService"));
		this.storageService = storageService;
		this.apiClient = new ApiClientEx(configElza.getUsername(), configElza.getPassword(), elzaUrl + "/login",
				elzaUrl + "/api/v1");
		this.ioApi = new IoApi(apiClient);
		this.exportFilter = configElzaArchDesc.getExportFilterId() != null
				? configElzaArchDesc.getExportFilterId().toString()
				: null;
	}

	@Override
	public void processCommand(Path commandFile, CommandProcessorContext context) {

		// check if files is writeable
		if (!Files.isWritable(commandFile)) {
			log.error("File have to be writeable {}", commandFile);
			return;
		}

		var fileName = commandFile.getFileName().toString();
		
		// get fund version from filename
		var id = fileName.substring(PREFIX_DASH.length());

		Integer existingRequestId = readRequestId(commandFile);
		loginIfNeed();
		if (existingRequestId == null) {
			existingRequestId = createRequest(id, commandFile);
		}

		while (!checkState(existingRequestId, commandFile)) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			if (!context.isRunning()) {
				return;
			}
		}
		var archDescXml = downloadResult(existingRequestId, commandFile);
		storeResult(archDescXml, commandFile);
		try {
			Files.delete(commandFile);
		} catch (IOException e) {
			log.error("Fail to delete file {}",e);
			throw new CommandProcessorException(e);
		}
	}

	/**
	 * Read request id from command file when exist
	 * @param commandFile command file
	 * @return id or null when not exist
	 */
	private Integer readRequestId(Path commandFile) {
		try {
			var lines = Files.readAllLines(commandFile);
			if (!lines.isEmpty()) {
				return Integer.parseInt(lines.get(0));
			}
		} catch (IOException e) {
			log.error("Fail to read command file {}", commandFile, e);
			throw new CommandProcessorException(e);
		}
		return null;
	}

	private void loginIfNeed() {
		try {
			apiClient.loginIfNeed();
		} catch (Exception e) {
			apiClient.clearCookie();
			throw new CommandProcessorException(e);
		}
	}

	/**
	 * Create request to export archdesc in Elza
	 * @param fundVersionId fund version id
	 * @param commandFile command file
	 * @return id of request
	 * 
	 * Store request id to command file
	 */
	private Integer createRequest(String fundVersionId, Path commandFile) {
		var exportParams = new ExportParams();
		var fundSection = new FundSections();
		fundSection.setFundVersionId(Integer.parseInt(fundVersionId));
		exportParams.getFundsSections().add(fundSection);
		if (exportFilter != null) {
			exportParams.setExportFilter(exportFilter);									
		}
		try {
			Integer requestId = ioApi.ioExportRequest(exportParams);
			log.info("Created request to Elza to export fund {}, request id {}", fundVersionId, requestId);
			try {
				// store request id to command file
				Files.write(commandFile, String.valueOf(requestId).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				log.error("Fail to write to file {}", commandFile);
				System.exit(-1);
			}
			return requestId;
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				log.error("Fund id {} not exists", fundVersionId);
				moveToError(commandFile, "fundVersionId: "+fundVersionId+" not exists");
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			apiClient.clearCookie();
			throw new CommandProcessorException(e);
		}
	}

	/**
	 * Check state of running export in Elza
	 * @param id id of export request
	 * @param commandFile command file
	 * @return true - finished, false - still running
	 * 
	 * Move to command file to error director when request not exist
	 */
	private boolean checkState(Integer id, Path commandFile) {
		try {
			var status = ioApi.ioGetExportStatus(id);
			if (RequestProcessState.FINISHED.equals(status.getState())) {
				log.info("Request to Elza id {} finished", id);
				return true;
			} else {
				return false;
			}
		} catch (ApiException e) {
			log.error("Fail to check export request state Elza id {}", id, e);
			apiClient.clearCookie();
			moveToError(commandFile, e);
			throw new CommandProcessorException(e);
		}
	}
	
	/**
	 * Download exported file
	 * @param id id of export request
	 * @param commandFile
	 * @return path to downloaded file
	 */
	private Path downloadResult(Integer id, Path commandFile) {
		try {
			var tmp = commandFile.getParent().resolve(commandFile.getFileName().toString()+"_"+id);
			apiClient.ioGetExportFileWithHttpInfo(id, tmp);								
			log.info("Data for request id {} downloaded from Elza", id);
			return tmp;
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				log.error("Request to Elza id {} not exist", id);				
				moveToError(commandFile, "Request to Elza id "+id+" not exist");
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			log.error("Fail to download data for request id {} from Elza", id, e);
			apiClient.clearCookie();
			throw new CommandProcessorException(e);
		}
	}
	
	/**
	 * Store downloaded file to import directory 
	 * @param srcXml source xml file
	 * @param commandFile command file
	 */
	private void storeResult(Path srcXml, Path commandFile) {
		var archDescPath = storageService.getInputPath().resolve("archdesc");
		try (InputStream is = Files.newInputStream(srcXml)) {
			ElzaXmlReader reader = ElzaXmlReader.read(is);
			var edx = reader.getEdx();
			var section = edx.getFs().getS().stream().filter(s -> "ZP2015".equals(s.getRule())).findFirst()
					.orElse(null);
			if (section == null) {
				log.error("xml {} doesn't contains section.", srcXml);
				moveToError(commandFile, "xml doesn't contains section");
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			var fundInfo = section.getFi();
			var institution = fundInfo.getIc();
			if (fundInfo.getNum()==null) {
				log.error("xml {} doesn't contains fund num.", srcXml);
				moveToError(commandFile, "xml doesn't contains fund num");
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			var fundCode = fundInfo.getNum().toString();
			var name = "archdesc-" + institution + "-" + fundCode;
			Files.createDirectory(archDescPath.resolve(name));
			Files.move(srcXml, archDescPath.resolve(name).resolve(name + ".xml"));
		} catch (Exception e) {			
			if (Files.exists(srcXml)) {
				try {
					Files.delete(srcXml);
				} catch (IOException e1) {
					log.error("Fail to delete file", e1);
				}
			}			
			log.error("Fail to store archdescs",e);
			throw new CommandProcessorException(e);
		}
	}

	/**
	 * Move command file to error directory, store exception as error message
	 * @param commadFile command file
	 * @param ApiException exception
	 */
	private void moveToError(Path commadFile, ApiException exc) {
		try {									
			storageService.moveToErrorDirWithErrMessage(commadFile, exc.getResponseBody());
			log.info("Command {} moved to error directory", commadFile);
		} catch (IOException e) {
			log.error("Fail to move to error directory {}", commadFile);
			throw new CommandProcessorException(e);
		}
	}
	
	/**
	 * Move command file to error directory, store error message
	 * @param commadFile
	 * @param message
	 */
	private void moveToError(Path commadFile, String message) {
		try {
			storageService.moveToErrorDirWithErrMessage(commadFile, message);
			log.info("Command {} moved to error directory", commadFile);
		} catch (IOException e) {
			log.error("Fail to move to error directory {}", commadFile);
			throw new CommandProcessorException(e);
		}
	}
	
	@Override
	public String getPrefix() {
		return PREFIX_DASH;
	}
	
	private static class ApiClientEx extends ApiClient {

		private final CookieManager cookieManager = new CookieManager();
		
		private final String username;
		
		private final String password;
		
		private final URI loginUri;
		
		private HttpClient created = null;
		
		public ApiClientEx(String username, String password, String loginUri, String baseUri) {
			super();
			this.username = username;
			this.password = password;
			this.loginUri = URI.create(loginUri);
			updateBaseUri(baseUri);
			var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).cookieHandler(cookieManager);			
			setHttpClientBuilder(builder);
		}		
		
		@Override
		public HttpClient getHttpClient() {
			if (created != null) {
				return created;
			}
			return created = super.getHttpClient();			
		}
		
		public void loginIfNeed() throws ApiException {			
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

	}
	
	public static String createCommandFileName(long fundVersionId) {
		return PREFIX_DASH + fundVersionId;
	}
	
	public static void main(String[] args) throws Exception {		
		var apiClient = new ApiClientEx(args[0],args[1],args[2],args[3]);
		var ioApi = new IoApi(apiClient);
		apiClient.loginIfNeed();
		
		var exportParams = new ExportParams();
		var fundSection = new FundSections();
		fundSection.setFundVersionId(1);
		exportParams.getFundsSections().add(fundSection);
		exportParams.setExportFilter("1");		
		var requestId = ioApi.ioExportRequest(exportParams);
		
		while(true) {									
			var resp2 = ioApi.ioGetExportStatusWithHttpInfo(requestId);
			if (resp2.getData().getState().equals(RequestProcessState.FINISHED)) {
				break;
			}
		}

		var targetFile = Paths.get("archdesc.xml");								
		var ret = apiClient.ioGetExportFileWithHttpInfo(requestId, targetFile); 			
		
		System.out.println("Downloaded "+ret);				
	}
	
}
