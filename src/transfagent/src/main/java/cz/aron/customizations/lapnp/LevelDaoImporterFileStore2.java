package cz.aron.customizations.lapnp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.customizations.lapnp.DaoFileStore2Service.ArchiveFundDao;
import cz.aron.transfagent.elza.dao.ArchDescLevelDaoImporter;
import cz.aron.transfagent.elza.dao.DaoRefRegistration;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.Level;

@ConditionalOnProperty(value = "filestore2.path")
@Service
public class LevelDaoImporterFileStore2  implements ArchDescLevelDaoImporter {
	
	private final DaoFileStore2Service fileStore;

    public LevelDaoImporterFileStore2(DaoFileStore2Service fileStore) {
        this.fileStore = fileStore;
	}

	@Override
	public int importDaos(String institutionCode, int fundCode, Level lvl, Apu apu, ContextDataProvider dataProvider,
			DaoRefRegistration daoRefReg) {
		int numDaos = 0;
		for (DescriptionItem di : lvl.getDdOrDoOrDp()) {
			if (di instanceof DescriptionItemString) {
				DescriptionItemString ds = (DescriptionItemString) di;
				if ("ZP2015_DAO_ID".equals(ds.getT())) {
					String id = ds.getV();
					try {
						ArchiveFundDao afd = new ArchiveFundDao(institutionCode, fundCode, id);
						List<Path> paths = fileStore.getDaos(afd);
						if (CollectionUtils.isNotEmpty(paths)) {
							// podivam se jestli uz dao neexistuje abych negeneroval nove uuid a neposlal ho
							// opakovane
							// TODO doresit situaci kdy by neexistujici dao bylo odkazovano z vice urovni
							// (nakesovat nove vytvarena uuid)
							UUID uuid = dataProvider.getDao(afd.toString());
							if (uuid == null) {
								uuid = UUID.randomUUID();
							}
							daoRefReg.registerDao(fileStore.getName(), afd.toString(), uuid);
							ApuSourceBuilder.addDao(apu, uuid);
							numDaos++;
						}
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
		}
		return numDaos;
	}

}
