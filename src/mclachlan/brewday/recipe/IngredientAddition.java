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

import java.util.*;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class IngredientAddition<T> implements Volume
{
	private String name;

	private List<T> ingredients;

	public IngredientAddition(T... ingredients)
	{
		this.ingredients = new ArrayList<T>();
		Collections.addAll(this.ingredients, ingredients);
	}

	public List<T> getIngredients()
	{
		return ingredients;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	public void setIngredients(List<T> ingredients)
	{
		this.ingredients = ingredients;
	}

	@Override
	public String describe()
	{
		return String.format("Ingredient Addition: '%s'", name);
	}
}
