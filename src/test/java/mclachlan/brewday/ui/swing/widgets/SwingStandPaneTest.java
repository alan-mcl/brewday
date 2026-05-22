package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingStandPaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void standPaneShowsDurationRowAndEditingDirtiesStep() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("StandPaneT");
		Stand stand = (Stand)RecipeEditorSteps.createStep(r, ProcessStep.Type.STAND);
		r.getSteps().add(stand);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingStandPane pane = new SwingStandPane(dirty, tree, false);

		SwingUtilities.invokeAndWait(() -> pane.refresh(stand, r));

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(3, countLabels(pane.getFormForTest()));
			assertEquals(30.0, stand.getDuration().get(Quantity.Unit.MINUTES), 0.001);
			Component south = pane.getComponent(2);
			assertTrue(south instanceof JPanel);
			assertEquals(1, ((JPanel)south).getComponentCount());
		});

		dirty.removeDirty(stand);

		SwingUtilities.invokeAndWait(() ->
		{
			SwingQuantityEditWidget<?> w = findFirstQuantityWidget(pane.getFormForTest());
			assertNotNull(w);
			w.getTextField().setText("45");
			w.getTextField().transferFocus();
		});

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(45.0, stand.getDuration().get(Quantity.Unit.MINUTES), 0.001);
			assertTrue(dirty.isDirty(stand));
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

	private static SwingQuantityEditWidget<?> findFirstQuantityWidget(JPanel form)
	{
		for (Component c : form.getComponents())
		{
			if (c instanceof SwingQuantityEditWidget<?> w)
			{
				return w;
			}
		}
		return null;
	}
}
