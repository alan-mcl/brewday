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

package mclachlan.brewday.process;

import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ingredients.Water;

/**
 *
 */
public class MashVolume implements Volume
{
	private String name;

	/** volume in ml */
	private double volume;

	/** temp in deg C */
	private double temperature;

	/** grains in the mash */
	private IngredientAddition<FermentableAddition> ingredientAddition;

	/** water in the mash */
	private Water water;

	/** gravity in GU */
	private double gravity;

	/** colour in SRM */
	private double colour;

	public MashVolume(
		double volume,
		IngredientAddition<FermentableAddition> ingredientAddition,
		Water water,
		double temperature,
		double gravity,
		double colour)
	{
		this.temperature = temperature;
		this.volume = volume;
		this.ingredientAddition = ingredientAddition;
		this.water = water;
		this.gravity = gravity;
		this.colour = colour;
	}

	public IngredientAddition<FermentableAddition> getIngredientAddition()
	{
		return ingredientAddition;
	}

	public Water getWater()
	{
		return water;
	}

	public double getGravity()
	{
		return gravity;
	}

	public double getColour()
	{
		return colour;
	}

	public void setColour(double colour)
	{
		this.colour = colour;
	}

	public double getVolume()
	{
		return volume;
	}

	public double getTemperature()
	{
		return temperature;
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

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("MashVolume{");
		sb.append("volume=").append(getVolume());
		sb.append(", temp=").append(getTemperature());
		sb.append(", grainBill=").append(ingredientAddition);
		sb.append(", water=").append(water);
		sb.append(", gravity=").append(gravity);
		sb.append(", colour=").append(colour);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public String describe()
	{
		return String.format("Mash: '%s'", name);
	}
}
