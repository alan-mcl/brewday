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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class YeastAdditionList extends Volume
{
	private String name;

	private List<YeastAddition> ingredients;

	public YeastAdditionList()
	{
	}

	public YeastAdditionList(String name, YeastAddition... ingredients)
	{
		super(Type.YEAST);
		this.name = name;
		this.ingredients = new ArrayList<YeastAddition>();
		Collections.addAll(this.ingredients, ingredients);
	}

	public List<YeastAddition> getIngredients()
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

	public void setIngredients(List<YeastAddition> ingredients)
	{
		this.ingredients = ingredients;
	}

	@JsonIgnore
	public double getCombinedWeight()
	{
		double result = 0D;
		for (YeastAddition f : getIngredients())
		{
			result += f.getWeight();
		}
		return result;
	}

	@Override
	public String describe()
	{
		return String.format("Yeast: '%s'", name);
	}

	@Override
	public boolean contains(IngredientAddition ingredient)
	{
		return ingredients.contains(ingredient);
	}
}
