package de.abas.esdk.app;

import de.abas.erp.db.infosystem.custom.ow1.ReplacementCatalogue;
import de.abas.esdk.test.util.EsdkIntegTest;
import de.abas.esdk.test.util.ServerSideErrorMessageException;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
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

	@After
	public void cleanup() {
		replacementCatalogue.abort();
	}

}