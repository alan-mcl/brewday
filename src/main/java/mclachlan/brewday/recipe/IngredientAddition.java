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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.recipe;

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;

public abstract class IngredientAddition
{
	private TimeUnit time;

	/*-------------------------------------------------------------------------*/
	public TimeUnit getTime()
	{
		return time;
	}

	public void setTime(TimeUnit time)
	{
		this.time = time;
	}

	public abstract Type getType();

	public abstract String getName();

	public abstract Quantity getQuantity();
	public abstract void setQuantity(Quantity weight);

	/**
	 * @return
	 * 	a deep clone of this ingredient addition
	 */
	public abstract IngredientAddition clone();

	public static enum Type
	{
		FERMENTABLES(1),
		HOPS(2),
		WATER(3),
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
	}
}