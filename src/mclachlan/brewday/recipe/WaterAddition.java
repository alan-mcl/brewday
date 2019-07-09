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

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class WaterAddition extends IngredientAddition
{
	private String name;

	/** vol in ml */
	// we abuse the "weight" property on the superclass for this.
	// yay for the metric system

	/** temp in C */
	private double temperature;

	/*-------------------------------------------------------------------------*/
	public WaterAddition()
	{
	}

	/*-------------------------------------------------------------------------*/
	public WaterAddition(String name, double volume,
		double temperature, double time)
	{
		this.name = name;
		setWeight(volume);
		setTime(time);
		this.temperature = temperature;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String describe()
	{
		return String.format("Water: %s, %.1fl at %.1fC", name, getWeight()/1000, temperature);
	}

	public boolean contains(IngredientAddition ingredient)
	{
		return ingredient == this;
	}

	public double getVolume()
	{
		return getWeight();
	}

	public double getTemperature()
	{
		return temperature;
	}

	public void setVolume(double volume)
	{
		this.setWeight(volume);
	}

	public void setTemperature(Double temperature)
	{
		this.temperature = temperature;
	}

	public WaterAddition getCombination(String name, WaterAddition other)
	{
		return new WaterAddition(
			name,
			this.getVolume()+other.getVolume(),
			Equations.calcNewFluidTemperature(
				this.getVolume(),
				this.getTemperature(),
				other.getVolume(),
				other.getTemperature()),
			0);
	}

	public void combineWith(WaterAddition other)
	{
		this.setWeight(getWeight() + other.getWeight());
		this.temperature = Equations.calcNewFluidTemperature(
			this.getVolume(),
			this.getTemperature(),
			other.getVolume(),
			other.getTemperature());
	}

	@Override
	public Type getType()
	{
		return Type.WATER;
	}
}
