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

import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

/**
 * Swing analogue of JFX {@code FermentableAdditionPane}.
 */
public class SwingFermentableAdditionPane extends SwingIngredientAdditionPane<FermentableAddition, Fermentable>
{
	public SwingFermentableAdditionPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree)
	{
		super(dirtyState, recipeTree);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientLabel("fermentable.addition.name", FermentableAddition::getFermentable, Fermentable::getName);

		addQuantitySelectAndEditControl("fermentable.addition.amount",
			FermentableAddition::getQuantity, FermentableAddition::setQuantity,
			FermentableAddition::getUnit, FermentableAddition::setUnit,
			Quantity.Unit.GRAMS,
			Quantity.Type.WEIGHT, Quantity.Type.VOLUME);

		addTimeUnitControl("fermentable.addition.time",
			FermentableAddition::getTime, FermentableAddition::setTime, Quantity.Unit.MINUTES);
	}
}
