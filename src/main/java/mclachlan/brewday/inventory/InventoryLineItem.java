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
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class InventoryLineItem implements V2DataObject
{
	/** unique ID */
	private String id;

	/** unique name of the ingredient */
	private String ingredient;

	/** type of this ingredient */
	private IngredientAddition.Type type;

	/**
	 * Amount of the ingredient, unit varies by type.
	 */
	private ArbitraryPhysicalQuantity amount;

	/**
	 * Price of this item, in minor denomination per unit of amount.
	 */
	private int price;

	/*-------------------------------------------------------------------------*/
	public InventoryLineItem()
	{
	}

	/*-------------------------------------------------------------------------*/
	public InventoryLineItem(
		String id,
		String ingredient,
		IngredientAddition.Type type,
		ArbitraryPhysicalQuantity amount,
		int price)
	{
		this.id = id;
		this.ingredient = ingredient;
		this.type = type;
		this.amount = amount;
		this.price = price;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String getName()
	{
		return id;
	}

	@Override
	public void setName(String newName)
	{
		this.id = newName;
	}

	/*-------------------------------------------------------------------------*/

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getIngredient()
	{
		return ingredient;
	}

	public void setIngredient(String ingredient)
	{
		this.ingredient = ingredient;
	}

	public ArbitraryPhysicalQuantity getAmount()
	{
		return amount;
	}

	public void setAmount(ArbitraryPhysicalQuantity amount)
	{
		this.amount = amount;
	}

	public IngredientAddition.Type getType()
	{
		return type;
	}

	public void setType(IngredientAddition.Type type)
	{
		this.type = type;
	}

	public int getPrice()
	{
		return price;
	}

	public void setPrice(int price)
	{
		this.price = price;
	}
}
