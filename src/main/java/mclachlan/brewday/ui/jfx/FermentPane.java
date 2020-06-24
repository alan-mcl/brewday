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
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ButtonType.*;

/**
 *
 */
public class FermentPane extends ProcessStepPane<Ferment>
{
	private Label estFG;

	public FermentPane(TrackDirty parent, RecipeTreeViewModel stepsTreeModel)
	{
		super(parent, stepsTreeModel);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ADD_HOP, ADD_MISC, ADD_YEAST, DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			Ferment::getInputVolume,
			Ferment::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		getControlUtils().addTemperatureUnitControl(this, "ferment.temp", Ferment::getTemperature, Ferment::setTemperature, Quantity.Unit.CELSIUS);

		getControlUtils().addTimeUnitControl(this, "ferment.duration", Ferment::getDuration, Ferment::setDuration, Quantity.Unit.DAYS);

		estFG = new Label();
		this.add(new Label(StringUtils.getUiString("ferment.fg")));
		this.add(estFG, "wrap");

		addComputedVolumePane("volumes.out", Ferment::getOutputVolume);
	}

	@Override
	protected void refreshInternal(Ferment step, Recipe recipe)
	{
		if (step != null)
		{
			estFG.setText(
				String.format("%.3f",
					step.getEstimatedFinalGravity().get(
						DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}
	}
}
