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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.jfx;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Cool;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class CoolPane extends ProcessStepPane<Cool>
{
	public CoolPane(TrackDirty parent)
	{
		super(parent);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in",
			Cool::getInputVolume,
			Cool::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		addTemperatureUnitControl("cool.target.temp",
			Cool::getTargetTemp,
			Cool::setTargetTemp,
			Quantity.Unit.CELSIUS);

		addComputedVolumePane("boil.wort.out", Cool::getOutputVolume);
	}
}
