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

package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class InventoryLineItemSerialiser implements V2SerialiserMap<InventoryLineItem>
{
	QuantitySerialiser quantitySerialiser = new QuantitySerialiser(false);

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(InventoryLineItem inventoryLineItem)
	{
		Map result = new HashMap();

		result.put("ingredient", inventoryLineItem.getName());
		result.put("quantity", quantitySerialiser.toMap(inventoryLineItem.getQuantity()));
		result.put("type", inventoryLineItem.getType().name());
		result.put("unit", inventoryLineItem.getUnit().name());

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public InventoryLineItem fromMap(Map<String, ?> map)
	{
		String name = (String)map.get("ingredient");
		IngredientAddition.Type type = IngredientAddition.Type.valueOf((String)map.get("type"));
		Quantity quantity = quantitySerialiser.fromMap((Map<String, ?>)map.get("quantity"));
		Quantity.Unit unit;

		if (map.get("unit") == null)
		{
			unit = quantity.getUnit();
		}
		else
		{
			unit = Quantity.Unit.valueOf((String)map.get("unit"));
		}

		InventoryLineItem result = new InventoryLineItem(name, type, quantity, unit);

		return result;
	}
}
