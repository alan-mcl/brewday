package mclachlan.brewday.ui.swing.dialogs;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwingRenameRecipeDialogTest
{
	private String fromName;
	private String blockName;
	private String targetName;

	@Before
	public void setUp()
	{
		SwingTestSupport.assumeDisplay();
		long n = System.nanoTime();
		fromName = "ZZ_RnFrom_" + n;
		blockName = "ZZ_RnBlock_" + n;
		targetName = "ZZ_RnTo_" + n;
	}

	@After
	public void tearDown()
	{
		Database db = Database.getInstance();
		db.getRecipes().remove(fromName);
		db.getRecipes().remove(blockName);
		db.getRecipes().remove(targetName);
	}

	@Test
	public void emptyAndDuplicateDisableOkValidEnables() throws Exception
	{
		Database.getInstance().loadAll();
		Database db = Database.getInstance();
		Recipe source = new Recipe(fromName);
		db.getRecipes().put(fromName, source);
		db.getRecipes().put(blockName, new Recipe(blockName));

		final SwingRenameRecipeDialog[] holder = new SwingRenameRecipeDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingRenameRecipeDialog(new JFrame(), source));
		SwingRenameRecipeDialog d = holder[0];

		SwingUtilities.invokeAndWait(() ->
		{
			d.setNameFieldForTest("");
			assertFalse(d.isOkEnabledForTest());
			d.setNameFieldForTest("  ");
			assertFalse(d.isOkEnabledForTest());
			d.setNameFieldForTest(blockName);
			assertFalse(d.isOkEnabledForTest());
			d.setNameFieldForTest(targetName);
			assertTrue(d.isOkEnabledForTest());
			d.confirmForTest();
		});

		assertTrue(d.getResult().equals(targetName));
	}
}
