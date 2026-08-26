package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import javax.swing.SwingUtilities;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Test;

public class SwingMiscAdditionPaneTest
{
	@Test
	public void refreshIncludesMeasurementTypeBeyondWeightAndVolume() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		Misc misc = new Misc("TempMiscProbe");
		misc.setMeasurementType(Quantity.Type.TEMPERATURE);
		MiscAddition ma = new MiscAddition(misc,
			new TemperatureUnit(20, Quantity.Unit.CELSIUS),
			Quantity.Unit.CELSIUS,
			new TimeUnit(0));
		Recipe r = new Recipe("MiscPaneT");
		Boil boil = (Boil)RecipeEditorSteps.createStep(r, ProcessStep.Type.BOIL);
		r.getSteps().add(boil);
		boil.getIngredientAdditions().add(ma);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		tree.setRecipe(r);
		SwingMiscAdditionPane pane = new SwingMiscAdditionPane(dirty, tree);

		SwingUtilities.invokeAndWait(() -> pane.refresh(ma, r));
	}
}
