package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.YeastRehydrate;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingYeastRehydratePaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void yeastRehydratePaneShowsDurationRowAndEditingDirtiesStep() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("YeastRehydratePaneT");
		YeastRehydrate yeastRehydrate = (YeastRehydrate)RecipeEditorSteps.createStep(r,
			ProcessStep.Type.YEAST_REHYDRATE);
		r.getSteps().add(yeastRehydrate);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingYeastRehydratePane pane = new SwingYeastRehydratePane(dirty, tree, false);

		SwingUtilities.invokeAndWait(() -> pane.refresh(yeastRehydrate, r));

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(3, countLabels(pane.getFormForTest()));
			assertEquals(30.0, yeastRehydrate.getDuration().get(Quantity.Unit.MINUTES), 0.001);
			Component south = pane.getComponent(2);
			assertTrue(south instanceof JPanel);
			assertEquals(1, ((JPanel)south).getComponentCount());
		});

		dirty.removeDirty(yeastRehydrate);

		SwingUtilities.invokeAndWait(() ->
		{
			SwingQuantityEditWidget<?> w = findFirstQuantityWidget(pane.getFormForTest());
			assertNotNull(w);
			w.getTextField().setText("45");
			w.getTextField().transferFocus();
		});

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(45.0, yeastRehydrate.getDuration().get(Quantity.Unit.MINUTES), 0.001);
			assertTrue(dirty.isDirty(yeastRehydrate));
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
