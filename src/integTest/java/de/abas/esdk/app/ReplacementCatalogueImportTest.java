package de.abas.esdk.app;

import de.abas.erp.db.infosystem.custom.ow1.ReplacementCatalogue;
import de.abas.erp.db.schema.custom.replacement.SparePart;
import de.abas.erp.db.schema.userenums.UserEnumSpareImportFileFormat;
import de.abas.erp.db.schema.vendor.Vendor;
import de.abas.erp.db.schema.vendor.VendorEditor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.esdk.test.util.EsdkIntegTest;
import de.abas.esdk.test.util.ServerSideErrorMessageException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static org.junit.Assert.fail;

public class ReplacementCatalogueImportTest extends EsdkIntegTest {

	private static final String ICON_ADD = "icon:plus";
	private static final String ICON_UPDATE = "icon:replace";

	private static Vendor VENDOR;
	private ReplacementCatalogue replacementCatalogue = ctx.openInfosystem(ReplacementCatalogue.class);

	@BeforeClass
	public static void createTestData() {
		VendorEditor vendorEditor = ctx.newObject(VendorEditor.class);
		vendorEditor.setSwd("SPARETEST");
		vendorEditor.commit();
		VENDOR = vendorEditor.objectId();
	}

	@Test
	public void needsCatalogueImportFile() {
		try {
			replacementCatalogue.invokeStart();
			fail("ServerSideErrorMessageException expected due to missing import file location");
		} catch (ServerSideErrorMessageException e) {
			assertThat(e.getMessage(), containsString("Please enter the import file location."));
		}
	}

	@Test
	public void needsFormatSpecification() {
		replacementCatalogue.setFile("doesNotExist");
		replacementCatalogue.setFormat(UserEnumSpareImportFileFormat.Undefined);
		try {
			replacementCatalogue.invokeStart();
			fail("ServerSideErrorMessageException expected due to missing file format");
		} catch (ServerSideErrorMessageException e) {
			assertThat(e.getMessage(), containsString("Please enter the import file format."));
		}
	}

	@Test
	public void needsExistingImportFile() {
		replacementCatalogue.setFile("doesNotExist.csv");
		try {
			replacementCatalogue.invokeStart();
			fail("ServerSideErrorMessageException expected due to non-existing file");
		} catch (ServerSideErrorMessageException e) {
			assertThat(e.getMessage(), containsString("File does not exist. Please enter a valid import file location."));
		}
	}

	@Test
	public void canDisplayReplacementCatalogueItems() {
		replacementCatalogue.setFile("ow1/REPLC1.TEST.CSV");
		replacementCatalogue.invokeStart();
		ReplacementCatalogue.Table table = replacementCatalogue.table();
		assertThat(table.getRowCount(), is(3));
		validateRow(table.getRow(1), "72645", "Mutter, selbstsichernd", "MU34", 3.12);
		validateRow(table.getRow(2), "92384", "Schraube 60mm, Senkkopf", "SCREW", 5.42);
		validateRow(table.getRow(3), "83474", "Blech, titan", "TITAN", 7.98);
	}

	@Test
	public void canCreateSpareParts() {
		importReplacementCatalogue("ow1/REPLC1.TEST.CSV", ICON_ADD);
		validateSparePart("72645", "Mutter, selbstsichernd", 3.12, "MU34");
		validateSparePart("92384", "Schraube 60mm, Senkkopf", 5.42, "SCREW");
		validateSparePart("83474", "Blech, titan", 7.98, "TITAN");
	}

	@Test
	public void canUpdateSpareParts() {
		importReplacementCatalogue("ow1/REPLC1.TEST.CSV", ICON_ADD);
		importReplacementCatalogue("ow1/REPLC2.TEST.CSV", ICON_UPDATE);
		validateSparePart("72645", "Mutter, selbstsichernd", 3.12, "MU34");
		validateSparePart("92384", "Schraube 65mm, Senkkopf", 5.42, "SCREW");
		validateSparePart("83474", "Blech, Titan", 9.98, "TITAN");
	}

	private void importReplacementCatalogue(final String file, final String icon) {
		replacementCatalogue.setFile(file);
		replacementCatalogue.invokeStart();
		replacementCatalogue.setVendor(VENDOR);
		for (final ReplacementCatalogue.Row row : replacementCatalogue.table().getRows()) {
			row.setImport(true);
		}
		replacementCatalogue.invokeStartimport();
		for (final ReplacementCatalogue.Row row : replacementCatalogue.table().getRows()) {
			assertThat(row.getTransfericon(), is(icon));
		}
	}

	@After
	public void cleanup() {
		replacementCatalogue.abort();
		for (final SparePart sparePart : ctx.createQuery(SelectionBuilder.create(SparePart.class).build())) {
			sparePart.delete();
		}
	}

	@AfterClass
	public static void deleteTestData() {
		VENDOR.delete();
	}

	private void validateSparePart(final String productNo, final String descr, final double price, final String matchcode) {
		List<SparePart> spareParts = ctx.createQuery(SelectionBuilder.create(SparePart.class).add(Conditions.eq(SparePart.META.yvendor, VENDOR)).add(Conditions.eq(SparePart.META.ymatchcode, matchcode)).build()).execute();
		assertThat(spareParts.size(), is(1));
		SparePart sparePart = spareParts.get(0);
		assertThat(sparePart.getYproductno(), is(productNo));
		assertThat(sparePart.getYdescr(), is(descr));
		assertThat(sparePart.getYprice(), is(closeTo(new BigDecimal(price), new BigDecimal(0.001))));
		assertThat(sparePart.getYdate(), is(notNullValue()));
		assertThat(sparePart.getYchanged(), is(notNullValue()));
		assertThat(sparePart.getYsigned(), is("sy"));
		assertThat(sparePart.getYtransferred(), is(true));
	}

	private void validateRow(final ReplacementCatalogue.Row row, final boolean toBeImported, final String icon, final String productNo, final String descr, final String matchcode, final double price) {
		assertThat(row.getImport(), is(toBeImported));
		assertThat(row.getTransfericon(), is(icon));
		assertThat(row.getProductno(), is(productNo));
		assertThat(row.getDescr(), is(descr));
		assertThat(row.getMatchcode(), is(matchcode));
		assertThat(row.getPrice(), is(closeTo(new BigDecimal(price), new BigDecimal(0.001))));
	}

	private void validateRow(final ReplacementCatalogue.Row row, final String productNo, final String descr, final String matchcode, final double price) {
		validateRow(row, false, "", productNo, descr, matchcode, price);
	}

}
