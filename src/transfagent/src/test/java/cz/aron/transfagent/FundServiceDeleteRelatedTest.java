package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.service.FundService;

/**
 * Smazani archivniho popisu a archivnich pomucek fondu odstraneneho zmenou
 */
@SpringBootTest
public class FundServiceDeleteRelatedTest extends AbstractCommonTest {

	@Autowired
	private FundService fundService;

	@Test
	public void testDeleteFundRelated() {
		int[] ids = tt.execute(t -> {
			var institution = new Institution();
			institution.setApuSource(createApuSource(SourceType.INSTITUTION));
			institution.setCode(INSTITUTION_CODE);
			institution.setSource("test");
			institution.setUuid(UUID.randomUUID());
			institution = institutionRepository.save(institution);

			var fundA = createFund(institution, "A");
			var fundB = createFund(institution, "B");

			var archDesc = new ArchDesc();
			archDesc.setApuSource(createApuSource(SourceType.ARCH_DESCS));
			archDesc.setUuid(UUID.randomUUID());
			archDesc.setFund(fundA);
			archDesc = archDescRepository.save(archDesc);

			// pomucka jen k fondu A
			var faOnlyA = createFindingAid(institution, "FA1");
			faOnlyA.addFund(fundA);
			// pomucka k fondum A i B
			var faShared = createFindingAid(institution, "FA2");
			faShared.addFund(fundA);
			faShared.addFund(fundB);

			return new int[] { fundA.getId(), fundB.getId(), archDesc.getApuSource().getId(),
					faOnlyA.getApuSource().getId(), faShared.getApuSource().getId(),
					fundA.getApuSource().getId() };
		});
		int fundAId = ids[0], fundBId = ids[1], archDescApuId = ids[2], faOnlyAApuId = ids[3],
				faSharedApuId = ids[4], fundAApuId = ids[5];

		tt.executeWithoutResult(t -> {
			fundService.deleteFundRelated(fundRepository.findById(fundAId).get(), true);
		});

		assertTrue(isDeleted(archDescApuId));
		assertTrue(isDeleted(faOnlyAApuId));
		// pomucka je svazana s platnym fondem B
		assertFalse(isDeleted(faSharedApuId));
		// samotny fond meni volajici
		assertFalse(isDeleted(fundAApuId));
		assertEquals(List.of(archDescApuId, faOnlyAApuId), getQueuedApuSourceIds());

		// opakovane volani nic nemeni
		tt.executeWithoutResult(t -> {
			fundService.deleteFundRelated(fundRepository.findById(fundAId).get(), true);
		});
		assertEquals(List.of(archDescApuId, faOnlyAApuId), getQueuedApuSourceIds());

		// po smazani fondu B se smaze i sdilena pomucka
		tt.executeWithoutResult(t -> {
			fundRepository.findById(fundBId).get().getApuSource().setDeleted(true);
		});
		tt.executeWithoutResult(t -> {
			fundService.deleteFundRelated(fundRepository.findById(fundAId).get(), true);
		});
		assertTrue(isDeleted(faSharedApuId));
		assertEquals(List.of(archDescApuId, faOnlyAApuId, faSharedApuId), getQueuedApuSourceIds());
	}

	@Test
	public void testDeleteFundRelatedFindingAidsNotSent() {
		int[] ids = tt.execute(t -> {
			var institution = new Institution();
			institution.setApuSource(createApuSource(SourceType.INSTITUTION));
			institution.setCode(INSTITUTION_CODE);
			institution.setSource("test");
			institution.setUuid(UUID.randomUUID());
			institution = institutionRepository.save(institution);
			var fund = createFund(institution, "A");
			var fa = createFindingAid(institution, "FA1");
			fa.addFund(fund);
			return new int[] { fund.getId(), fa.getApuSource().getId() };
		});

		tt.executeWithoutResult(t -> {
			fundService.deleteFundRelated(fundRepository.findById(ids[0]).get(), false);
		});

		assertTrue(isDeleted(ids[1]));
		assertTrue(getQueuedApuSourceIds().isEmpty());
	}

	private boolean isDeleted(int apuSourceId) {
		return apuSourceRepository.findById(apuSourceId).get().isDeleted();
	}

	private List<Integer> getQueuedApuSourceIds() {
		return tt.execute(t -> coreQueueRepository.findAll(Sort.by("id")).stream()
				.map(cq -> cq.getApuSource().getId()).collect(Collectors.toList()));
	}

	private Fund createFund(Institution institution, String code) {
		var fund = new Fund();
		fund.setApuSource(createApuSource(SourceType.FUND));
		fund.setInstitution(institution);
		fund.setCode(code);
		fund.setSource("peva");
		fund.setUuid(UUID.randomUUID());
		return fundRepository.save(fund);
	}

	private FindingAid createFindingAid(Institution institution, String code) {
		var findingAid = new FindingAid();
		findingAid.setApuSource(createApuSource(SourceType.FINDING_AID));
		findingAid.setInstitution(institution);
		findingAid.setCode(code);
		findingAid.setUuid(UUID.randomUUID());
		return findingAidRepository.save(findingAid);
	}

	private ApuSource createApuSource(SourceType sourceType) {
		var apuSource = new ApuSource();
		apuSource.setDataDir("");
		apuSource.setDateImported(ZonedDateTime.now());
		apuSource.setDeleted(false);
		apuSource.setForce(false);
		apuSource.setDisableSend(false);
		apuSource.setNumAttachments(0);
		apuSource.setNumDaos(0);
		apuSource.setOrigDir("");
		apuSource.setReimport(false);
		apuSource.setSourceType(sourceType);
		apuSource.setUuid(UUID.randomUUID());
		return apuSourceRepository.save(apuSource);
	}

}
