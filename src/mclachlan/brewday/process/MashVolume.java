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

import mclachlan.brewday.recipe.FermentableAdditionList;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class MashVolume extends Volume
{
	private String name;

	/** volume in ml */
	private double volume;

	/** temp in deg C */
	private double temperature;

	/** grains in the mash */
	private FermentableAdditionList fermentables;

	/** water in the mash */
	private WaterAddition water;

	/** gravity in GU */
	private double gravity;

	/** colour in SRM */
	private double colour;

	public MashVolume(
		double volume,
		FermentableAdditionList fermentables,
		WaterAddition water,
		double temperature,
		double gravity,
		double colour)
	{
		super(Type.MASH);
		this.temperature = temperature;
		this.volume = volume;
		this.fermentables = fermentables;
		this.water = water;
		this.gravity = gravity;
		this.colour = colour;
	}

	public FermentableAdditionList getFermentables()
	{
		return fermentables;
	}

	public WaterAddition getWater()
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
	public String describe()
	{
		return String.format(
			"Type: '%s'\n" +
			"Temp: %.2fC\n" +
			"Volume: %.1fl\n" +
			"Fermentables in: %s\n" +
			"Water in: %s\n" +
			"Gravity: %.1f\n" +
			"Colour: %.1f SRM",
			getType().toString(), temperature, volume/1000, fermentables.getName(), water.getName(),
			1000+gravity, colour);
	}
}
