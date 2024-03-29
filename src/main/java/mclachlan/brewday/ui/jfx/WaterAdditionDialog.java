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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import mclachlan.brewday.Settings;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class WaterAdditionDialog extends IngredientAdditionDialog<WaterAddition, Water>
{
	private QuantityEditWidget<TemperatureUnit> temperature;
	private QuantitySelectAndEditWidget quantity;
	private QuantityEditWidget<TimeUnit> time;

	public WaterAdditionDialog(ProcessStep step, WaterAddition addition, boolean captureTimeAndTemp)
	{
		super(Icons.waterIcon, "common.add.water", step, captureTimeAndTemp);

		if (addition != null)
		{
			quantity.refresh(addition.getQuantity(), addition.getUnit(), addition.getAdditionQuantityType());
			if (captureTimeAndTemp)
			{
				temperature.refresh(addition.getTemperature());
				time.refresh(addition.getTime());
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.WATER;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		Quantity.Unit tempUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TEMPERATURE, getStep(), IngredientAddition.Type.WATER);
		Quantity.Unit volUnit = settings.getUnitForStepAndIngredient(Quantity.Type.VOLUME, getStep(), IngredientAddition.Type.WATER);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(), IngredientAddition.Type.WATER);

		quantity = new QuantitySelectAndEditWidget(volUnit, Quantity.Type.VOLUME);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(quantity, "wrap");

		if (isCaptureTime())
		{
			time = new QuantityEditWidget<>(timeUnit);
			pane.add(new Label(StringUtils.getUiString("recipe.time")));
			pane.add(time, "wrap");

			temperature = new QuantityEditWidget<>(tempUnit);
			pane.add(new Label(StringUtils.getUiString("water.addition.temperature")));
			pane.add(temperature, "wrap");
		}
	}

	/*-------------------------------------------------------------------------*/
	protected WaterAddition createIngredientAddition(
		Water selectedItem)
	{
		return new WaterAddition(
			selectedItem,
			quantity.getQuantity(),
			quantity.getUnit(),
			isCaptureTime() ? temperature.getQuantity() : null,
			isCaptureTime() ? time.getQuantity() : null);
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Water> getReferenceIngredients()
	{
		return Database.getInstance().getWaters();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Water, String>[] getColumns(TableView<Water> tableView)
	{
		TableColumn[] result = {
			getTableBuilder().getIconColumn(water -> Icons.waterIcon),
			getTableBuilder().getStringPropertyValueCol("water.name", "name"),
			getTableBuilder().getQuantityPropertyValueCol("water.calcium.abbr", Water::getCalcium, Quantity.Unit.PPM),
			getTableBuilder().getQuantityPropertyValueCol("water.bicarbonate.abbr", Water::getBicarbonate, Quantity.Unit.PPM),
			getTableBuilder().getQuantityPropertyValueCol("water.sulfate.abbr", Water::getSulfate, Quantity.Unit.PPM),
			getTableBuilder().getQuantityPropertyValueCol("water.chloride.abbr", Water::getChloride, Quantity.Unit.PPM),
			getTableBuilder().getQuantityPropertyValueCol("water.alkalinity", Water::getAlkalinity, Quantity.Unit.PPM),
			getTableBuilder().getQuantityPropertyValueCol("water.ra", Water::getResidualAlkalinity, Quantity.Unit.PPM),
			getAmountInInventoryCol("ingredient.addition.amount.in.inventory"),
		};

		tableView.getSelectionModel().selectedItemProperty().addListener(
			(observableValue, oldSel, newSel) ->
			{
				if (oldSel == null)
				{
					quantity.refresh(0);
				}
			}
		);

		return result;
	}
}