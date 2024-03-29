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
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class HopAdditionDialog extends IngredientAdditionDialog<HopAddition, Hop>
{
	private QuantitySelectAndEditWidget quantity;
	private QuantityEditWidget<TimeUnit> time;

	/*-------------------------------------------------------------------------*/
	public HopAdditionDialog(ProcessStep step, HopAddition addition, boolean captureTime)
	{
		super(Icons.hopsIcon, "common.add.hop", step, captureTime);

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
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.HOPS;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		IngredientAddition.Type ingType = IngredientAddition.Type.HOPS;
		Quantity.Unit quantityUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep(), ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(), ingType);

		quantity = new QuantitySelectAndEditWidget(quantityUnit, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
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
	protected HopAddition createIngredientAddition(
		Hop selectedItem)
	{
		return new HopAddition(
			selectedItem,
			quantity.getQuantity(),
			quantity.getUnit(),
			isCaptureTime() ? time.getQuantity() : null);
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Hop> getReferenceIngredients()
	{
		return Database.getInstance().getHops();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Hop, String>[] getColumns(TableView<Hop> tableView)
	{
		TableColumn[] result = {
			getTableBuilder().getIconColumn(hop -> Icons.hopsIcon),
			getTableBuilder().getStringPropertyValueCol("hop.name", "name"),
			getTableBuilder().getStringPropertyValueCol("hop.type", "type"),
			getTableBuilder().getStringPropertyValueCol("hop.origin", "origin"),
			getTableBuilder().getQuantityPropertyValueCol("hop.alpha", Hop::getAlphaAcid, Quantity.Unit.PERCENTAGE_DISPLAY),
			getTableBuilder().getQuantityPropertyValueCol("hop.beta", Hop::getBetaAcid, Quantity.Unit.PERCENTAGE_DISPLAY),
			getAmountInInventoryCol("ingredient.addition.amount.in.inventory"),
		};

		tableView.getSelectionModel().selectedItemProperty().addListener(
			(observableValue, oldSel, newSel) ->
			{
				if (oldSel == null || newSel.getForm() != oldSel.getForm())
				{
					quantity.refresh(
						Quantity.parseQuantity("0", newSel.getForm().getDefaultUnit()),
						newSel.getForm().getDefaultUnit(),
						newSel.getForm().getQuantityType());
				}
			}
		);


		return result;
	}
}