package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingHopAdditionPaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void hopAdditionPaneShowsFieldsAndTimeEditDirtiesAddition() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Assume.assumeFalse(Database.getInstance().getHops().isEmpty());

		Hop hop = Database.getInstance().getHops().values().iterator().next();
		Recipe r = new Recipe("HopPaneT");
		Stand stand = (Stand)RecipeEditorSteps.createStep(r, ProcessStep.Type.STAND);
		r.getSteps().add(stand);
		HopAddition ha = new HopAddition(hop,
			Quantity.parseQuantity("10", Quantity.Unit.GRAMS),
			Quantity.Unit.GRAMS,
			new TimeUnit(5, MINUTES, false));
		stand.addIngredientAddition(ha);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingHopAdditionPane pane = new SwingHopAdditionPane(dirty, tree);

		SwingUtilities.invokeAndWait(() -> pane.refresh(ha, r));

		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(countLabels(pane.getFormForTest()) >= 4);
			assertTrue(formContainsText(pane.getFormForTest(), hop.getName()));
		});

		dirty.removeDirty(ha);

		SwingUtilities.invokeAndWait(() ->
		{
			SwingQuantityEditWidget<?> timeW = findFirstQuantityEditWidget(pane.getFormForTest());
			assertNotNull(timeW);
			timeW.getTextField().setText("60");
			timeW.getTextField().transferFocus();
		});

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(60.0, ha.getTime().get(MINUTES), 0.001);
			assertTrue(dirty.isDirty(ha));
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

	private static boolean formContainsText(JPanel form, String text)
	{
		for (Component c : form.getComponents())
		{
			if (c instanceof JLabel lab && text.equals(lab.getText()))
			{
				return true;
			}
		}
		return false;
	}

	private static SwingQuantityEditWidget<?> findFirstQuantityEditWidget(JPanel form)
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
