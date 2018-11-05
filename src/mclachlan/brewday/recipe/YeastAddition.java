/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the yeaste that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.recipe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import mclachlan.brewday.ingredients.Yeast;

/**
 *
 */
public class YeastAddition implements IngredientAddition
{
	private Yeast yeast;

	/** weight in g */
	private double weight;

	public YeastAddition()
	{
	}

	public YeastAddition(Yeast yeast, double weight)
	{
		this.yeast = yeast;
		this.weight = weight;
	}

	public Yeast getYeast()
	{
		return yeast;
	}

	public double getWeight()
	{
		return weight;
	}

	public void setYeast(Yeast yeast)
	{
		this.yeast = yeast;
	}

	public void setWeight(double weight)
	{
		this.weight = weight;
	}

	@Override
	@JsonIgnore
	public String getName()
	{
		return yeast.getName();
	}

	@Override
	@JsonIgnore
	public Type getType()
	{
		return Type.YEAST;
	}
}
