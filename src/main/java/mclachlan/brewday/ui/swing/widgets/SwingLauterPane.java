package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.process.Lauter;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

/**
 * Swing analogue of JFX {@code LauterPane}.
 */
public class SwingLauterPane extends SwingProcessStepPane<Lauter>
{
	public SwingLauterPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("lauter.mash",
			Lauter::getInputMashVolume,
			Lauter::setInputMashVolume,
			Volume.Type.MASH);

		addIngredientButtonsForPrototype(new Lauter());

		addComputedVolumePane("lauter.first.runnings", Lauter::getOutputFirstRunnings);
		addComputedVolumePane("lauter.lautered.mash", Lauter::getOutputLauteredMashVolume);
	}
}
