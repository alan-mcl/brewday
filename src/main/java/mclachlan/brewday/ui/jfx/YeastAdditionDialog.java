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
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class YeastAdditionDialog extends IngredientAdditionDialog<YeastAddition, Yeast>
{
	private QuantitySelectAndEditWidget quantity;
	private QuantityEditWidget<TimeUnit> time;

	/*-------------------------------------------------------------------------*/
	public YeastAdditionDialog(ProcessStep step, YeastAddition addition, boolean captureTime)
	{
		super(Icons.yeastIcon, "common.add.yeast", step, captureTime);

		if (addition != null)
		{
			quantity.refresh(addition.getQuantity());
			if (captureTime)
			{
				time.refresh(addition.getTime());
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected boolean getFilterPredicate(String searchText, Yeast yeast)
	{
		String s = searchText.toLowerCase();

		return
			yeast.getName().toLowerCase().contains(s) ||
				(yeast.getLaboratory()!= null && yeast.getLaboratory().toLowerCase().contains(s)) ||
				(yeast.getProductId()!= null && yeast.getProductId().toLowerCase().contains(s)) ||
				(yeast.getRecommendedStyles()!=null && yeast.getRecommendedStyles().toLowerCase().contains(s)) ||
				(yeast.getDescription()!=null && yeast.getDescription().toLowerCase().contains(s));
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
		Quantity.Unit weightUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep(), ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(), ingType);

		Quantity.Type[] types = {Quantity.Type.VOLUME, Quantity.Type.WEIGHT};

		quantity = new QuantitySelectAndEditWidget(weightUnit, types);
		pane.add(new Label(StringUtils.getUiString("yeast.weight")));
		pane.add(quantity, "wrap");

		if (isCaptureTime())
		{
			time = new QuantityEditWidget<>(timeUnit);
			pane.add(new Label(StringUtils.getUiString("yeast.time")));
			pane.add(time, "wrap");
		}
	}

	/*-------------------------------------------------------------------------*/
	protected YeastAddition createIngredientAddition(
		Yeast selectedItem)
	{
		return new YeastAddition(selectedItem, quantity.getQuantity(), quantity.getUnit(),
			isCaptureTime() ? time.getQuantity() : null);
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
				getTableBuilder().getIconColumn(yeast -> Icons.yeastIcon),
				getTableBuilder().getStringPropertyValueCol("yeast.name", "name"),
				getTableBuilder().getStringPropertyValueCol("yeast.laboratory", "laboratory"),
				getTableBuilder().getStringPropertyValueCol("yeast.product.id", "productId"),
				getTableBuilder().getStringPropertyValueCol("yeast.type", "type"),
				getTableBuilder().getQuantityPropertyValueCol("yeast.attenuation", Yeast::getAttenuation, Quantity.Unit.PERCENTAGE_DISPLAY),
				getTableBuilder().getStringPropertyValueCol("yeast.flocculation", "flocculation"),
				getAmountInInventoryCol("ingredient.addition.amount.in.inventory"),
			};
	}
}