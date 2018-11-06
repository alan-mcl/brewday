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

import java.util.*;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.recipe.IngredientAddition;
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
	private List<IngredientAddition> fermentables;

	/** water in the mash */
	private WaterAddition water;

	/** gravity in GU */
	private DensityUnit gravity = new DensityUnit();

	/** colour in SRM */
	private double colour;

	/** in ml, populated by the mash step */
	private double tunDeadSpace;

	/*-------------------------------------------------------------------------*/
	public MashVolume()
	{
		super(Type.MASH);
	}

	/*-------------------------------------------------------------------------*/
	public MashVolume(
		double volume,
		List<IngredientAddition> fermentables,
		WaterAddition water,
		double temperature,
		DensityUnit gravity,
		double colour,
		double tunDeadSpace)
	{
		super(Type.MASH);
		this.temperature = temperature;
		this.volume = volume;
		this.fermentables = fermentables;
		this.water = water;
		this.gravity = gravity;
		this.colour = colour;
		this.tunDeadSpace = tunDeadSpace;
	}

	public List<IngredientAddition> getFermentables()
	{
		return fermentables;
	}

	public WaterAddition getWater()
	{
		return water;
	}

	public DensityUnit getGravity()
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

	public double getTunDeadSpace()
	{
		return tunDeadSpace;
	}

	public void setTunDeadSpace(double tunDeadSpace)
	{
		this.tunDeadSpace = tunDeadSpace;
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
			"%s: '%s'\n" +
			"Temp: %.2fC\n" +
			"Volume: %.1fl\n" +
			"Gravity: %.3f\n" +
			"Colour: %.1f SRM",
			getType().toString(),
			getName(),
			temperature,
			volume/1000,
			gravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
			colour);
	}

	@Override
	public boolean contains(IngredientAddition ingredient)
	{
		return false;
	}
}
