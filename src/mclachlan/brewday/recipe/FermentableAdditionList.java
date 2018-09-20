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
public class FermentableAdditionList extends Volume
{
	private String name;

	private List<FermentableAddition> ingredients;

	public FermentableAdditionList(String name, FermentableAddition... ingredients)
	{
		super(Type.FERMENTABLES);
		this.name = name;
		this.ingredients = new ArrayList<FermentableAddition>();
		Collections.addAll(this.ingredients, ingredients);
	}

	public List<FermentableAddition> getIngredients()
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

	public void setIngredients(List<FermentableAddition> ingredients)
	{
		this.ingredients = ingredients;
	}

	@Override
	public String describe()
	{
		return String.format("Fermentables: '%s'", name);
	}

	public double getCombinedWeight()
	{
		double grainWeight = 0D;
		for (FermentableAddition f : getIngredients())
		{
			grainWeight += f.getWeight();
		}
		return grainWeight;
	}
}