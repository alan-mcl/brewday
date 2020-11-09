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

package mclachlan.brewday.inventory;

import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class InventoryLineItem implements V2DataObject
{
	/** unique name of the ingredient */
	private String ingredient;

	/** type of this ingredient */
	private IngredientAddition.Type type;

	/**
	 * Amount of the ingredient, unit varies by type.
	 */
	private Quantity quantity;

	/**
	 * Unit to express this ingredient in.
	 */
	private Quantity.Unit unit;

	/*-------------------------------------------------------------------------*/
	public InventoryLineItem()
	{
	}

	/*-------------------------------------------------------------------------*/
	public InventoryLineItem(
		String ingredient,
		IngredientAddition.Type type,
		Quantity quantity,
		Quantity.Unit unit)
	{
		this.ingredient = ingredient;
		this.type = type;
		this.quantity = quantity;
		this.unit = unit;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String getName()
	{
		return ingredient;
	}

	@Override
	public void setName(String newName)
	{
		this.ingredient = newName;
	}

	/*-------------------------------------------------------------------------*/

	public Quantity getQuantity()
	{
		return quantity;
	}

	public void setQuantity(Quantity quantity)
	{
		this.quantity = quantity;
	}

	public Quantity.Unit getUnit()
	{
		return unit;
	}

	public void setUnit(Quantity.Unit unit)
	{
		this.unit = unit;
	}



	public IngredientAddition.Type getType()
	{
		return type;
	}

	public void setType(IngredientAddition.Type type)
	{
		this.type = type;
	}
}
