package cz.aron.transfagent.peva;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.peva.codelist.AccessibilityProvider;
import cz.aron.transfagent.peva.codelist.DatingMethodProvider;
import cz.aron.transfagent.peva.codelist.DepartmentProvider;
import cz.aron.transfagent.peva.codelist.EvidenceUnitTypeProvider;
import cz.aron.transfagent.peva.codelist.FindingAidFormTypeProvider;
import cz.aron.transfagent.peva.codelist.FindingAidTypeProvider;
import cz.aron.transfagent.peva.codelist.GeoObjectTypeProvider;
import cz.aron.transfagent.peva.codelist.IntegrityProvider;
import cz.aron.transfagent.peva.codelist.LanguageProvider;
import cz.aron.transfagent.peva.codelist.OriginatorSubClassProvider;
import cz.aron.transfagent.peva.codelist.PhysicalStateProvider;
import cz.aron.transfagent.peva.codelist.ThematicEvidenceGroupProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2CodeListDownloader {
	
	private final PEvA2Connection peva2;	
	
	public Peva2CodeListDownloader(PEvA2Connection peva2) {
		this.peva2 = peva2;
	}
	
    public Peva2CodeLists downloadCodeLists() {
        return new Peva2CodeLists(AccessibilityProvider.create(peva2), PhysicalStateProvider.create(peva2),
                IntegrityProvider.create(peva2),
                FindingAidTypeProvider.create(peva2), LanguageProvider.create(peva2),
                EvidenceUnitTypeProvider.create(peva2), FindingAidFormTypeProvider.create(peva2),
                OriginatorSubClassProvider.create(peva2), DatingMethodProvider.create(peva2),
                ThematicEvidenceGroupProvider.create(peva2), GeoObjectTypeProvider.create(peva2),
                DepartmentProvider.create(peva2));
    }

}
