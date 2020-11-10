
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.inventory;

import java.util.*;
import javafx.scene.image.Image;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.jfx.Icons;
import mclachlan.brewday.ui.jfx.JfxUi;

/**
 *
 */
public class InventoryFacade
{

	/*-------------------------------------------------------------------------*/
	public static List<InventoryLineItemDelta> getInventoryDelta(
		String recipeName,
		boolean remove)
	{
		List<InventoryLineItemDelta> result = new ArrayList<>();

		Recipe recipe = Database.getInstance().getRecipes().get(recipeName);

		// de-duplicate the recipe additions
		Map<String, InventoryLineItemDelta> dedupe = new HashMap<>();
		for (IngredientAddition ia : recipe.getIngredients())
		{
			String inventoryId = ia.getInventoryId();

			InventoryLineItem ili = Database.getInstance().getInventory().get(inventoryId);
			Quantity.Unit unit = ili == null ? ia.getUnit() : ili.getUnit();

			if (dedupe.containsKey(inventoryId))
			{
				// add to the current delta

				InventoryLineItemDelta ilid = dedupe.get(inventoryId);

				double c = ilid.getDelta().get(unit) + ia.getQuantity().get(unit);

				Quantity consumed = Quantity.parseQuantity("" + c, unit);
				ilid.setDelta(consumed);
			}
			else
			{
				// add a new delta

				Quantity inInv = null;
				if (ili != null)
				{
					inInv = ili.getQuantity();
				}
				else
				{
					inInv = Quantity.parseQuantity("0", ia.getUnit());
				}
				double c = ia.getQuantity().get(unit);

				Quantity consumed = Quantity.parseQuantity("" + c, unit);

				InventoryLineItemDelta ilid = new InventoryLineItemDelta(
					ia.getName(), ia.getType(), inInv, consumed, unit);
				result.add(ilid);

				dedupe.put(inventoryId, ilid);
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static void consumeInventory(
		List<InventoryLineItemDelta> inventoryDelta)
	{
		Map<String, InventoryLineItem> inventory = Database.getInstance().getInventory();

		for (InventoryLineItemDelta ilid : inventoryDelta)
		{
			InventoryLineItem item = inventory.get(ilid.getInventoryId());
			if (item != null)
			{
				Quantity.Unit unit = item.getQuantity().getUnit();
				double c = item.getQuantity().get() - ilid.getDelta().get();
				item.setQuantity(Quantity.parseQuantity("" + c, unit));

				if (item.getQuantity().get() <= 0)
				{
					inventory.remove(ilid.getInventoryId());
				}

				JfxUi.getInstance().setDirty(JfxUi.INVENTORY);
				JfxUi.getInstance().setDirty(item);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void restoreInventory(
		List<InventoryLineItemDelta> inventoryDelta)
	{
		Map<String, InventoryLineItem> inventory = Database.getInstance().getInventory();

		for (InventoryLineItemDelta ilid : inventoryDelta)
		{
			InventoryLineItem item = inventory.get(ilid.getInventoryId());
			if (item == null)
			{
				item = new InventoryLineItem(ilid.ingredient, ilid.type, ilid.delta, ilid.unit);
				inventory.put(ilid.getInventoryId(), item);
			}
			else
			{
				Quantity.Unit unit = item.getQuantity().getUnit();
				double c = item.getQuantity().get() + ilid.getDelta().get();
				item.setQuantity(Quantity.parseQuantity("" + c, unit));
			}

			JfxUi.getInstance().setDirty(JfxUi.INVENTORY);
			JfxUi.getInstance().setDirty(item);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static class InventoryLineItemDelta
	{
		private final String ingredient;
		private final IngredientAddition.Type type;
		private final Quantity inInventory;
		private final Quantity.Unit unit;
		private Quantity delta;

		public InventoryLineItemDelta(
			String ingredient,
			IngredientAddition.Type type,
			Quantity inInventory,
			Quantity delta,
			Quantity.Unit unit)
		{
			this.ingredient = ingredient;
			this.type = type;
			this.inInventory = inInventory;
			this.delta = delta;
			this.unit = unit;
		}

		public String getIngredient()
		{
			return ingredient;
		}

		public IngredientAddition.Type getType()
		{
			return type;
		}

		public Quantity getInInventory()
		{
			return inInventory;
		}

		public Quantity getDelta()
		{
			return delta;
		}

		public void setDelta(Quantity delta)
		{
			this.delta = delta;
		}

		public Quantity.Unit getUnit()
		{
			return unit;
		}

		public String getInventoryId()
		{
			return InventoryLineItem.getUniqueId(ingredient, type);
		}

		public Image getIcon()
		{
			switch (type)
			{
				case FERMENTABLES:
					return UiUtils.getFermentableIcon(Database.getInstance().getFermentables().get(ingredient));
				case HOPS:
					return Icons.hopsIcon;
				case WATER:
					return Icons.waterIcon;
				case YEAST:
					return Icons.yeastIcon;
				case MISC:
					return UiUtils.getMiscIcon(Database.getInstance().getMiscs().get(ingredient));
				default:
					throw new BrewdayException("Unexpected value: " + type);
			}
		}
	}
}
