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
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class BoilPane extends ProcessStepPane<Boil>
{
	public BoilPane(TrackDirty parent)
	{
		super(parent);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("boil.wort.in",
			Boil::getInputWortVolume,
			Boil::setInputWortVolume,
			Volume.Type.WORT);

		addTimeUnitControl("boil.duration",
			Boil::getDuration,
			Boil::setDuration,
			Quantity.Unit.MINUTES);

		addComputedVolumePane("boil.wort.out", Boil::getOutputWortVolume);
	}
}
