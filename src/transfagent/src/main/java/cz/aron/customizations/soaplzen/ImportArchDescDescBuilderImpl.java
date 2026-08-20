package cz.aron.customizations.soaplzen;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.ImportArchDescDescBuilder;
import cz.aron.transfagent.elza.LevelNameDescValueCollector;
import cz.aron.transfagent.elza.LevelNameDescValues;

@Service
@ConditionalOnProperty(value = "filestore5.path")
public class ImportArchDescDescBuilderImpl implements ImportArchDescDescBuilder {

	private final ConfigElzaArchDesc configArchDesc;

	public ImportArchDescDescBuilderImpl(ConfigElzaArchDesc configArchDesc) {
		this.configArchDesc = configArchDesc;
	}

	@Override
	public String getDesc(LevelContext levelContext, Set<String> levelDuplicities, ElzaXmlReader elzaXmlReader) {

		if (levelContext.getLevel().getPid() == null) {
			// for root no description
			return null;
		}

		LevelNameDescValues valueCollector;
		if (elzaXmlReader.getEdx().getInfo()!=null) {
			var vc = new LevelNameDescValueCollector(elzaXmlReader, true);
			vc.collect(levelContext);
			valueCollector = vc;
		} else {
			var vc = new NameDescValueCollector(elzaXmlReader);
			vc.collect(levelContext);
			valueCollector = vc;
		}

		StringBuilder sb = new StringBuilder();
		if (valueCollector.isMatrika()) {
			var title = valueCollector.getUkladaciJednotka();
			if (title==null) {
				title = valueCollector.getName();
			}			
			sb.append(title);
			if (StringUtils.isNotBlank(valueCollector.getDatace())) {
				sb.append(", ").append(valueCollector.getDatace());
				var typZaznamu = valueCollector.getTypZaznamu();
				if (StringUtils.isNotBlank(typZaznamu)) {
					sb.append(" ").append(typZaznamu);
				}
			}
		} else {
			var title = valueCollector.getName();
			if (StringUtils.isNotBlank(title)
					&& (!valueCollector.isTitleInherited() || configArchDesc.isInheritNameDate())) {
				sb.append(title);
			}
			var datace = valueCollector.getDatace();
			if (configArchDesc.isAddDateToName() && StringUtils.isNotBlank(datace)
					&& (!valueCollector.isDataceInherited() || configArchDesc.isInheritNameDate())) {
				if (sb.length()>0) {
					sb.append(", ");
				}
				sb.append(datace);
			}
			if (levelDuplicities != null) {
				var rawDesc = sb.toString();
				if (levelDuplicities.contains(rawDesc) && StringUtils.isNotBlank(valueCollector.getDatace())
						&& valueCollector.isDataceInherited() == false && !configArchDesc.isAddDateToName()) {
					if (sb.length()>0) {
						sb.append(", ");
					}
					sb.append(valueCollector.getDatace());
				}
			}
		}

		if (sb.length() == 0) {
			return null;
		}
		return sb.toString();
	}

}
