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

package mclachlan.brewday.recipe;

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;

public abstract class IngredientAddition implements V2DataObject
{
	private Quantity quantity;
	private Quantity.Unit unit;
	private TimeUnit time;

	/*-------------------------------------------------------------------------*/
	public TimeUnit getTime()
	{
		return time;
	}

	public void setTime(Quantity time)
	{
		this.time = (TimeUnit)time;
	}

	public abstract Type getType();

	public abstract String getName();

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

	public abstract Quantity.Type getAdditionQuantityType();

	/**
	 * @return
	 * 	a deep clone of this ingredient addition
	 */
	public abstract IngredientAddition clone();

	/*-------------------------------------------------------------------------*/
	public String getInventoryId()
	{
		return InventoryLineItem.getUniqueId(this.getName(), this.getType());
	}

	/*-------------------------------------------------------------------------*/
	public String describe()
	{
		double v = getQuantity().get(getUnit());
		return StringUtils.format(v, getUnit());
	}

	/*-------------------------------------------------------------------------*/
	public enum Type
	{
		FERMENTABLES(2),
		HOPS(3),
		WATER(1),
		YEAST(4),
		MISC(5);

		private int sortOrder;

		Type(int sortOrder)
		{
			this.sortOrder = sortOrder;
		}

		public int getSortOrder()
		{
			return sortOrder;
		}


		@Override
		public String toString()
		{
			return StringUtils.getUiString("ingredient.addition.type."+name());
		}
	}
}
