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

import mclachlan.brewday.math.*;

/**
 *
 */
public abstract class FluidVolume extends Volume
{
	private String name;
	private VolumeUnit volume;
	private TemperatureUnit temperature;
	private DensityUnit gravity;
	private ColourUnit colour;
	private BitternessUnit bitterness;
	/** ABV in % */
	private double abv;

	/*-------------------------------------------------------------------------*/
	protected FluidVolume()
	{
	}

	/*-------------------------------------------------------------------------*/
	public FluidVolume(
		Type type,
		TemperatureUnit temperature,
		ColourUnit colour,
		BitternessUnit bitterness,
		DensityUnit gravity,
		VolumeUnit volume,
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

	public DensityUnit getGravity()
	{
		return gravity;
	}

	public ColourUnit getColour()
	{
		return colour;
	}

	public BitternessUnit getBitterness()
	{
		return bitterness;
	}

	public VolumeUnit getVolume()
	{
		return volume;
	}

	public TemperatureUnit getTemperature()
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

	public void setVolume(VolumeUnit volume)
	{
		this.volume = volume;
	}

	public void setTemperature(TemperatureUnit temperature)
	{
		this.temperature = temperature;
	}

	public void setGravity(DensityUnit gravity)
	{
		this.gravity = gravity;
	}

	public void setColour(ColourUnit colour)
	{
		this.colour = colour;
	}

	public void setBitterness(BitternessUnit bitterness)
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
