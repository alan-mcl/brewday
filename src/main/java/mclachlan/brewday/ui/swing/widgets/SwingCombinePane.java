package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.process.Combine;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

public class SwingCombinePane extends SwingProcessStepPane<Combine>
{
	public SwingCombinePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Combine::getInputVolume, Combine::setInputVolume,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);
		addInputVolumeComboBox("combine.input.2", Combine::getInputVolume2, Combine::setInputVolume2,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);
		addAddIngredientButton(IngredientAddition.Type.HOPS);
		addAddIngredientButton(IngredientAddition.Type.WATER);
		addComputedVolumePane("volumes.out", Combine::getOutputVolume);
	}
}
