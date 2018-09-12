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

package mclachlan.brewday.ingredients;

import mclachlan.brewday.process.Volume;

/**
 *
 */
public class Water implements Volume
{
	private String name;
	private double volume;
	private double temperature;

	public Water(double volume, double temperature)
	{
		this.volume = volume;
		this.temperature = temperature;
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
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("Water{");
		sb.append("volume=").append(volume);
		sb.append(", temperature=").append(temperature);
		sb.append('}');
		return sb.toString();
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
}
