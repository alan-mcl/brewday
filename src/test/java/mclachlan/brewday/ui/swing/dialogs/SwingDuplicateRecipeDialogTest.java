package mclachlan.brewday.ui.swing.dialogs;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingDuplicateRecipeDialogTest
{
	private String sourceName;
	private String takenName;
	private String copyName;

	@Before
	public void setUp()
	{
		SwingTestSupport.assumeDisplay();
		long n = System.nanoTime();
		sourceName = "ZZ_DupSrc_" + n;
		takenName = "ZZ_DupTk_" + n;
		copyName = "ZZ_DupNew_" + n;
	}

	@After
	public void tearDown()
	{
		Database db = Database.getInstance();
		db.getRecipes().remove(sourceName);
		db.getRecipes().remove(takenName);
		db.getRecipes().remove(copyName);
	}

	@Test
	public void emptyAndTakenNameDisableOkValidReturnsCopy() throws Exception
	{
		Database.getInstance().loadAll();
		Database db = Database.getInstance();
		Recipe source = new Recipe(sourceName);
		db.getRecipes().put(sourceName, source);
		db.getRecipes().put(takenName, new Recipe(takenName));

		final SwingDuplicateRecipeDialog[] holder = new SwingDuplicateRecipeDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingDuplicateRecipeDialog(new JFrame(), source));
		SwingDuplicateRecipeDialog d = holder[0];

		SwingUtilities.invokeAndWait(() ->
		{
			d.setNameFieldForTest("");
			assertFalse(d.isOkEnabledForTest());
			d.setNameFieldForTest(takenName);
			assertFalse(d.isOkEnabledForTest());
			d.setNameFieldForTest(copyName);
			assertTrue(d.isOkEnabledForTest());
			d.confirmForTest();
		});

		Recipe out = d.getResult();
		assertNotNull(out);
		assertEquals(copyName, out.getName());
		assertEquals(sourceName, source.getName());
	}
}
