package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Stand;
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
		addComputedVolumePane("volumes.out", Stand::getOutputVolume);
	}
}
