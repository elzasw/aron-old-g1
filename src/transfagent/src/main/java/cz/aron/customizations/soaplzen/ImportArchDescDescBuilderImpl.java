package cz.aron.customizations.soaplzen;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigElzaArchDesc;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.elza.ImportArchDesc.LevelContext;
import cz.aron.transfagent.elza.ImportArchDescDescBuilder;

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

		var valueCollector = new NameDescValueCollector(elzaXmlReader);
		valueCollector.collect(levelContext);

		StringBuilder sb = new StringBuilder();
		if (valueCollector.isMatrika()) {
			sb.append(valueCollector.getUkladaciJednotka());
			if (StringUtils.isNotBlank(valueCollector.getDatace())) {
				sb.append(", ").append(valueCollector.getDatace());
			}
		} else {
			var title = valueCollector.getTitle();
			if (StringUtils.isNotBlank(title)
					&& (!valueCollector.isTitleInherited() || configArchDesc.isInheritNameDate())) {
				sb.append(title);
			}
			var datace = valueCollector.getDatace();
			if (configArchDesc.isAddDateToName() && StringUtils.isNotBlank(datace)
					&& (!valueCollector.isDataceInherited() || configArchDesc.isInheritNameDate())) {
				sb.append(", ").append(datace);
			}
			if (levelDuplicities != null) {
				var rawDesc = sb.toString();
				if (levelDuplicities.contains(rawDesc) && StringUtils.isNotBlank(valueCollector.getDatace())
						&& valueCollector.isDataceInherited() == false && !configArchDesc.isAddDateToName()) {
					sb.append(", ").append(valueCollector.getDatace());
				}
			}
		}

		if (sb.length() == 0) {
			return null;
		}
		return sb.toString();
	}

}
