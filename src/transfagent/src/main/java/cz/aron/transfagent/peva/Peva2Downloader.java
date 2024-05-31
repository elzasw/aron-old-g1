package cz.aron.transfagent.peva;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.domain.Property;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportContext;

public abstract class Peva2Downloader {

	private final String updateAfterPropertyName;

	private final String searchAfterPropertyName;

	protected final PEvA2Connection peva2;

	protected final PropertyRepository propertyRepository;

	protected final ConfigPeva2 config;

	protected final TransactionTemplate tt;

	protected final StorageService storageService;
	
	private final boolean active;
	
	protected boolean storeState = true;

	public Peva2Downloader(String agendaName, PEvA2Connection peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService, boolean active) {
		this.peva2 = peva2;
		this.propertyRepository = propertyRepository;
		this.config = config;
		this.tt = tt;
		this.storageService = storageService;
		this.active = active;
		if (peva2.isMainConnection()) {
		    updateAfterPropertyName = "PEVA2_" + agendaName + "_UPDATE_AFTER";
		    searchAfterPropertyName = "PEVA2_" + agendaName + "_SEARCH_AFTER";
		} else {
		    updateAfterPropertyName = "PEVA2_" + agendaName + "_" + peva2.getInstitutionId() + "_UPDATE_AFTER";
            searchAfterPropertyName = "PEVA2_" + agendaName + "_" + peva2.getInstitutionId() + "_SEARCH_AFTER";
		}
	}

	public void importDataInternal(ImportContext ic, Peva2CodeListProvider codeListProvider) {

		var updateAfterProp = propertyRepository.findByName(updateAfterPropertyName);
		var searchAfterProp = propertyRepository.findByName(searchAfterPropertyName);

		final OffsetDateTime nowTime = OffsetDateTime.now();

		XMLGregorianCalendar od = null;
		if (updateAfterProp != null && StringUtils.isNotBlank(updateAfterProp.getValue())) {
			try {
				od = DatatypeFactory.newInstance().newXMLGregorianCalendar(updateAfterProp.getValue());
				// chyba v PEvA2, pokud je zadana casova zona, tak je updatedAfter ignorovano
				od.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
			} catch (DatatypeConfigurationException e) {
				throw new IllegalStateException();
			}
		}

		String searchAfter = searchAfterProp != null && StringUtils.isNotBlank(searchAfterProp.getValue())
				? searchAfterProp.getValue()
				: null;

		if (synchronizeAgenda(od, 0L, searchAfter, codeListProvider) > 0) {
			// TODO reportovat realnou hodnotu
			ic.addProcessed();
		}

		if (storeState) {
			tt.execute(t -> {
				var sa = propertyRepository.findByName(searchAfterPropertyName);
				if (sa != null) {
					sa.setValue("");
					propertyRepository.save(sa);
				}
				var ua = propertyRepository.findByName(updateAfterPropertyName);
				if (ua == null) {
					ua = new Property();
					ua.setName(updateAfterPropertyName);
				}
				// posunu zpet o offset, do PEvA neprenesu zonu a databaze ji asi interne
				// pouziva
				var offsetSeconds = ZonedDateTime.now().getOffset().getTotalSeconds();
				var newRun = nowTime.minusSeconds(offsetSeconds);
				ua.setValue(newRun.toString());
				propertyRepository.save(ua);
				return null;
			});
		}

	}

	protected void storeSearchAfter(String searchAfter) {
		String searchAfterFinal = searchAfter;
		tt.execute(t -> {
			var sa = propertyRepository.findByName(searchAfterPropertyName);
			if (sa != null) {
				sa.setValue(searchAfterFinal != null ? searchAfterFinal : "");
			} else {
				sa = new Property();
				sa.setName(searchAfterPropertyName);
				sa.setValue(searchAfterFinal);
			}
			propertyRepository.save(sa);
			return null;
		});
	}

	protected abstract int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial,
			Peva2CodeListProvider codeListProvider);

	/**
	 * Process command described by file
	 * @param path path co command file
	 * @param codeListProvider ciselniky
	 * @return true command processed, false not interested on this command file
	 * 
	 * Command file will be deleted when return true
	 * 
	 */
	protected boolean processCommand(Path path, Peva2CodeListProvider codeListProvider) {
		// by default command is not processed
		return false;
	}

	/**
	 * Update agendas when true. Process commands only otherwise. 
	 * @return true active, false not active
	 */
	protected boolean isActive() {
		return active;
	}
	
	/**
	 * Return downloader name. Overwrite when name is instance dependent.
	 * @return String
	 */
	protected String getName() {
	    return this.getClass().getName();
	}
	
	public static void main(String [] args) {
		
		
		System.out.println(""+ZonedDateTime.now().getOffset().getTotalSeconds());
		
		var odt = OffsetDateTime.now();
		var ldt = LocalDateTime.now();
		var zdt = ZonedDateTime.now();
		
		System.out.println(""+odt);
		System.out.println(""+ldt);
		System.out.println(""+zdt);
		
		parse(""+odt);
		parse(""+ldt);
		parse(""+zdt);
	}
	
	private static void parse(String str) {
		try {
			var tmp = DatatypeFactory.newInstance().newXMLGregorianCalendar(str);
			// chyba v PEvA2, pokud je zadana casova zona, tak je updatedAfter ignorovano
			tmp.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
			System.out.println(""+str+" -> "+tmp);
		} catch (DatatypeConfigurationException e) {
			System.out.println("Fail to parse "+str);
		}
	}
	
}
