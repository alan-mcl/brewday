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

package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

/**
 * Swing analogue of JFX {@code WaterAdditionPane}.
 */
public class SwingWaterAdditionPane extends SwingIngredientAdditionPane<WaterAddition, Water>
{
	public SwingWaterAdditionPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree)
	{
		super(dirtyState, recipeTree);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientLabel("water.addition.name", WaterAddition::getWater, Water::getName);

		addQuantitySelectAndEditControl("water.addition.amount",
			WaterAddition::getQuantity, WaterAddition::setQuantity,
			WaterAddition::getUnit, WaterAddition::setUnit,
			Quantity.Unit.LITRES,
			Quantity.Type.VOLUME);

		addTimeUnitControl("water.addition.time",
			WaterAddition::getTime, WaterAddition::setTime, Quantity.Unit.MINUTES);

		addTemperatureUnitControl("water.addition.temperature",
			WaterAddition::getTemperature, WaterAddition::setTemperature, Quantity.Unit.CELSIUS);
	}
}
