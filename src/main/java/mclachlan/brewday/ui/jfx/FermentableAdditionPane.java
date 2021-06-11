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

import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class FermentableAdditionPane extends IngredientAdditionPane<FermentableAddition, Fermentable>
{
	public FermentableAdditionPane(TrackDirty parent, RecipeTreeView model)
	{
		super(parent, model);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientComboBox(
			"fermentable.addition.name",
			FermentableAddition::getFermentable,
			FermentableAddition::setFermentable,
			IngredientAddition.Type.FERMENTABLES);

		getUnitControlUtils().addWeightUnitControl(
			this,
			"fermentable.addition.weight",
			FermentableAddition::getQuantity,
			FermentableAddition::setQuantity,
			Quantity.Unit.KILOGRAMS);

		getUnitControlUtils().addTimeUnitControl(
			this,
			"fermentable.addition.time",
			FermentableAddition::getTime,
			FermentableAddition::setTime,
			Quantity.Unit.MINUTES);
	}
}
