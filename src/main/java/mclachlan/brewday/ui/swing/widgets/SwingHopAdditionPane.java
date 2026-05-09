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

import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

/**
 * Swing analogue of JFX {@code HopAdditionPane}.
 */
public class SwingHopAdditionPane extends SwingIngredientAdditionPane<HopAddition, Hop>
{
	public SwingHopAdditionPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree)
	{
		super(dirtyState, recipeTree);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientLabel("hop.addition.name", HopAddition::getHop, Hop::getName);
		addIngredientLabel("hop.form", HopAddition::getHop, Hop::getForm);

		addQuantitySelectAndEditControl("hop.addition.amount",
			HopAddition::getQuantity, HopAddition::setQuantity,
			HopAddition::getUnit, HopAddition::setUnit,
			Quantity.Unit.GRAMS,
			Quantity.Type.WEIGHT, Quantity.Type.VOLUME);

		addTimeUnitControl("hop.addition.time",
			HopAddition::getTime, HopAddition::setTime, Quantity.Unit.MINUTES);
	}
}
