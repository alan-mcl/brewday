package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Cool;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

public class SwingCoolPane extends SwingProcessStepPane<Cool>
{
	public SwingCoolPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Cool::getInputVolume, Cool::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		addTemperatureUnitControl("cool.target.temp", Cool::getTargetTemp, Cool::setTargetTemp, Quantity.Unit.CELSIUS);

		addAddIngredientButton(IngredientAddition.Type.HOPS);
		addAddIngredientButton(IngredientAddition.Type.WATER);

		addComputedVolumePane("cool.wort.out", Cool::getOutputVolume);
	}
}
