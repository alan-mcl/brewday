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
public class HopAdditionList extends Volume
{
	private String name;

	private List<HopAddition> ingredients;

	public HopAdditionList(String name, HopAddition... ingredients)
	{
		super(Type.HOPS);
		this.name = name;
		this.ingredients = new ArrayList<HopAddition>();
		Collections.addAll(this.ingredients, ingredients);
	}

	public List<HopAddition> getIngredients()
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

	public void setIngredients(List<HopAddition> ingredients)
	{
		this.ingredients = ingredients;
	}

	@Override
	public String describe()
	{
		return String.format("Hops: '%s'", name);
	}
}
