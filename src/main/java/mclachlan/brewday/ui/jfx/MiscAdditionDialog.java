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
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.ui.UiUtils;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class MiscAdditionDialog extends IngredientAdditionDialog<MiscAddition, Misc>
{
	private QuantitySelectAndEditWidget quantity;
	private QuantityEditWidget<TimeUnit> time;

	/*-------------------------------------------------------------------------*/
	public MiscAdditionDialog(ProcessStep step, MiscAddition addition, boolean captureTime)
	{
		super(Icons.miscIconGeneric, "common.add.misc", step, captureTime);

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
		return IngredientAddition.Type.MISC;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		IngredientAddition.Type ingType = IngredientAddition.Type.MISC;
		Quantity.Unit unit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep(), ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(), ingType);

		quantity = new QuantitySelectAndEditWidget(unit, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
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
	protected MiscAddition createIngredientAddition(
		Misc selectedItem)
	{
		return new MiscAddition(selectedItem, quantity.getQuantity(), quantity.getUnit(),
			isCaptureTime() ? time.getQuantity() : null);
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Misc> getReferenceIngredients()
	{
		return Database.getInstance().getMiscs();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Misc, String>[] getColumns(TableView<Misc> tableView)
	{
		TableColumn[] result = {
			getTableBuilder().getIconColumn(UiUtils::getMiscIcon),
			getTableBuilder().getStringPropertyValueCol("misc.name", "name"),
			getTableBuilder().getStringPropertyValueCol("misc.type", "type"),
			getTableBuilder().getStringPropertyValueCol("misc.use", "use"),
			getTableBuilder().getStringPropertyValueCol("misc.usage.recommendation", "usageRecommendation"),
			getAmountInInventoryCol("ingredient.addition.amount.in.inventory"),
		};

		tableView.getSelectionModel().selectedItemProperty().addListener(
			(observableValue, oldSel, newSel) ->
			{
				if (oldSel == null || newSel.getMeasurementType() != oldSel.getMeasurementType())
				{
					quantity.refresh(
						Quantity.parseQuantity("0", newSel.getMeasurementType().getDefaultUnit()),
						newSel.getMeasurementType().getDefaultUnit(),
						newSel.getMeasurementType());
				}
			}
		);

		return result;
	}
}