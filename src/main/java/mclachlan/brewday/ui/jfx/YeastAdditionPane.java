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

import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;

/**
 *
 */
public class YeastAdditionPane extends IngredientAdditionPane<YeastAddition, Yeast>
{
	public YeastAdditionPane(TrackDirty parent)
	{
		super(parent);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientComboBox(
			"yeast.yeast",
			YeastAddition::getYeast,
			YeastAddition::setYeast,
			IngredientAddition.Type.YEAST);

		addQuantityControl(
			"yeast.weight",
			YeastAddition::getQuantity,
			YeastAddition::setQuantity,
			Quantity.Unit.GRAMS);

		addQuantityControl(
			"yeast.time",
			YeastAddition::getTime,
			YeastAddition::setTime,
			Quantity.Unit.DAYS);
	}
}
