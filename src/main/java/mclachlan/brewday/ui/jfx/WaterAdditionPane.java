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

import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class WaterAdditionPane extends IngredientAdditionPane<WaterAddition, Water>
{
	public WaterAdditionPane(TrackDirty parent, RecipeTreeViewModel model)
	{
		super(parent, model);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientComboBox(
			"water.addition.name",
			WaterAddition::getWater,
			WaterAddition::setWater,
			IngredientAddition.Type.WATER);

		getUnitControlUtils().addVolumeUnitControl(
			this,
			"water.addition.volume",
			WaterAddition::getVolume,
			WaterAddition::setVolume,
			Quantity.Unit.LITRES);

		getUnitControlUtils().addTimeUnitControl(
			this,
			"water.addition.time",
			WaterAddition::getTime,
			WaterAddition::setTime,
			Quantity.Unit.MINUTES);

		getUnitControlUtils().addTemperatureUnitControl(
			this,
			"water.addition.temperature",
			WaterAddition::getTemperature,
			WaterAddition::setTemperature,
			Quantity.Unit.CELSIUS);
	}
}
