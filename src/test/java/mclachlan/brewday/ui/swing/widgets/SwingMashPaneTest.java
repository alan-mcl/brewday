package mclachlan.brewday.ui.swing.widgets;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SwingMashPaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void mashToolbarHasIngredientButtonsPlusMashUtilities() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("MashPaneUtil");
		Mash mash = (Mash)RecipeEditorSteps.createStep(r, ProcessStep.Type.MASH);
		r.getSteps().add(mash);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingMashPane pane = new SwingMashPane(dirty, tree, false);

		SwingUtilities.invokeAndWait(() -> pane.refresh(mash, r));

		SwingUtilities.invokeAndWait(() ->
		{
			int n = pane.getStepToolbarForTest().getComponentCount();
			assertTrue("toolbar count=" + n, n >= 8);
		});
	}
}
