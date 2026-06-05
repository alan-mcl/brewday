package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Steep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingWaterAdditionPaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void waterAdditionPaneTemperatureEditDirtiesAddition() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Assume.assumeFalse(Database.getInstance().getWaters().isEmpty());

		Water water = Database.getInstance().getWaters().values().iterator().next();
		Recipe r = new Recipe("WaterPaneT");
		Steep steep = (Steep)RecipeEditorSteps.createStep(r, ProcessStep.Type.STEEP);
		r.getSteps().add(steep);
		WaterAddition wa = new WaterAddition(
			water,
			(VolumeUnit)Quantity.parseQuantity("2", LITRES),
			LITRES,
			new TemperatureUnit(18, CELSIUS, false),
			new TimeUnit(0, MINUTES, false));
		steep.addIngredientAddition(wa);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingWaterAdditionPane pane = new SwingWaterAdditionPane(dirty, tree);

		SwingUtilities.invokeAndWait(() -> pane.refresh(wa, r));

		dirty.removeDirty(wa);

		SwingUtilities.invokeAndWait(() ->
		{
			java.util.List<SwingQuantityEditWidget<?>> edits = findQuantityEditWidgets(pane.getFormForTest());
			assertTrue(edits.size() >= 2);
			SwingQuantityEditWidget<?> tempW = edits.get(1);
			assertNotNull(tempW);
			tempW.getTextField().setText("22");
			tempW.getTextField().transferFocus();
		});

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(22.0, wa.getTemperature().get(CELSIUS), 0.001);
			assertTrue(dirty.isDirty(wa));
		});
	}

	private static java.util.List<SwingQuantityEditWidget<?>> findQuantityEditWidgets(JPanel form)
	{
		java.util.List<SwingQuantityEditWidget<?>> list = new java.util.ArrayList<>();
		for (Component c : form.getComponents())
		{
			if (c instanceof SwingQuantityEditWidget<?> w)
			{
				list.add(w);
			}
		}
		return list;
	}
}
