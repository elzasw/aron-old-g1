package cz.aron.customizations.soaplzen;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.ImportArchDescNameBuilder;
import cz.aron.transfagent.elza.LevelNameDescValueCollector;
import cz.aron.transfagent.elza.LevelNameDescValues;
import cz.tacr.elza.schema.v2.Section;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportArchDescNameBuilderImpl implements ImportArchDescNameBuilder {

	@Override
	public String getName(Section sect, LevelContext levelContext, String institutionName,
			String institutionShortName, ElzaXmlReader elzaXmlReader) {
				
		
		LevelNameDescValues valueCollector;
		if (elzaXmlReader.getEdx().getInfo()!=null) {
			var vc = new LevelNameDescValueCollector(elzaXmlReader, false);
			vc.collect(levelContext);
			valueCollector = vc;
		} else {
			var vc = new NameDescValueCollector(elzaXmlReader);
			vc.collect(levelContext);
			valueCollector = vc;
		}
		
		if (valueCollector.isMatrika()) {
			var title = valueCollector.getUkladaciJednotka(); 
			if (title==null) {
				title = valueCollector.getName();
			}		
			return title;
		} else {
			StringBuilder sb = new StringBuilder();
			if (valueCollector.isRoot()) {
				// for root use name of fund
				sb.append(institutionShortName).append(": ");
				sb.append(sect.getFi().getN());
			} else {
				sb.append(valueCollector.getName());
			}
			return sb.toString();	
		}
		
	}

}
