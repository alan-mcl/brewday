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
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class YeastAdditionDialog extends IngredientAdditionDialog<YeastAddition, Yeast>
{
	private QuantityEditWidget<WeightUnit> weight;
	private QuantityEditWidget<TimeUnit> time;

	/*-------------------------------------------------------------------------*/
	public YeastAdditionDialog(ProcessStep step, YeastAddition addition)
	{
		super(JfxUi.yeastIcon, "common.add.yeast", step);

		if (addition != null)
		{
			weight.refresh(addition.getQuantity());
			time.refresh(addition.getTime());
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.YEAST;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		IngredientAddition.Type ingType = IngredientAddition.Type.YEAST;
		Quantity.Unit weightUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep().getType(), ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep().getType(), ingType);

		weight = new QuantityEditWidget<>(weightUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(weight, "wrap");

		time = new QuantityEditWidget<>(timeUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.time")));
		pane.add(time, "wrap");
	}

	/*-------------------------------------------------------------------------*/
	protected YeastAddition createIngredientAddition(
		Yeast selectedItem)
	{
		return new YeastAddition(selectedItem, weight.getQuantity(), time.getQuantity());
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Yeast> getReferenceIngredients()
	{
		return Database.getInstance().getYeasts();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Yeast, String>[] getColumns()
	{
		return new TableColumn[]
			{
				getPropertyValueTableColumn("yeast.name", "name"),
				getPropertyValueTableColumn("yeast.laboratory", "laboratory"),
				getPropertyValueTableColumn("yeast.product.id", "productId"),
				getPropertyValueTableColumn("yeast.type", "type"),
				getQuantityPropertyValueCol("yeast.attenuation", Yeast::getAttenuation, Quantity.Unit.PERCENTAGE_DISPLAY),
				getPropertyValueTableColumn("yeast.flocculation", "flocculation"),
				getPropertyValueTableColumn("yeast.recommended.styles", "recommendedStyles"),
			};
	}
}