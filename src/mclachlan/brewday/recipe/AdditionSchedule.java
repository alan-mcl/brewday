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

/**
 * Represents a schedule of ingredient additions within a process step.
 */
public class AdditionSchedule
{
	/**
	 * Time of this addition, measured from the end of the process
	 * step involved (i.e. following the hop boil addition convention).
	 * <p>
	 * The unit will vary depending on the type of addition (for eg minutes for
	 * a boil hop addition, days for a fermentation yeast addition)
	 * <p>
	 * Examples:
	 * <ul>
	 *    <li>hop addition at the start of a 60min boil: time=60
	 *    <li>grain addition 10 min before the end of a mash: time=10
	 * </ul>
	 */
	private double time;

	/**
	 * The volume name of the ingredient addition at this time.
	 */
	private String ingredientAddition;

	public AdditionSchedule(String ingredientAddition, double time)
	{
		this.time = time;
		this.ingredientAddition = ingredientAddition;
	}

	public double getTime()
	{
		return time;
	}

	public void setTime(double time)
	{
		this.time = time;
	}

	public String getIngredientAddition()
	{
		return ingredientAddition;
	}

	public void setIngredientAddition(String ingredientAddition)
	{
		this.ingredientAddition = ingredientAddition;
	}
}
