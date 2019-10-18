package de.abas.esdk.app;

import de.abas.erp.db.infosystem.custom.ow1.ReplacementCatalogue;
import de.abas.erp.db.schema.userenums.UserEnumSpareImportFileFormat;
import de.abas.esdk.test.util.EsdkIntegTest;
import de.abas.esdk.test.util.ServerSideErrorMessageException;
import org.junit.After;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static org.junit.Assert.fail;

public class ReplacementCatalogueImportTest extends EsdkIntegTest {

	private ReplacementCatalogue replacementCatalogue = ctx.openInfosystem(ReplacementCatalogue.class);

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

	@After
	public void cleanup() {
		replacementCatalogue.abort();
	}

}
