package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.service.CommandProcessor;
import cz.aron.transfagent.service.CommandProcessorContext;
import cz.aron.transfagent.service.CommandProcessorException;
import cz.aron.transfagent.service.StorageService;
import cz.tacr.elza.ApiClient;
import cz.tacr.elza.ApiException;
import cz.tacr.elza.client.IoApi;
import cz.tacr.elza.client.vo.ExportParams;
import cz.tacr.elza.client.vo.ExportRequestState;
import cz.tacr.elza.client.vo.FundSections;

@Component
public class ImportArchDescCommand implements CommandProcessor {

	private static final Logger log = LoggerFactory.getLogger(ImportArchDescCommand.class);

	private static final String PREFIX_DASH = "elzaarchdesc-";

	private final StorageService storageService;

	private final IoApi ioApi;

	private final ConfigElza configElza;

	private final String COOKIE_NAME = "JSESSIONID";

	private final String elzaUrl;

	private String cookie;

	public ImportArchDescCommand(StorageService storageService, ConfigElza configElza) {
		this.storageService = storageService;
		this.ioApi = new IoApi(new ApiClient());
		this.configElza = configElza;
		// TODO predelat url elzy
		this.elzaUrl = configElza.getUrl().substring(0, configElza.getUrl().indexOf("/services/ExportService"));
		ioApi.setCustomBaseUrl(elzaUrl + "/api/v1");
	}

	@Override
	public void processCommand(Path path, CommandProcessorContext context) {

		// check if files is writeable
		if (!Files.isWritable(path)) {
			log.error("File have to be writeable {}", path);
			return;
		}

		var fileName = path.getFileName().toString();
		var id = fileName.substring(PREFIX_DASH.length());

		Integer existingRequestId = readRequestId(path);
		loginIfNeed();
		if (existingRequestId == null) {
			existingRequestId = createRequest(id, path);
		}

		while (!checkState(existingRequestId, path)) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			if (!context.isRunning()) {
				return;
			}
		}
		var archDescXml = downloadResult(existingRequestId, path);
		storeResult(archDescXml, path);
		try {
			Files.delete(path);
		} catch (IOException e) {
			log.error("Fail to delete file {}",e);
			throw new CommandProcessorException(e);
		}
	}

	private Integer readRequestId(Path path) {
		try {
			var lines = Files.readAllLines(path);
			if (!lines.isEmpty()) {
				return Integer.parseInt(lines.get(0));
			}
		} catch (IOException e) {
			log.error("Fail to read command file {}", path, e);
			throw new CommandProcessorException(e);
		}
		return null;
	}

	private void loginIfNeed() {
		if (cookie == null) {
			try {
				login();
			} catch (ApiException e) {
				cookie = null;
				throw new CommandProcessorException(e);
			}
		}
	}

	private Integer createRequest(String fundId, Path path) {
		var exportParams = new ExportParams();
		var fundSection = new FundSections();
		fundSection.setFundVersionId(Integer.parseInt(fundId));
		exportParams.getFundsSections().add(fundSection);
		try {
			Integer requestId = ioApi.ioExportRequest(exportParams);
			log.info("Created request to Elza to export fund {}, request id {}", fundId, requestId);
			try {
				// store request id to command file
				Files.write(path, String.valueOf(requestId).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				log.error("Fail to write to file {}", path);
				System.exit(-1);
			}
			return requestId;
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				log.error("Fund id {} not exists", fundId);
				moveToError(path);
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			cookie = null;
			throw new CommandProcessorException(e);
		}
	}

	private boolean checkState(Integer id, Path path) {
		try {
			var status = ioApi.ioGetExportStatus(id);
			if (status.getState().equals(ExportRequestState.FINISHED)) {
				log.info("Request to Elza id {} finished", id);
				return true;
			} else {
				return false;
			}
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				log.error("Request to Elza id {} not exist", id);
				moveToError(path);
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			cookie = null;
			throw new CommandProcessorException(e);
		}
	}

	private Path downloadResult(Integer id, Path path) {
		try {
			var ret = ioApi.ioGetExportFile(id).toPath(); 			
			log.info("Data for request id {} downloaded from Elza", id);
			return ret;
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				log.error("Request to Elza id {} not exist", id);
				moveToError(path);
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			cookie = null;
			throw new CommandProcessorException(e);
		}
	}
	
	private void storeResult(Path srcXml, Path path) {
		var archDescPath = storageService.getInputPath().resolve("archdesc");
		try (InputStream is = Files.newInputStream(srcXml)) {
			ElzaXmlReader reader = ElzaXmlReader.read(is);
			var edx = reader.getEdx();
			var section = edx.getFs().getS().stream().filter(s -> "ZP2015".equals(s.getRule())).findFirst()
					.orElse(null);
			if (section == null) {
				log.error("xml {} doesn't contains section.", srcXml);
				moveToError(path);
				throw new CommandProcessorException(CommandProcessorException.Result.CONTINUE_PROCESSING);
			}
			var fundInfo = section.getFi();
			var institution = fundInfo.getIc();
			if (fundInfo.getNum()==null) {
				log.error("xml {} doesn't contains fund num.", srcXml);
				moveToError(path);
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

	private void moveToError(Path path) {
		try {
			storageService.moveToErrorDir(path);
			log.info("Command {} moved to error directory", path);
		} catch (IOException e) {
			log.error("Fail to move to error directory {}", path);
			throw new CommandProcessorException(e);
		}
	}

	@Override
	public String getPrefix() {
		return PREFIX_DASH;
	}

	private void login() throws ApiException {
		var cookies = new HashMap<String, String>();
		var formParams = new HashMap<String, Object>();
		formParams.put("username", configElza.getUsername());
		formParams.put("password", configElza.getPassword());
		var headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		List<MediaType> localVarAccept = new ArrayList<MediaType>();
		localVarAccept.add(MediaType.TEXT_PLAIN);
		var call = ioApi.getApiClient().buildCall(elzaUrl, "/login", "POST", Collections.emptyList(),
				Collections.emptyList(), null, headers, cookies, formParams, new String[] {}, null);
		var ret = ioApi.getApiClient().execute(call);
		var retCookies = ret.getHeaders().get("Set-Cookie");
		if (retCookies != null) {
			String cookieStr = retCookies.stream().filter(c -> c.startsWith(COOKIE_NAME)).findFirst().orElse(null);
			if (cookieStr != null) {
				var parsedCookies = HttpCookie.parse(cookieStr);
				var cookie = parsedCookies.get(0);
				this.cookie = cookie.getValue();
				ioApi.getApiClient().addDefaultCookie(COOKIE_NAME, this.cookie);
				log.info("Logged in to elza");
				return;
			}
		}
		this.cookie = null;
		throw new RuntimeException();
	}

	public static void main(String[] args) throws Exception {
		var ioApi = new IoApi(new ApiClient());
		var cookies = new HashMap<String, String>();
		var formParams = new HashMap<String, Object>();
		formParams.put("username", "lcadmin");
		formParams.put("password", "visnova15");
		var body = new HashMap<String, String>();
		body.put("username", "user");
		body.put("password", "password");
		var headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		List<MediaType> localVarAccept = new ArrayList<MediaType>();
		localVarAccept.add(MediaType.TEXT_PLAIN);
		var call = ioApi.getApiClient().buildCall("http://10.2.0.27:8084", "/login", "POST", Collections.emptyList(),
				Collections.emptyList(), null /* body */, headers, cookies, formParams, new String[] {}, null);
		var ret = ioApi.getApiClient().execute(call);
		var retCookies = ret.getHeaders().get("Set-Cookie");
		if (retCookies != null) {
			String cookieStr = retCookies.stream().filter(c -> c.startsWith("JSESSIONID")).findFirst().orElse(null);
			if (cookieStr != null) {
				var parsedCookies = HttpCookie.parse(cookieStr);
				var cookie = parsedCookies.get(0);
				System.out.println(cookie);
			}

		}
		System.out.println(retCookies);
	}

}
