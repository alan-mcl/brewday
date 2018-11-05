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
 *
 */
public class RecipeLineItem
{
	private double time;
	private IngredientAddition ingredient;

	public RecipeLineItem()
	{
	}

	public RecipeLineItem(
		double time,
		IngredientAddition ingredient)
	{
		this.time = time;
		this.ingredient = ingredient;
	}

	public double getTime()
	{
		return time;
	}

	public void setTime(double time)
	{
		this.time = time;
	}

	public IngredientAddition getIngredient()
	{
		return ingredient;
	}

	public void setIngredient(IngredientAddition ingredient)
	{
		this.ingredient = ingredient;
	}
}
