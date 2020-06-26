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

import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;

/**
 *
 */
public class MiscAdditionPane extends IngredientAdditionPane<MiscAddition, Misc>
{
	public MiscAdditionPane(TrackDirty parent, RecipeTreeViewModel model)
	{
		super(parent, model);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientComboBox(
			"misc.misc",
			MiscAddition::getMisc,
			MiscAddition::setMisc,
			IngredientAddition.Type.MISC);

		getUnitControlUtils().addWeightUnitControl(
			this,
			"misc.weight",
			MiscAddition::getQuantity,
			MiscAddition::setQuantity,
			Quantity.Unit.GRAMS);

		getUnitControlUtils().addTimeUnitControl(
			this,
			"misc.addition.time",
			MiscAddition::getTime,
			MiscAddition::setTime,
			Quantity.Unit.MINUTES);
	}
}
