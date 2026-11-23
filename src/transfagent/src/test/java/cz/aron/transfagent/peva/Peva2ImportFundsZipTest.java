package cz.aron.transfagent.peva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

import javax.xml.datatype.DatatypeFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.aron.peva2.wsdl.ExternalChange;
import cz.aron.peva2.wsdl.ExternalChangeTypeCode;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.InternalChange;
import cz.aron.peva2.wsdl.InternalChangeTypeCode;
import cz.aron.peva2.wsdl.ListExternalChangeResponse;
import cz.aron.peva2.wsdl.ListInternalChangeResponse;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.State;

public class Peva2ImportFundsZipTest {

	private static final String FUND_ID = "225201010-123";

	private static final String FUND_UUID = "0a0a0a0a-0000-0000-0000-000000000001";

	private static final String COMMITTED = "2026-01-15T10:00:00";

	/**
	 * Stavy listu NAD, ktere vyrazuji fond z evidence (import i reimport).
	 */
	@Test
	public void testRemovedByState() {
		assertTrue(ImportPevaFund.isRemovedByState(State.MOVED));
		assertTrue(ImportPevaFund.isRemovedByState(State.DESTROYED));
		assertTrue(ImportPevaFund.isRemovedByState(State.NOT_FOUND));
		assertFalse(ImportPevaFund.isRemovedByState(State.OK));
		assertFalse(ImportPevaFund.isRemovedByState(State.PARTIALLY_NOT_FOUND));
		assertFalse(ImportPevaFund.isRemovedByState(null));
	}

	@Test
	public void testStoreAndReadFundZip(@TempDir Path dir) throws Exception {
		// xml ve starem formatu, po ulozeni zipu ma byt odstraneno
		var oldXml = dir.resolve("pevafund-" + FUND_ID + ".xml");
		Peva2XmlReader.marshalGetNadSheetResponse(createFund(), oldXml);
		assertEquals(FUND_UUID,
				Peva2XmlReader.unmarshalGetNadSheetResponseFromFundFile(oldXml).getNadPrimarySheet().getId());

		var externalChange = new ExternalChange();
		externalChange.setId("0a0a0a0a-0000-0000-0000-000000000002");
		var externalChanges = new ListExternalChangeResponse();
		externalChanges.setExternalChanges(new ListExternalChangeResponse.ExternalChanges());
		externalChanges.getExternalChanges().getExternalChange().add(externalChange);
		externalChanges.setCount(1);

		var internalChange = new InternalChange();
		internalChange.setId("0a0a0a0a-0000-0000-0000-000000000003");
		var internalChanges = new ListInternalChangeResponse();
		internalChanges.setInternalChanges(new ListInternalChangeResponse.InternalChanges());
		internalChanges.getInternalChanges().getInternalChange().add(internalChange);
		internalChanges.setCount(1);

		Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), externalChanges, internalChanges);

		var zip = dir.resolve("pevafund-" + FUND_ID + ".zip");
		assertTrue(Files.isRegularFile(zip));
		assertFalse(Files.exists(oldXml));
		try (var files = Files.list(dir)) {
			assertEquals(1, files.count());
		}

		List<String> names = new ArrayList<>();
		try (var zipFile = new ZipFile(zip.toFile())) {
			zipFile.stream().forEach(e -> names.add(e.getName()));
			try (var is = zipFile.getInputStream(zipFile.getEntry(Peva2ImportFunds.EXTERNAL_CHANGES_XML))) {
				var read = Peva2XmlReader.unmarshalListExternalChangeResponse(is);
				assertEquals(1, read.getExternalChanges().getExternalChange().size());
				assertEquals(externalChange.getId(), read.getExternalChanges().getExternalChange().get(0).getId());
			}
			try (var is = zipFile.getInputStream(zipFile.getEntry(Peva2ImportFunds.INTERNAL_CHANGES_XML))) {
				var read = Peva2XmlReader.unmarshalListInternalChangeResponse(is);
				assertEquals(1, read.getInternalChanges().getInternalChange().size());
				assertEquals(internalChange.getId(), read.getInternalChanges().getInternalChange().get(0).getId());
			}
		}
		Collections.sort(names);
		assertEquals(List.of("external_changes.xml", "internal_changes.xml", "pevafund-" + FUND_ID + ".xml"), names);

		assertEquals(FUND_UUID,
				Peva2XmlReader.unmarshalGetNadSheetResponseFromFundFile(zip).getNadPrimarySheet().getId());
	}

	/**
	 * Zip zapisovany Peva2ImportFundIds, jmeno ${jafa}#${institutionId}#${uuid}
	 */
	@Test
	public void testStoreAndReadFundIdsZip(@TempDir Path dir) throws Exception {
		var baseName = FUND_ID + "#0a0a0a0a-0000-0000-0000-000000000009#" + FUND_UUID;

		// xml ve starem formatu, po ulozeni zipu ma byt odstraneno
		var oldXml = dir.resolve(baseName + ".xml");
		Peva2XmlReader.marshalGetNadSheetResponse(createFund(), oldXml);

		var externalChanges = new ListExternalChangeResponse();
		externalChanges.setExternalChanges(new ListExternalChangeResponse.ExternalChanges());
		externalChanges.getExternalChanges().getExternalChange()
				.add(createExternalChange("0a0a0a0a-0000-0000-0000-000000000002", ExternalChangeTypeCode.DE_51));

		Peva2ImportFunds.storeZip(dir, baseName, createFund(), externalChanges,
				createInternalChanges(createInternalChange(InternalChangeTypeCode.DE_11)));

		var zip = dir.resolve(baseName + ".zip");
		assertTrue(Files.isRegularFile(zip));
		assertFalse(Files.exists(oldXml));
		try (var files = Files.list(dir)) {
			assertEquals(1, files.count());
		}

		List<String> names = new ArrayList<>();
		try (var zipFile = new ZipFile(zip.toFile())) {
			zipFile.stream().forEach(e -> names.add(e.getName()));
		}
		Collections.sort(names);
		assertEquals(List.of(baseName + ".xml", "external_changes.xml", "internal_changes.xml"), names);

		assertEquals(FUND_UUID,
				Peva2XmlReader.unmarshalGetNadSheetResponseFromFundFile(zip).getNadPrimarySheet().getId());
		assertEquals(1, Peva2XmlReader.unmarshalListExternalChangeResponseFromFundFile(zip).getExternalChanges()
				.getExternalChange().size());
		assertEquals(1, Peva2XmlReader.unmarshalListInternalChangeResponseFromFundFile(zip).getInternalChanges()
				.getInternalChange().size());
	}

	@Test
	public void testRemovedByChange(@TempDir Path dir) throws Exception {
		var internalChanges = new ListInternalChangeResponse();
		internalChanges.setInternalChanges(new ListInternalChangeResponse.InternalChanges());

		// zmena jineho typu, fond neni odstranen
		var externalChanges = new ListExternalChangeResponse();
		externalChanges.setExternalChanges(new ListExternalChangeResponse.ExternalChanges());
		externalChanges.getExternalChanges().getExternalChange()
				.add(createExternalChange("0a0a0a0a-0000-0000-0000-000000000002", ExternalChangeTypeCode.DE_51));
		Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), externalChanges, internalChanges);
		var zip = dir.resolve("pevafund-" + FUND_ID + ".zip");
		assertFalse(isRemovedByChange(zip));

		// pridana neprepsana zmena DE_52, fond neni odstranen
		var de52 = createExternalChange("0a0a0a0a-0000-0000-0000-000000000004", ExternalChangeTypeCode.DE_52);
		externalChanges.getExternalChanges().getExternalChange().add(de52);
		Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), externalChanges, internalChanges);
		assertFalse(isRemovedByChange(zip));

		// zmena DE_52 prepsana, fond je odstranen
		de52.setCommitted(DatatypeFactory.newInstance().newXMLGregorianCalendar(COMMITTED));
		Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), externalChanges, internalChanges);
		assertTrue(isRemovedByChange(zip));

		// starsi data primo v xml zmeny neobsahuji
		var oldXml = dir.resolve("pevafund-" + FUND_ID + ".xml");
		Peva2XmlReader.marshalGetNadSheetResponse(createFund(), oldXml);
		assertNull(Peva2XmlReader.unmarshalListExternalChangeResponseFromFundFile(oldXml));
		assertNull(Peva2XmlReader.unmarshalListInternalChangeResponseFromFundFile(oldXml));
		assertFalse(ImportPevaFundInfo.isRemovedByChange(null, null));
	}

	@Test
	public void testRemovedByExternalChange(@TempDir Path dir) throws Exception {
		var internalChanges = new ListInternalChangeResponse();
		internalChanges.setInternalChanges(new ListInternalChangeResponse.InternalChanges());
		var zip = dir.resolve("pevafund-" + FUND_ID + ".zip");

		// zmeny casti archivniho souboru a ulozeni mimo archiv, fond neni odstranen
		// ani po prepsani
		for (var type : List.of(ExternalChangeTypeCode.DE_51, ExternalChangeTypeCode.DE_53,
				ExternalChangeTypeCode.DE_56)) {
			var change = createExternalChange("0a0a0a0a-0000-0000-0000-000000000004", type);
			change.setCommitted(DatatypeFactory.newInstance().newXMLGregorianCalendar(COMMITTED));
			Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), createExternalChanges(change), internalChanges);
			assertFalse(isRemovedByChange(zip), type.toString());
		}

		// ubytek celeho archivniho souboru, fond je odstranen az po prepsani
		for (var type : List.of(ExternalChangeTypeCode.DE_52, ExternalChangeTypeCode.DE_54,
				ExternalChangeTypeCode.DE_55)) {
			var change = createExternalChange("0a0a0a0a-0000-0000-0000-000000000004", type);
			Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), createExternalChanges(change), internalChanges);
			assertFalse(isRemovedByChange(zip), type.toString());

			change.setCommitted(DatatypeFactory.newInstance().newXMLGregorianCalendar(COMMITTED));
			Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), createExternalChanges(change), internalChanges);
			assertTrue(isRemovedByChange(zip), type.toString());
		}
	}

	private static ListExternalChangeResponse createExternalChanges(ExternalChange externalChange) {
		var externalChanges = new ListExternalChangeResponse();
		externalChanges.setExternalChanges(new ListExternalChangeResponse.ExternalChanges());
		externalChanges.getExternalChanges().getExternalChange().add(externalChange);
		return externalChanges;
	}

	@Test
	public void testRemovedByInternalChange(@TempDir Path dir) throws Exception {
		var externalChanges = new ListExternalChangeResponse();
		externalChanges.setExternalChanges(new ListExternalChangeResponse.ExternalChanges());
		var zip = dir.resolve("pevafund-" + FUND_ID + ".zip");

		// zmeny casti archivniho souboru, fond neni odstranen ani po prepsani
		for (var type : List.of(InternalChangeTypeCode.DE_11, InternalChangeTypeCode.DE_21,
				InternalChangeTypeCode.DE_22)) {
			var change = createInternalChange(type);
			change.setCommitted(DatatypeFactory.newInstance().newXMLGregorianCalendar(COMMITTED));
			Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), externalChanges, createInternalChanges(change));
			assertFalse(isRemovedByChange(zip), type.toString());
		}

		// zmeny celeho archivniho souboru, fond je odstranen az po prepsani
		for (var type : List.of(InternalChangeTypeCode.DE_12, InternalChangeTypeCode.DE_13,
				InternalChangeTypeCode.DE_23)) {
			var change = createInternalChange(type);
			Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), externalChanges, createInternalChanges(change));
			assertFalse(isRemovedByChange(zip), type.toString());

			change.setCommitted(DatatypeFactory.newInstance().newXMLGregorianCalendar(COMMITTED));
			Peva2ImportFunds.storeFundZip(dir, FUND_ID, createFund(), externalChanges, createInternalChanges(change));
			assertTrue(isRemovedByChange(zip), type.toString());
		}
	}

	private static boolean isRemovedByChange(Path fundFile) throws Exception {
		return ImportPevaFundInfo.isRemovedByChange(
				Peva2XmlReader.unmarshalListExternalChangeResponseFromFundFile(fundFile),
				Peva2XmlReader.unmarshalListInternalChangeResponseFromFundFile(fundFile));
	}

	private static InternalChange createInternalChange(InternalChangeTypeCode type) {
		var internalChange = new InternalChange();
		internalChange.setId("0a0a0a0a-0000-0000-0000-000000000005");
		internalChange.setType(type);
		return internalChange;
	}

	private static ListInternalChangeResponse createInternalChanges(InternalChange internalChange) {
		var internalChanges = new ListInternalChangeResponse();
		internalChanges.setInternalChanges(new ListInternalChangeResponse.InternalChanges());
		internalChanges.getInternalChanges().getInternalChange().add(internalChange);
		return internalChanges;
	}

	private static ExternalChange createExternalChange(String id, ExternalChangeTypeCode type) {
		var externalChange = new ExternalChange();
		externalChange.setId(id);
		externalChange.setType(type);
		return externalChange;
	}

	private static GetNadSheetResponse createFund() {
		var nps = new NadPrimarySheet();
		nps.setId(FUND_UUID);
		nps.setEvidenceNumber("123");
		var gnsr = new GetNadSheetResponse();
		gnsr.setNadPrimarySheet(nps);
		return gnsr;
	}

}
