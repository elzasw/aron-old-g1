package cz.aron.customizations.soaplzen;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.ImportArchDescNameBuilder;
import cz.tacr.elza.schema.v2.Section;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportArchDescNameBuilderImpl implements ImportArchDescNameBuilder {

	@Override
	public String getName(Section sect, LevelContext levelContext, String institutionName,
			String institutionShortName, ElzaXmlReader elzaXmlReader) {
				
		var valueCollector = new NameDescValueCollector(elzaXmlReader);
		valueCollector.collect(levelContext);
		
		if (valueCollector.isMatrika()) {
			var title = valueCollector.getUkladaciJednotka(); 
			if (title==null) {
				title = valueCollector.getTitle();
			}		
			return title;
		} else {
			StringBuilder sb = new StringBuilder();
			if (valueCollector.isRoot()) {
				// for root use name of fund
				sb.append(institutionShortName).append(": ");
				sb.append(sect.getFi().getN());
			} else {
				sb.append(valueCollector.getTitle());
			}
			return sb.toString();	
		}
		
	}

}
