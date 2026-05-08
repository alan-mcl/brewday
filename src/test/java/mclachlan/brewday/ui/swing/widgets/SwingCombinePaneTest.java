package mclachlan.brewday.ui.swing.widgets;

import java.awt.GraphicsEnvironment;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.Combine;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;

public class SwingCombinePaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void combinePaneHasTwoIndependentInputCombos() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("CombPaneT");
		Combine combine = (Combine)RecipeEditorSteps.createStep(r, ProcessStep.Type.COMBINE);
		r.getSteps().add(combine);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingCombinePane pane = new SwingCombinePane(dirty, tree, false);

		SwingUtilities.invokeAndWait(() -> pane.refresh(combine, r));

		SwingUtilities.invokeAndWait(() ->
		{
			JComboBox<String> c0 = pane.getInputVolumeComboForTest(0);
			JComboBox<String> c1 = pane.getInputVolumeComboForTest(1);
			assertNotSame(c0, c1);
		});
	}
}
