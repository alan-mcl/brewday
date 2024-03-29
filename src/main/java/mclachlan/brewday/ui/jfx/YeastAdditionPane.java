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

import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;

/**
 *
 */
public class YeastAdditionPane extends IngredientAdditionPane<YeastAddition, Yeast>
{
	public YeastAdditionPane(TrackDirty parent, RecipeTreeView model)
	{
		super(parent, model);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientLabels(
			"yeast.yeast",
			YeastAddition::getYeast,
			YeastAddition::setYeast,
			IngredientAddition.Type.YEAST,
			Yeast::getName);

		Quantity.Type[] types = {Quantity.Type.VOLUME, Quantity.Type.WEIGHT};

		getUnitControlUtils().addQuantityEditAndSelectControl(
			this,
			"yeast.addition.amount",
			YeastAddition::getQuantity,
			YeastAddition::setQuantity,
			YeastAddition::getUnit,
			YeastAddition::setUnit,
			Quantity.Unit.GRAMS,
			YeastAddition::getAdditionQuantityType,
			types);

		getUnitControlUtils().addTimeUnitControl(
			this,
			"yeast.addition.time",
			YeastAddition::getTime,
			YeastAddition::setTime,
			Quantity.Unit.DAYS);
	}
}
