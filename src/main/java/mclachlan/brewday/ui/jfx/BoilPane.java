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
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ButtonType.*;

/**
 *
 */
public class BoilPane extends ProcessStepPane<Boil>
{
	public BoilPane(TrackDirty parent, RecipeTreeViewModel stepsTreeModel)
	{
		super(parent, stepsTreeModel);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ADD_FERMENTABLE, ADD_HOP, ADD_MISC, ADD_WATER, DUPLICATE, DELETE);

		addInputVolumeComboBox("boil.wort.in",
			Boil::getInputWortVolume,
			Boil::setInputWortVolume,
			Volume.Type.WORT);

		getUnitControlUtils().addTimeUnitControl(this, "boil.duration", Boil::getDuration, Boil::setDuration, Quantity.Unit.MINUTES);

		addComputedVolumePane("boil.wort.out", Boil::getOutputWortVolume);
	}
}
