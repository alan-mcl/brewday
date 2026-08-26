/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui;

import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.util.StringUtils;

/**
 * Cosmetic quantity labels for read-only UI (recipe tree, inventory table,
 * generated documents). Does not change persisted units on domain objects.
 */
public final class UiQuantityDisplay
{
	private UiQuantityDisplay()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static Settings currentSettings()
	{
		return Database.getInstance().getSettings();
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Format a quantity using UI display-unit preferences.
	 */
	public static String describe(Quantity quantity)
	{
		if (quantity == null)
		{
			return "-";
		}
		UiUnitPreferences prefs = UiUnitPreferences.from(currentSettings());
		Quantity.Unit unit = prefs.displayUnitFor(quantity);
		if (unit == null)
		{
			unit = quantity.getUnit();
		}
		return quantity.describe(unit);
	}

	/*-------------------------------------------------------------------------*/
	public static String describeAdditionQuantity(IngredientAddition addition)
	{
		if (addition == null || addition.getQuantity() == null)
		{
			return "";
		}
		Quantity.Unit unit = currentSettings().getUnitForInventory(
			addition.getQuantity().getType(),
			addition.getType());
		return addition.getQuantity().describe(unit);
	}

	/*-------------------------------------------------------------------------*/
	public static String describeAdditionQuantity(
		IngredientAddition addition,
		ProcessStep step)
	{
		if (addition == null || addition.getQuantity() == null)
		{
			return "";
		}
		Quantity.Unit unit = currentSettings().getUnitForStepAndIngredient(
			addition.getQuantity().getType(),
			step,
			addition.getType());
		return addition.getQuantity().describe(unit);
	}

	/*-------------------------------------------------------------------------*/
	public static String formatInventoryQuantity(InventoryLineItem item, Settings settings)
	{
		if (item == null || item.getQuantity() == null)
		{
			return "";
		}
		Quantity.Unit unit = settings.getUnitForInventory(
			item.getQuantity().getType(),
			item.getType());
		return item.getQuantity().describe(unit);
	}

	/*-------------------------------------------------------------------------*/
	public static String formatAdditionTreeLabel(
		IngredientAddition addition,
		ProcessStep step,
		Settings settings)
	{
		if (addition == null)
		{
			return "";
		}
		Quantity.Unit qtyUnit = settings.getUnitForStepAndIngredient(
			addition.getQuantity().getType(),
			step,
			addition.getType());
		String qty = addition.getQuantity().describe(qtyUnit);

		if (addition instanceof YeastAddition yeast)
		{
			return StringUtils.getUiString("yeast.addition.toString",
				yeast.getName(),
				qty);
		}
		if (addition instanceof FermentableAddition fermentable)
		{
			return StringUtils.getUiString("fermentable.addition.toString",
				fermentable.getName(),
				qty,
				additionTimeValue(addition, step, settings));
		}
		if (addition instanceof HopAddition hop)
		{
			return StringUtils.getUiString("hop.addition.toString",
				hop.getName(),
				qty,
				additionTimeValue(addition, step, settings));
		}
		if (addition instanceof WaterAddition water)
		{
			return StringUtils.getUiString("water.addition.toString",
				water.getName(),
				qty,
				additionTimeValue(addition, step, settings));
		}
		if (addition instanceof MiscAddition misc)
		{
			return StringUtils.getUiString("misc.addition.toString",
				misc.getName(),
				qty,
				additionTimeValue(addition, step, settings));
		}
		return addition.getName() + ", " + qty;
	}

	/*-------------------------------------------------------------------------*/
	private static double additionTimeValue(
		IngredientAddition addition,
		ProcessStep step,
		Settings settings)
	{
		if (addition.getTime() == null)
		{
			return 0D;
		}
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(
			Quantity.Type.TIME,
			step,
			addition.getType());
		return addition.getTime().get(timeUnit);
	}
}
