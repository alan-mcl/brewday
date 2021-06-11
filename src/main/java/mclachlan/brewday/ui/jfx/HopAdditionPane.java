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

import java.util.*;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class HopAdditionPane extends IngredientAdditionPane<HopAddition, Hop>
{
	private ComboBox<HopAddition.Form> hopForm;

	public HopAdditionPane(TrackDirty parent, RecipeTreeView model)
	{
		super(parent, model);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ButtonType.DUPLICATE, ButtonType.SUBSTITUTE, ButtonType.DELETE);

		addIngredientComboBox(
			"hop.addition.name",
			HopAddition::getHop,
			HopAddition::setHop,
			IngredientAddition.Type.HOPS);

		hopForm = new ComboBox(
			FXCollections.observableList(
				Arrays.asList(HopAddition.Form.values().clone())));

		this.add(new Label(StringUtils.getUiString("hop.addition.form")));
		this.add(hopForm, "wrap");

		getUnitControlUtils().addWeightUnitControl(
			this,
			"hop.addition.weight",
			HopAddition::getQuantity,
			HopAddition::setQuantity,
			Quantity.Unit.GRAMS);

		getUnitControlUtils().addTimeUnitControl(
			this,
			"hop.addition.time",
			HopAddition::getTime,
			HopAddition::setTime,
			Quantity.Unit.MINUTES);

		// -----

		hopForm.setOnAction(actionEvent ->
		{
			if (!refreshing)
			{
				getAddition().setForm(hopForm.getSelectionModel().getSelectedItem());
			}

			if(detectDirty)
			{
				getParentTrackDirty().setDirty(getStep());
			}
		});
	}

	/*-------------------------------------------------------------------------*/

	@Override
	protected void refreshInternal(HopAddition step, Recipe recipe)
	{
		if (step != null)
		{
			hopForm.getSelectionModel().select(step.getForm());
		}
	}
}
