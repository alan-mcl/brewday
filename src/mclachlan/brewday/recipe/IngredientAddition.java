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

public abstract class IngredientAddition
{
	/** weight of this addition in g */
	private double weight;

	/** time, depends on the step type */
	private double time;

	/*-------------------------------------------------------------------------*/
	public double getTime()
	{
		return time;
	}

	public void setTime(double time)
	{
		this.time = time;
	}

	public double getWeight()
	{
		return weight;
	}

	public void setWeight(double weight)
	{
		this.weight = weight;
	}

	public abstract Type getType();

	public abstract String getName();

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
