package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.FreezeConcentrate;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

public class SwingFreezeConcentratePane extends SwingProcessStepPane<FreezeConcentrate>
{
	public SwingFreezeConcentratePane(
		DirtyStateService dirtyState,
		SwingRecipeTree recipeTree,
		boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("freeze.concentrate.input",
			FreezeConcentrate::getInputVolume, FreezeConcentrate::setInputVolume,
			Volume.Type.BEER);

		addTimeUnitControl("freeze.concentrate.duration",
			FreezeConcentrate::getDuration, FreezeConcentrate::setDuration,
			Quantity.Unit.HOURS);

		addTemperatureUnitControl("freeze.concentrate.freezer.temp",
			FreezeConcentrate::getFreezerTemperature, FreezeConcentrate::setFreezerTemperature,
			Quantity.Unit.CELSIUS);

		addComputedVolumePane("freeze.concentrate.output", FreezeConcentrate::getOutputVolume);
	}
}
