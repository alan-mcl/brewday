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

import mclachlan.brewday.math.DensityUnit;

/**
 *
 */
public abstract class FluidVolume extends Volume
{
	private String name;
	/** volume in ml */
	private double volume;
	/** temp in deg C */
	private double temperature;
	/** gravity */
	private DensityUnit gravity = new DensityUnit();
	/** colour in SRM */
	private double colour;
	/** bitterness in IBU */
	private double bitterness;
	/** ABV in % */
	private double abv;

	/*-------------------------------------------------------------------------*/
	protected FluidVolume()
	{
	}

	/*-------------------------------------------------------------------------*/
	public FluidVolume(
		Type type,
		double temperature,
		double colour,
		double bitterness,
		DensityUnit gravity,
		double volume,
		double abv)
	{
		super(type);
		this.setTemperature(temperature);
		this.setColour(colour);
		this.setBitterness(bitterness);
		this.setGravity(gravity);
		this.setVolume(volume);
		this.setAbv(abv);
	}

	public FluidVolume(Type type)
	{
		super(type);
	}

	/** gravity */
	public DensityUnit getGravity()
	{
		return gravity;
	}

	/** colour in SRM */
	public double getColour()
	{
		return colour;
	}

	/** bitterness in IBU */
	public double getBitterness()
	{
		return bitterness;
	}

	/** volume in ml */
	public double getVolume()
	{
		return volume;
	}

	/** temp in deg C */
	public double getTemperature()
	{
		return temperature;
	}

	/** ABV in % */
	public double getAbv()
	{
		return abv;
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

	public void setVolume(double volume)
	{
		this.volume = volume;
	}

	public void setTemperature(double temperature)
	{
		this.temperature = temperature;
	}

	public void setGravity(DensityUnit gravity)
	{
		this.gravity = gravity;
	}

	public void setColour(double colour)
	{
		this.colour = colour;
	}

	public void setBitterness(double bitterness)
	{
		this.bitterness = bitterness;
	}

	public void setAbv(double abv)
	{
		this.abv = abv;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("FluidVolume{");
		sb.append("name='").append(name).append('\'');
		sb.append(", volume=").append(volume);
		sb.append(", temperature=").append(temperature);
		sb.append(", gravity=").append(gravity);
		sb.append(", colour=").append(colour);
		sb.append(", bitterness=").append(bitterness);
		sb.append(", abv=").append(abv);
		sb.append('}');
		return sb.toString();
	}
}
