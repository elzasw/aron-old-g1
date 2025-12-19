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
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.StorageService;

@Service
public class FindingAidReportService {
	
	private static final Logger log = LoggerFactory.getLogger(FindingAidReportService.class);

	private final EntityManager em;
	
	private final StorageService storageService;

	public FindingAidReportService(EntityManager em, StorageService storageService) {
		this.em = em;
		this.storageService = storageService;
	}

	@Transactional(readOnly = true)
	public Path getFindingAidsXlsx(String code, Integer instId) throws IOException {
		
	    CriteriaBuilder cb = em.getCriteriaBuilder();
	    CriteriaQuery<FindingAidDTO> cq = cb.createQuery(FindingAidDTO.class);	    	   
	    
	    Root<FindingAid> findingAid = cq.from(FindingAid.class);
	    Join<FindingAid, Institution> institution = findingAid.join(FindingAid.JPA_INSTITUTION);
	    Join<FindingAid, ApuSource> apuSource = findingAid.join(Fund.JPA_APUSOURCE);
	    Join<FindingAid, Fund> funds = findingAid.join(FindingAid.JPA_FUNDS);
	    
	    cq.select(cb.construct(
	        FindingAidDTO.class,
	        findingAid.get("uuid"),
	        funds.get("nad"),
	        funds.get("subNad"),
	        findingAid.get("evidenceNumber"),
	        findingAid.get("partNumber"),
	        apuSource.get("numAttachments"),
	        apuSource.get("numDaos"),
	        findingAid.get("name"),
	        institution.get("name")
	    ));	    
	    FindingAidService.addFindingAidPredicate(cq, cb, findingAid, code, instId, null);
	    
	    var xlsxPath = storageService.createXlsxReportName("pomucky");				
		boolean deleteFile = true;
		try (SXSSFWorkbook wb = new SXSSFWorkbook(100); OutputStream fos = Files.newOutputStream(xlsxPath)) {
			
        	wb.setCompressTempFiles(true);
            var sheet = wb.createSheet();
            // umozni menit sirku sloupcu
            sheet.lockFormatColumns(false);
            createHeaders(sheet, 0);
			
            MutableInt rowNum = new MutableInt(1);
			try (Stream<FindingAidDTO> stream = em.createQuery(cq).getResultStream()) {
				stream.forEach(faDTO->{
                    var row = sheet.createRow(rowNum.getAndIncrement());
                    if (faDTO.getNad() != null) {
                    	if (faDTO.getSubNad()!=null) {
                    		row.createCell(0).setCellValue(faDTO.getNad().toString() + "/" + faDTO.getSubNad().toString());		
                    	} else {
                    		row.createCell(0).setCellValue(faDTO.getNad().toString());
                    	}
                    }
                    if (faDTO.getEvidenceNumber()!=null) {
            	        if (faDTO.getPartNumber()!=null) {
            	        	row.createCell(1).setCellValue(""+faDTO.getEvidenceNumber()+"/"+faDTO.getPartNumber());
            	        } else {
            	        	row.createCell(1).setCellValue(""+faDTO.getEvidenceNumber());
            	        }
                    }
                    row.createCell(2).setCellValue(faDTO.getNumAttachments());
                    row.createCell(3).setCellValue(faDTO.getNumDaos());
                    row.createCell(4).setCellValue(faDTO.getName());
                    row.createCell(5).setCellValue(faDTO.getInstitution());
                    row.createCell(6).setCellValue(faDTO.getUuid().toString());                    
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
		sh.setColumnWidth(3, 30 * charSize);
		sh.setColumnWidth(4, 20 * charSize);
		sh.setColumnWidth(5, 20 * charSize);

		// hlavicka
		Row header = sh.createRow(row);
		header.createCell(0).setCellValue("NAD");
		header.createCell(1).setCellValue("Pomůcka");
		header.createCell(2).setCellValue("Přílohy");
		header.createCell(3).setCellValue("Digitalizáty");
		header.createCell(4).setCellValue("Název pomůcky");
		header.createCell(5).setCellValue("Instituce");
		header.createCell(6).setCellValue("UUID");
	}
	
	
	public static class FindingAidDTO {
		
		private final UUID uuid;
		
		private final Integer nad;
		
		private final Integer subNad;
		
		private final Integer evidenceNumber;
		
		private final Integer partNumber;
		
		private final Integer numAttachments;
		
		private final Integer numDaos;
		
		private final String name;
		
		private final String institution;

		public FindingAidDTO(UUID uuid, Integer nad, Integer subNad, Integer evidenceNumber, Integer partNumber,
				Integer numAttachments, Integer numDaos, String name, String institution) {
			this.uuid = uuid;
			this.nad = nad;
			this.subNad = subNad;
			this.evidenceNumber = evidenceNumber;
			this.partNumber = partNumber;
			this.numAttachments = numAttachments;
			this.numDaos = numDaos;
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

		public Integer getEvidenceNumber() {
			return evidenceNumber;
		}

		public Integer getPartNumber() {
			return partNumber;
		}

		public Integer getNumAttachments() {
			return numAttachments;
		}

		public Integer getNumDaos() {
			return numDaos;
		}

		public String getName() {
			return name;
		}

		public String getInstitution() {
			return institution;
		}
		
	}
	
}
