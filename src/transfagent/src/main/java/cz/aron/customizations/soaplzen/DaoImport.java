package cz.aron.customizations.soaplzen;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.customizations.soaplzen.DaoFileStore5Service.InstitutionFindingAidKey;
import cz.aron.transfagent.common.BulkOperation;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.repository.DaoRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.InstitutionsService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

/**
 * Class for detecting changes in fund and finding aid daos. Schedule
 * reimport on Dao where some change is detected.
 * 
 *  Detected changes 
 *   - add
 *   - modification
 *   - delete
 */
@Service
@ConditionalOnProperty(name = "filestore5.autoImportCsv", havingValue = "true")
public class DaoImport implements ImportProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(DaoImport.class);

	private final ConfigDaoFileStore5 config;
	
	private final InstitutionsService institutionsService;
	
	private final DaoFileStore5Service daoFileStore5Service;
	
	private final DaoRepository daoRepository;
	
	private final FindingAidRepository findingAidRepository;
	
	private final FileImportService importService;
	
	private final TransactionTemplate tt;
	
	private OffsetDateTime nextRun = null;
	
	public DaoImport(ConfigDaoFileStore5 config, InstitutionsService institutionsService,
			DaoFileStore5Service daoFileStore5Service, TransactionTemplate tt, DaoRepository daoRepository,
			FindingAidRepository findingAidRepository, FileImportService importService) {
		this.config = config;
		this.institutionsService = institutionsService;
		this.daoFileStore5Service = daoFileStore5Service;
		this.daoRepository = daoRepository;
		this.findingAidRepository = findingAidRepository;
		this.importService = importService;
		this.tt = tt;
	}

	@PostConstruct
	void init() {
		importService.registerImportProcessor(this);
	}
	
	@Override
	public int getPriority() {
		// low priority
		return -1;
	}
	
	@Override
	public void importData(ImportContext ic) {
		
		OffsetDateTime nowTime = OffsetDateTime.now();
		if (nextRun != null && nextRun.isAfter(OffsetDateTime.now())) {
			// TODO nejake lepsi planovani
			return;
		}

		var toInsert = new HashMap<InstitutionFindingAidKey,Long>();
		
		log.info("CSV scan started");
		var start = Instant.now();
		var numModified = new MutableInt();
		try {
			var handleLastModified = daoFileStore5Service.getKeyLastModified();			
			for(var entry:handleLastModified.entrySet()) {				
				tt.executeWithoutResult(t->{					
					var daoOpt = daoRepository.findByHandleAndSource(entry.getKey().getHandle(), daoFileStore5Service.getName());
					if(daoOpt.isPresent()) {
						var dao = daoOpt.get();
						if (dao.getLastModified() == 0) {
							// just set last modified
							dao.setLastModified(entry.getValue());
						} else if (dao.getLastModified() != entry.getValue()) {
							// dao was modified, set to be reimported
							dao.setLastModified(entry.getValue());
							dao.setState(DaoState.ACCESSIBLE);
							dao.setTransferred(false);
							numModified.increment();
							log.info("Dao modified, handle={}, uuid={}", dao.getHandle(), dao.getUuid());
						}
					} else {
						// dao not exist, insert to database to be imported
						toInsert.put(entry.getKey(), entry.getValue());
					}					
				});				
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		var numNew = new MutableInt();
		if (!toInsert.isEmpty()) {
			// new daos to reimport
			var instCodes = config.getInstitutions().stream().map(i -> i.getInstitutionCode()).collect(Collectors.toSet());
			var institutions = institutionsService.getInstitutionsByCodes(instCodes);
			var codeToId = institutions.stream().collect(Collectors.toMap((v) -> v.getCode(), (v) -> v.getId()));			
			
			BulkOperation.run(toInsert.entrySet(), 100, (batch) -> {
				tt.executeWithoutResult(t -> {
					for(var kv:batch) {
						var ifak = kv.getKey();											
						var instId = codeToId.get(ifak.getInstitutionCode());
						FindingAid findingAid = null;
						if (instId != null ) {
							if (ifak.getFindingAidPartNumber()!=null) {
								findingAid = findingAidRepository.findByInstitutionIdAndEvidenceNumberAndPartNumber(instId, ifak.getFindingAidCode(), Integer.parseInt(ifak.getFindingAidPartNumber()));	
							} else {
								findingAid = findingAidRepository.findByInstitutionIdAndEvidenceNumberAndPartNumberIsNull(instId, ifak.getFindingAidCode());
							}
							if (findingAid != null ) {
								if (!findingAid.getApuSource().isDeleted()) {
									findingAid.getApuSource().setReimport(true);
									log.info("New dao detected, handle={}, finding aid scheduled to reimport" , ifak.getHandle());								
									numNew.increment();
								} else {
									log.warn("Found new dao related to deleted finding aid, handle={}", ifak.getHandle());
								}
							} else {
								log.warn("Found new dao not related to finding aid, handle={}", ifak.getHandle());
							}
						} else {
							log.warn("Found new dao not related to finding aid, handle={}", ifak.getHandle());
						}
					}					
				});
			});
		}
		
		log.info("CSV changes check finished. Num reimported: {}, new: {}, time: {}s", numModified.getValue(), numNew.getValue(),Duration.between(start, Instant.now()).toSeconds());
		nextRun = nowTime.plusSeconds(3600);		
	}

}
