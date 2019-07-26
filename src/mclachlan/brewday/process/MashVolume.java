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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class MashVolume extends Volume
{
	private String name;

	private VolumeUnit volume;
	private TemperatureUnit temperature;
	private DensityUnit gravity = new DensityUnit();
	private ColourUnit colour;

	/** grains in the mash */
	private List<IngredientAddition> fermentables;

	/** water in the mash */
	private WaterAddition water;

	/** populated from the equipment profile */
	private VolumeUnit tunDeadSpace;

	/*-------------------------------------------------------------------------*/
	public MashVolume()
	{
		super(Type.MASH);
	}

	/*-------------------------------------------------------------------------*/
	public MashVolume(
		VolumeUnit volume,
		List<IngredientAddition> fermentables,
		WaterAddition water,
		TemperatureUnit temperature,
		DensityUnit gravity,
		ColourUnit colour,
		VolumeUnit tunDeadSpace)
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

	public ColourUnit getColour()
	{
		return colour;
	}

	public void setColour(ColourUnit colour)
	{
		this.colour = colour;
	}

	public VolumeUnit getVolume()
	{
		return volume;
	}

	public TemperatureUnit getTemperature()
	{
		return temperature;
	}

	public VolumeUnit getTunDeadSpace()
	{
		return tunDeadSpace;
	}

	public void setTunDeadSpace(VolumeUnit tunDeadSpace)
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
		double t = temperature==null ? Double.NaN : temperature.get(Quantity.Unit.CELSIUS);
		double v = volume==null ? Double.NaN : volume.get(Quantity.Unit.LITRES);
		double g = gravity==null ? Double.NaN : gravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY);
		double c = colour == null ? Double.NaN : colour.get(Quantity.Unit.SRM);

		return
			StringUtils.getProcessString("volumes.mash.format",
				getType().toString(),
				getName(),
				t,
				v,
				g,
				c);
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public Volume clone()
	{
		return new MashVolume(
			volume,
			fermentables,
			water,
			temperature,
			gravity,
			colour,
			tunDeadSpace);
	}
}
