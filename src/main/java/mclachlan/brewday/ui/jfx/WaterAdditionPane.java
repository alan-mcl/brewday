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

import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class WaterAdditionPane extends IngredientAdditionPane<WaterAddition, Water>
{
	public WaterAdditionPane(TrackDirty parent)
	{
		super(parent);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientComboBox(
			"water.addition.name",
			WaterAddition::getWater,
			WaterAddition::setWater,
			IngredientAddition.Type.WATER);

		getControlUtils().addTimeUnitControl(
			this,
			"water.addition.time",
			WaterAddition::getTime,
			WaterAddition::setTime,
			Quantity.Unit.MINUTES);

		getControlUtils().addVolumeUnitControl(
			this,
			"water.addition.volume",
			WaterAddition::getVolume,
			WaterAddition::setVolume,
			Quantity.Unit.LITRES);

		getControlUtils().addTemperatureUnitControl(
			this,
			"water.addition.temperature",
			WaterAddition::getTemperature,
			WaterAddition::setTemperature,
			Quantity.Unit.CELSIUS);
	}
}
