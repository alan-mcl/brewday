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

import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import mclachlan.brewday.Settings;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.UiUtils;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class FermentableAdditionDialog extends IngredientAdditionDialog<FermentableAddition, Fermentable>
{
	private QuantitySelectAndEditWidget quantity;
	private QuantityEditWidget<TimeUnit> time;

	/*-------------------------------------------------------------------------*/
	public FermentableAdditionDialog(ProcessStep step, FermentableAddition addition, boolean captureTime)
	{
		super(Icons.fermentableIconGeneric, "common.add.fermentable", step, captureTime);

		if (addition != null)
		{
			quantity.refresh(addition.getQuantity(), addition.getUnit(), addition.getAdditionQuantityType());
			if (captureTime)
			{
				time.refresh(addition.getTime());
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected boolean mandatoryInputProvided()
	{
		return quantity.getQuantity() != null && (!isCaptureTime() || time.getQuantity() != null);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.FERMENTABLES;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		Quantity.Unit weightUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep(), IngredientAddition.Type.FERMENTABLES);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(), IngredientAddition.Type.FERMENTABLES);

		quantity = new QuantitySelectAndEditWidget(weightUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(quantity, "wrap");

		if (isCaptureTime())
		{
			time = new QuantityEditWidget<>(timeUnit);
			pane.add(new Label(StringUtils.getUiString("recipe.time")));
			pane.add(time, "wrap");
		}
	}

	/*-------------------------------------------------------------------------*/
	protected FermentableAddition createIngredientAddition(
		Fermentable selectedItem)
	{
		return new FermentableAddition(
			selectedItem, quantity.getQuantity(), quantity.getUnit(),
			isCaptureTime() ? time.getQuantity() : null);
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Fermentable> getReferenceIngredients()
	{
		return Database.getInstance().getFermentables();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Fermentable, String>[] getColumns(
		TableView<Fermentable> tableView)
	{
		TableColumn[] result = {
			getTableBuilder().getIconColumn(UiUtils::getFermentableIcon),
			getTableBuilder().getStringPropertyValueCol("fermentable.name", "name"),
			getTableBuilder().getStringPropertyValueCol("fermentable.type", "type"),
			getTableBuilder().getStringPropertyValueCol("fermentable.origin", "origin"),
			getTableBuilder().getQuantityPropertyValueCol("fermentable.yield", Fermentable::getYield, Quantity.Unit.PERCENTAGE_DISPLAY),
			getTableBuilder().getQuantityPropertyValueCol("fermentable.colour", Fermentable::getColour, Quantity.Unit.SRM),
			getAmountInInventoryCol("ingredient.addition.amount.in.inventory"),
		};

		/*
		The intention here was to zero the amount if it no longer makes sense.
		This has bugs right now in edge cases like substitution and filtering.
		Leaving this out for now, may revisit.

		tableView.getSelectionModel().selectedItemProperty().addListener(
			(observableValue, oldSel, newSel) ->
			{
				if (oldSel == null || newSel.getType() != oldSel.getType())
				{
					quantity.refresh(
						Quantity.parseQuantity("0", newSel.getType().getDefaultUnit()),
						newSel.getType().getDefaultUnit(),
						newSel.getType().getQuantityType());
				}
			}
		);*/

		return result;
	}
}