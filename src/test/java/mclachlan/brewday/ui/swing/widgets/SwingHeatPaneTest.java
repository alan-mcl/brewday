package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.Heat;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwingHeatPaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void heatPaneShowsExpectedRowsAndComputedVolume() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("HeatPaneT");
		Heat heat = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);
		r.getSteps().add(heat);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingHeatPane pane = new SwingHeatPane(dirty, tree, false);

		SwingUtilities.invokeAndWait(() -> pane.refresh(heat, r));

		SwingUtilities.invokeAndWait(() ->
		{
			int labels = countLabels(pane.getFormForTest());
			assertEquals(4, labels);
			assertTrue(pane.getInputVolumeComboForTest(0).getItemCount() >= 1);
			Component south = pane.getComponent(2);
			assertTrue(south instanceof JPanel);
			assertEquals(1, ((JPanel)south).getComponentCount());
		});
	}

	private static int countLabels(JPanel form)
	{
		int n = 0;
		for (Component c : form.getComponents())
		{
			if (c instanceof JLabel)
			{
				n++;
			}
		}
		return n;
	}
}
