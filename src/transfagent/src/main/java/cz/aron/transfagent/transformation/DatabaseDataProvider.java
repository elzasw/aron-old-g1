package cz.aron.transfagent.transformation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.elza.ImportInstitution;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.DaoRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.StorageService;

@Service
public class DatabaseDataProvider implements ContextDataProvider {

    private static Logger log = LoggerFactory.getLogger(DatabaseDataProvider.class);

    final private InstitutionRepository institutionRepository;

    final private ArchivalEntityRepository entityRepository;

    final private FundRepository fundRepository;

    final private DaoRepository daoRepository;

    final private StorageService storageService;

    public DatabaseDataProvider(final InstitutionRepository institutionRepository,
            final ArchivalEntityRepository entityRepository,
            final FundRepository fundRepository,
            final DaoRepository daoRepository,
            final StorageService storageService) {
        this.institutionRepository = institutionRepository;
        this.entityRepository = entityRepository;
        this.fundRepository = fundRepository;
        this.daoRepository = daoRepository;
        this.storageService= storageService;
    }

    @Override
    public InstitutionInfo getInstitutionApu(String instCode) {
        Institution institution = institutionRepository.findByCode(instCode);
        if (institution != null) {
            UUID uuid = institution.getUuid();
            ApuSource apuSrc = institution.getApuSource();
            Path fileXml = storageService.getDataPath().resolve(apuSrc.getDataDir()).resolve(apuSrc.getOrigDir() + ".xml");
            ImportInstitution ii = new ImportInstitution();
            try {
                ii.importInstitution(fileXml, instCode, uuid);
            } catch (IOException | JAXBException e) {
                log.error("Error processing file={} institution code={}", fileXml, instCode, e);
                throw new RuntimeException(e);
            }                        
            return new InstitutionInfo(uuid, ii.getFullName(), ii.getShortName());
        }
        return null;
    }

    @Override
    public List<ArchEntityInfo> getArchivalEntityWithParentsByElzaId(Integer elzaId) {
        return convertObjectsListTo(entityRepository.findByElzaIdWithParents(elzaId));
    }

    @Override
    public List<ArchEntityInfo> getArchivalEntityWithParentsByUuid(UUID apUuid) {
        return convertObjectsListTo(entityRepository.findByUUIDWithParents(apUuid));
    }

    private List<ArchEntityInfo> convertObjectsListTo(List<Object[]> objects) {
        List<ArchEntityInfo> result = new ArrayList<>();
        for(Object[] obj : objects) {
            if(obj[0] != null && obj[1] != null) {
                result.add(new ArchEntityInfo(UUID.fromString(obj[0].toString()), obj[1].toString()));
            }
        }
        return result;
    }

	@Override
	public UUID getFundApu(String institutionCode, String fundCode) {
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
        	return null;
        }
		var fund = fundRepository.findByCodeAndInstitution(fundCode, institution);
		if(fund==null) {
			return null;
		}
		Validate.notNull(fund.getUuid());
		return fund.getUuid();
	}
	
	@Override
	public UUID getFundApuByUUID(String institutionCode, UUID fundUuid) {
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
        	return null;
        }
		var fund = fundRepository.findByUuidAndInstitution(fundUuid, institution);
		if(fund==null) {
			return null;
		}
		Validate.notNull(fund.getUuid());
		return fund.getUuid();		
	}

    @Override
    public UUID getDao(String daoHandle) {
        var dao = daoRepository.findByHandle(daoHandle);
        if(dao.isPresent()) {
            return dao.get().getUuid();
        } else {
            return null;
        }
    }

	@Override
	public List<ArchEntitySourceInfo> getArchEntityApuSources(List<UUID> uuids, String entityClass) {
		return this.entityRepository.getArchEntityApuSources(uuids, entityClass);
	}

}
