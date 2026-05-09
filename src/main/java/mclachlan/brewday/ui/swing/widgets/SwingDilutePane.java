package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.process.Dilute;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

public class SwingDilutePane extends SwingProcessStepPane<Dilute>
{
	public SwingDilutePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Dilute::getInputVolume, Dilute::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);
		addAddIngredientButton(IngredientAddition.Type.WATER);
		addComputedVolumePane("volumes.out", Dilute::getOutputVolume);
	}
}
