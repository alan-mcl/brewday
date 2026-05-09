package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

public class SwingStandPane extends SwingProcessStepPane<Stand>
{
	public SwingStandPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Stand::getInputVolume, Stand::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);
		addTimeUnitControl("stand.duration", Stand::getDuration, Stand::setDuration, Quantity.Unit.MINUTES);
		addAddIngredientButton(IngredientAddition.Type.HOPS);
		addAddIngredientButton(IngredientAddition.Type.WATER);
		addAddIngredientButton(IngredientAddition.Type.FERMENTABLES);
		addAddIngredientButton(IngredientAddition.Type.YEAST);
		addAddIngredientButton(IngredientAddition.Type.MISC);
		addComputedVolumePane("volumes.out", Stand::getOutputVolume);
	}
}
