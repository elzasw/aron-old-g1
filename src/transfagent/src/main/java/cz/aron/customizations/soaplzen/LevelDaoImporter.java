package cz.aron.customizations.soaplzen;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.aron.transfagent.elza.dao.ArchDescLevelDaoImporter;
import cz.aron.transfagent.elza.dao.DaoRefRegistration;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.Level;

@ConditionalOnProperty(value = "filestore5.path")
@Service
public class LevelDaoImporter implements ArchDescLevelDaoImporter {

	private final ThumbnailFileStore thumbnailFileStore;

	public LevelDaoImporter(ThumbnailFileStore thumbnailFileStore) {
		this.thumbnailFileStore = thumbnailFileStore;
	}

	@Override
	public int importDaos(String institutionCode, int fundCode, Level lvl, Apu apu, ContextDataProvider dataProvider,
			DaoRefRegistration daoRefReg) {
		for (DescriptionItem di : lvl.getDdOrDoOrDp()) {
			if (ElzaTypes.ZP2015_DAO_ID.equals(di.getT()) && (di instanceof DescriptionItemString)) {
				String daoId = ((DescriptionItemString) di).getV();
				if (StringUtils.isBlank(daoId)) {
					continue;
				}
				var linkThumbnail = thumbnailFileStore.getDaoIdLink(daoId);
				if (linkThumbnail != null && linkThumbnail.getLink() != null) {
					var part = ApuSourceBuilder.getFirstPart(apu, CoreTypes.PT_ARCH_DESC);
					if (part != null) {
						ApuSourceBuilder.addLink(part, CoreTypes.SOURCE_LINK, linkThumbnail.getLink(), "Porta fontium");
					}
				}
			}
		}
		return 0;
	}

}
