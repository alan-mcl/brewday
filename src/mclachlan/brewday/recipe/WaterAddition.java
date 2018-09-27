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
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class WaterAddition extends Volume
{
	private String name;

	/** vol in ml */
	private double volume;

	/** temp in C */
	private double temperature;

	public WaterAddition(String name, double volume,
		double temperature)
	{
		super(Type.WATER);
		this.name = name;
		this.volume = volume;
		this.temperature = temperature;
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
	public String describe()
	{
		return String.format("Water: %s, %.1fl at %.1fC", name, volume/1000, temperature);
	}

	public double getVolume()
	{
		return volume;
	}

	public double getTemperature()
	{
		return temperature;
	}

	public void setVolume(double volume)
	{
		this.volume = volume;
	}

	public void setTemperature(Double temperature)
	{
		this.temperature = temperature;
	}

	public WaterAddition combineWith(String name, WaterAddition other)
	{
		return new WaterAddition(
			name,
			this.getVolume()+other.getVolume(),
			Equations.calcNewFluidTemperature(
				this.getVolume(),
				this.getTemperature(),
				other.getVolume(),
				other.getTemperature()));
	}
}
