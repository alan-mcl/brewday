/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Heat;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.DELETE;
import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.DUPLICATE;

/**
 *
 */
public class HeatPane extends ProcessStepPane<Heat>
{
	public HeatPane(TrackDirty parent, RecipeTreeView stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(new Heat().getSupportedIngredientAdditions(), DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			Heat::getInputVolume,
			Heat::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER, Volume.Type.MASH);

		getUnitControlUtils().addTemperatureUnitControl(this, "heat.target.temp",
			Heat::getTargetTemp, Heat::setTargetTemp, Quantity.Unit.CELSIUS);

		getUnitControlUtils().addTimeUnitControl(this, "heat.ramp.time",
			Heat::getRampTime, Heat::setRampTime, Quantity.Unit.MINUTES);

		getUnitControlUtils().addTimeUnitControl(this, "heat.stand.time",
			Heat::getStandTime, Heat::setStandTime, Quantity.Unit.MINUTES);

		addComputedVolumePane("heat.wort.out", Heat::getOutputVolume);
	}
}
