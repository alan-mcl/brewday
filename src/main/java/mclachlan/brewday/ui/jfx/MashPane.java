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

import javafx.scene.control.Label;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ButtonType.*;

/**
 *
 */
public class MashPane extends ProcessStepPane<Mash>
{
	private Label mashTemp;

	/*-------------------------------------------------------------------------*/
	public MashPane(TrackDirty parent)
	{
		super(parent);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void buildUiInternal()
	{
		addToolbar(ADD_FERMENTABLE, ADD_HOP, ADD_MISC, ADD_WATER, DUPLICATE, SUBSTITUTE, DELETE);

		addTemperatureUnitControl("mash.grain.temp",
			Mash::getGrainTemp,
			Mash::setGrainTemp,
			Quantity.Unit.CELSIUS);

		mashTemp = new Label();
		this.add(new Label(StringUtils.getUiString("mash.temp")));
		this.add(mashTemp, "wrap");

		addTimeUnitControl("mash.duration",
			Mash::getDuration,
			Mash::setDuration,
			Quantity.Unit.MINUTES);

		addComputedVolumePane("mash.volume.created", Mash::getOutputMashVolume);
		addComputedVolumePane("mash.first.runnings", Mash::getOutputFirstRunnings);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void refreshInternal(Mash step, Recipe recipe)
	{
		if (step != null)
		{
			double v = step.getMashTemp()==null ? Double.NaN : step.getMashTemp().get(Quantity.Unit.CELSIUS);
			mashTemp.setText(StringUtils.getUiString("mash.temp.format", v));
		}
	}
}
