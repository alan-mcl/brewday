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

import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class HopAdditionPane extends IngredientAdditionPane<HopAddition, Hop>
{
	public HopAdditionPane(TrackDirty parent)
	{
		super(parent);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientComboBox(
			"hop.addition.name",
			HopAddition::getHop,
			HopAddition::setHop,
			IngredientAddition.Type.HOPS);

		addQuantityControl(
			"hop.addition.weight",
			HopAddition::getQuantity,
			HopAddition::setQuantity,
			Quantity.Unit.GRAMS);

		addQuantityControl(
			"hop.addition.time",
			HopAddition::getTime,
			HopAddition::setTime,
			Quantity.Unit.MINUTES);
	}
}
