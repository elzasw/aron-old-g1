package cz.aron.transfagent.peva.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.customizations.soaplzen.DaoFileStore5Service;

@ConditionalOnProperty(value = "filestore5.path")
@Service
public class FindingAidDaoImporterFileStore5 implements FindingAidDaoImporter {
	
	private final DaoFileStore5Service fileStore5;

	public FindingAidDaoImporterFileStore5(DaoFileStore5Service fileStore5) {
		this.fileStore5 = fileStore5;
	}

	@Override
	public String getFindingAidDaoHandle(String archiveCode, String findingAidCode, String findingAidPartNumber) {		
		return fileStore5.getFindingAidDaoHandle(archiveCode, findingAidCode, findingAidPartNumber);
	}

	@Override
	public String getName() {
		return fileStore5.getName();
	}
	
}
