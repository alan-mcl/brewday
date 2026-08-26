package mclachlan.brewday.ui.swing.screens;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.SwingTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AboutScreenTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@BeforeClass
	public static void requireDisplay()
	{
		SwingTestSupport.assumeDisplay();
	}

	@Test
	public void aboutFieldsAreReadableAndCopyable()
	{
		AboutScreen screen = new AboutScreen();

		assertFalse(screen.getAppField().isEditable());
		assertFalse(screen.getSourceField().isEditable());
		assertFalse(screen.getDbField().isEditable());
		assertFalse(screen.getLogField().isEditable());
		assertFalse(screen.getCreditsArea().isEditable());

		assertTrue(screen.getAppField().getText().contains("Brewday"));
		assertTrue(screen.getSourceField().getText().contains("github.com/alanmclachlan/brewday"));
		assertTrue(screen.getDbField().getText().contains("Local database"));
		assertTrue(screen.getLogField().getText().contains("Log file location"));
		assertTrue(screen.getCreditsArea().getText().contains("GNU Affero General Public License"));
	}
}
