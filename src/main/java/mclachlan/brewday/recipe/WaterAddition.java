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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.recipe;

import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.*;

/**
 *
 */
public class WaterAddition extends IngredientAddition
{
	private Water water;
	private TemperatureUnit temperature;

	/*-------------------------------------------------------------------------*/
	public WaterAddition()
	{
	}

	/*-------------------------------------------------------------------------*/
	public WaterAddition(
		Water water,
		VolumeUnit quantity,
		Quantity.Unit unit,
		TemperatureUnit temperature,
		TimeUnit time)
	{
		this.water = water;
		setQuantity(quantity);
		setUnit(unit);
		setTime(time);
		this.temperature = temperature;
	}

	public Water getWater()
	{
		return water;
	}

	public void setWater(Water water)
	{
		this.water = water;
	}

	@Override
	public String getName()
	{
		return water.getName();
	}

	@Override
	public void setName(String newName)
	{
		// not possible
	}

	@Override
	public IngredientAddition clone()
	{
		return new WaterAddition(
			this.water,
			(VolumeUnit)getQuantity(),
			getUnit(),
			new TemperatureUnit(this.temperature.get()),
			this.getTime());
	}

	public boolean contains(IngredientAddition ingredient)
	{
		return ingredient == this;
	}

	public TemperatureUnit getTemperature()
	{
		return temperature;
	}

	public void setTemperature(TemperatureUnit temperature)
	{
		this.temperature = temperature;
	}

	public VolumeUnit getVolume()
	{
		return (VolumeUnit)getQuantity();
	}

	public void setVolume(VolumeUnit volume)
	{
		setQuantity(volume);
	}

	public WaterAddition getCombination(WaterAddition other)
	{
		TemperatureUnit combinedTemp = Equations.calcCombinedTemperature(
			(VolumeUnit)getQuantity(),
			this.getTemperature(),
			(VolumeUnit)other.getQuantity(),
			other.getTemperature());

		VolumeUnit combinedVolume = new VolumeUnit(
			getQuantity().get() + other.getQuantity().get());

		Water combinedWater = Equations.calcCombinedWaterProfile(
			this.water, (VolumeUnit)getQuantity(),
			other.water, (VolumeUnit)other.getQuantity());

		return new WaterAddition(
			combinedWater,
			combinedVolume,
			getUnit(),
			combinedTemp,
			new TimeUnit(0));
	}

	@Override
	public Type getType()
	{
		return Type.WATER;
	}

	@Override
	public String toString()
	{
		return getName();
//		return StringUtils.getUiString("water.addition.toString",
//			getName(),
//			getQuantity().get(Quantity.Unit.LITRES),
//			getTime().get(Quantity.Unit.MINUTES));
	}
}
