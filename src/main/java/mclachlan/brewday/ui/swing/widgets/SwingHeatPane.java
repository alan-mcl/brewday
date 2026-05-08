package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Heat;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

public class SwingHeatPane extends SwingProcessStepPane<Heat>
{
	public SwingHeatPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Heat::getInputVolume, Heat::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER, Volume.Type.MASH);

		addTemperatureUnitControl("heat.target.temp", Heat::getTargetTemp, Heat::setTargetTemp, Quantity.Unit.CELSIUS);
		addTimeUnitControl("heat.ramp.time", Heat::getRampTime, Heat::setRampTime, Quantity.Unit.MINUTES);
		addTimeUnitControl("heat.stand.time", Heat::getStandTime, Heat::setStandTime, Quantity.Unit.MINUTES);

		addAddIngredientButton(IngredientAddition.Type.HOPS);
		addAddIngredientButton(IngredientAddition.Type.WATER);

		addComputedVolumePane("heat.wort.out", Heat::getOutputVolume);
	}
}
