package cz.aron.transfagent.service.reports;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.service.ArchDescService;
import cz.aron.transfagent.service.StorageService;

@Service
public class ArchDescReportService {
	
	private static final Logger log = LoggerFactory.getLogger(FundReportService.class);

	private final EntityManager em;
	
	private final StorageService storageService;

	public ArchDescReportService(EntityManager em, StorageService storageService) {
		this.em = em;
		this.storageService = storageService;
	}

	@Transactional(readOnly = true)
	public Path getArchDescsXlsx(String query, Integer instId) throws IOException {
	    CriteriaBuilder cb = em.getCriteriaBuilder();
	    CriteriaQuery<ArchDescsDTO> cq = cb.createQuery(ArchDescsDTO.class);	    	   
	    
	    Root<ArchDesc> archDesc = cq.from(ArchDesc.class);
	    Join<ArchDesc, Fund> fund = archDesc.join(ArchDesc.JPA_FUND);
	    Join<Fund, Institution> institution = fund.join(Fund.JPA_INSTITUTION);
	    Join<ArchDesc, ApuSource> apuSource = archDesc.join(ArchDesc.JPA_APUSOURCE);	    
	    
	    cq.select(cb.construct(
	        ArchDescsDTO.class,
	        archDesc.get("uuid"),
	        fund.get("nad"),
	        fund.get("subNad"),
	        apuSource.get("numAttachments"),
	        apuSource.get("numDaos"),
	        archDesc.get("numLevels"),
	        fund.get("name"),
	        institution.get("name")
	    ));	    
	    ArchDescService.addArchDescPredicate(cq, cb, archDesc, query, instId);
		
		var xlsxPath = storageService.createXlsxReportName("archivalie");				
		boolean deleteFile = true;
		try (SXSSFWorkbook wb = new SXSSFWorkbook(100); OutputStream fos = Files.newOutputStream(xlsxPath)) {
			
        	wb.setCompressTempFiles(true);
            var sheet = wb.createSheet();
            // umozni menit sirku sloupcu
            sheet.lockFormatColumns(false);
            createHeaders(sheet, 0);
			
            MutableInt rowNum = new MutableInt(1);
			try (Stream<ArchDescsDTO> stream = em.createQuery(cq).getResultStream()) {
				stream.forEach(fDTO->{
                    var row = sheet.createRow(rowNum.getAndIncrement());
                    if (fDTO.getNad() != null) {
                    	if (fDTO.getSubNad()!=null) {
                    		row.createCell(0).setCellValue(fDTO.getNad().toString() + "/" + fDTO.getSubNad().toString());		
                    	} else {
                    		row.createCell(0).setCellValue(fDTO.getNad().toString());
                    	}
                    }                    
                    row.createCell(1).setCellValue(fDTO.getNumAttachments());
                    row.createCell(2).setCellValue(fDTO.getNumDaos());
                    row.createCell(3).setCellValue(fDTO.getNumLevels());
                    row.createCell(4).setCellValue(fDTO.getName());
                    row.createCell(5).setCellValue(fDTO.getInstitution());
                    row.createCell(6).setCellValue(fDTO.getUuid().toString());                    
				});
			}						
			wb.write(fos);
			deleteFile = false;	
		} finally {
			if (deleteFile) {
				try {
					Files.delete(xlsxPath);
				} catch (IOException e) {
					log.warn("Fail to delete file {}", xlsxPath);
				}
			}
		}		
		return xlsxPath;		

	}
	
	private void createHeaders(SXSSFSheet sh, int row) {

		// sirka sloupce se udava v nasobcich 1/256 sirky znaku
		int charSize = 256;

		sh.setColumnWidth(0, 10 * charSize);
		sh.setColumnWidth(1, 10 * charSize);
		sh.setColumnWidth(2, 10 * charSize);
		sh.setColumnWidth(3, 10 * charSize);
		sh.setColumnWidth(4, 30 * charSize);
		sh.setColumnWidth(5, 20 * charSize);
		sh.setColumnWidth(6, 20 * charSize);

		// hlavicka
		Row header = sh.createRow(row);
		header.createCell(0).setCellValue("NAD");
		header.createCell(1).setCellValue("Přílohy");
		header.createCell(2).setCellValue("Digitalizáty");
		header.createCell(3).setCellValue("APU");
		header.createCell(4).setCellValue("Název fondu");
		header.createCell(5).setCellValue("Instituce");
		header.createCell(6).setCellValue("UUID");
	}
	
	public static class ArchDescsDTO {
		
		private final UUID uuid;
		
		private final Integer nad;
		
		private final Integer subNad;
		
		private final Integer numAttachments;
		
		private final Integer numDaos;
		
		private final Integer numLevels;
		
		private final String name;
		
		private final String institution;
		
		public ArchDescsDTO(UUID uuid, Integer nad, Integer subNad, Integer numAttachments, Integer numDaos, Integer numLevels,
				String name, String institution) {
			this.uuid = uuid;
			this.nad = nad;
			this.subNad = subNad;
			this.numAttachments = numAttachments;
			this.numDaos = numDaos;
			this.numLevels = numLevels;
			this.name = name;
			this.institution = institution;
		}

		public UUID getUuid() {
			return uuid;
		}

		public Integer getNad() {
			return nad;
		}

		public Integer getSubNad() {
			return subNad;
		}

		public Integer getNumAttachments() {
			return numAttachments;
		}

		public Integer getNumDaos() {
			return numDaos;
		}

		public Integer getNumLevels() {
			return numLevels;
		}

		public String getName() {
			return name;
		}

		public String getInstitution() {
			return institution;
		}		
		
	}

}
