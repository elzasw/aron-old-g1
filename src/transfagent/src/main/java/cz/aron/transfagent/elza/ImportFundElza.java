package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.FundService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFundService.FundImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFundElza implements FundImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ImportFundElza.class);

	private static final String PREFIX = "fund-";
	
	private static final String FUND_SOURCE = "elza";
	
	private final FundService fundService;
	
	private final FundRepository fundRepository;
	
	private final StorageService storageService;
	
	private final InstitutionRepository institutionRepository;
	
    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
    
    private final ConfigElza configElza;

	public ImportFundElza(FundService fundService, FundRepository fundRepository, StorageService storageService,
			InstitutionRepository institutionRepository, DatabaseDataProvider databaseDataProvider,
			ConfigurationLoader configurationLoader, ConfigElza configElza) {
		this.fundService = fundService;
		this.fundRepository = fundRepository;
		this.storageService = storageService;
		this.institutionRepository = institutionRepository;
		this.databaseDataProvider = databaseDataProvider;
		this.configurationLoader = configurationLoader;
		this.configElza = configElza;
	}

	@Override
	public ImportResult processPath(Path path) {
		
		if (!path.getFileName().toString().startsWith(PREFIX)) {
			return ImportResult.UNSUPPORTED;
		}
		
		List<Path> xmls;
		try (var stream = Files.list(path)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) &&  f.getFileName().toString().startsWith(PREFIX)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
		
		var elzaFundXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();
		
		if (elzaFundXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", path);
            return ImportResult.FAIL;
		}
		
		if (processFund(path,elzaFundXml.get())) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.FAIL;
		}
	}

	private boolean processFund(Path dir, Path fundXml) {
		var fileName = fundXml.getFileName().toString();
		var tmp = fileName.substring("fund-".length());
		var fundCode = tmp.substring(0, tmp.length() - ".xml".length());

		var fund = fundRepository.findByCode(fundCode);
		UUID fundUuid = (fund != null) ? fund.getUuid() : null;

		var ifi = new ImportFundInfo();
		ApuSourceBuilder apusrcBuilder;

		try {
			apusrcBuilder = ifi.importFundInfo(fundXml, fundUuid, databaseDataProvider, configElza.isArchdescFlag());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		}

		if (fund != null) {
			apusrcBuilder.setUuid(fund.getApuSource().getUuid());
		}

		var institutionCode = ifi.getInstitutionCode();
		var institution = institutionRepository.findByCode(institutionCode);
		if (institution == null) {
			throw new IllegalStateException("The entry Institution code={" + institutionCode + "} must exist.");
		}

		try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
			apusrcBuilder.build(fos, new ApuValidator(configurationLoader.getConfig()));
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		}

		Path dataDir;
		try {
			dataDir = storageService.moveToDataDir(dir);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		if (fund == null) {
			fundService.createFund(institution, dataDir, dir, apusrcBuilder, fundCode, FUND_SOURCE);
		} else {
			fundService.updateFund(fund, dataDir, dir, true);
		}
		return true;
	}

	@Override
	public Result reimport(ApuSource apuSource, Fund fund, Path dataDir) {
		
		if (!dataDir.getFileName().toString().startsWith(PREFIX)) {
			return Result.UNSUPPORTED;
		}
		
        var fileName = PREFIX + fund.getCode() + ".xml";
        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        ApuSourceBuilder apuSourceBuilder;
        var ifi = new ImportFundInfo();
        try {
            apuSourceBuilder = ifi.importFundInfo(apuDir.resolve(fileName), fund.getUuid(), databaseDataProvider, configElza.isArchdescFlag());
            apuSourceBuilder.setUuid(apuSource.getUuid());
            try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
                apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", fileName, apuDir, e);
            return Result.FAILED;
        }
        return Result.REIMPORTED;
	}

}
