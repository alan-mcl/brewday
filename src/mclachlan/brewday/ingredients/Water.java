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
public class Water extends Volume
{
	private String name;
	private double volume;
	private double temperature;
	private String description;
	private double calcium;
	private double bicarbonate;
	private double sulfate;
	private double chloride;
	private double sodium;
	private double magnesium;
	private double ph;

	public Water(String name, double volume, double temperature)
	{
		super(Type.WATER);
		this.name = name;
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

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}

	public void setCalcium(double calcium)
	{
		this.calcium = calcium;
	}

	public double getCalcium()
	{
		return calcium;
	}

	public void setBicarbonate(double bicarbonate)
	{
		this.bicarbonate = bicarbonate;
	}

	public double getBicarbonate()
	{
		return bicarbonate;
	}

	public void setSulfate(double sulfate)
	{
		this.sulfate = sulfate;
	}

	public double getSulfate()
	{
		return sulfate;
	}

	public void setChloride(double chloride)
	{
		this.chloride = chloride;
	}

	public double getChloride()
	{
		return chloride;
	}

	public void setSodium(double sodium)
	{
		this.sodium = sodium;
	}

	public double getSodium()
	{
		return sodium;
	}

	public void setMagnesium(double magnesium)
	{
		this.magnesium = magnesium;
	}

	public double getMagnesium()
	{
		return magnesium;
	}

	public void setPh(double ph)
	{
		this.ph = ph;
	}

	public double getPh()
	{
		return ph;
	}

	public void setVolume(double v)
	{
		this.volume = v;
	}

	public void setTemperature(Double temperature)
	{
		this.temperature = temperature;
	}
}
