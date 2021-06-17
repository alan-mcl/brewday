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

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.*;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;

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
		setWater(water);
		setQuantity(quantity);
		setUnit(unit);
		setTime(time);
		setTemperature(temperature);
	}

	public WaterAddition(WaterAddition other)
	{
		setWater(other.getWater());
		setQuantity(other.getQuantity());
		setUnit(other.getUnit());
		setTime(other.getTime());
		setTemperature(other.getTemperature());
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
	public Quantity.Type getAdditionQuantityType()
	{
		return Quantity.Type.VOLUME;
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

	/*-------------------------------------------------------------------------*/
	public String describe()
	{
		double quantity = getQuantity().get(getUnit());
		String quantityS = StringUtils.format(quantity, getUnit());

		double temp = getTemperature().get(CELSIUS);
		String tempS = StringUtils.format(temp, CELSIUS);

		return StringUtils.getDocString("water.addition.desc",
			quantityS, water.getName(), tempS);
	}

	@Override
	public Type getType()
	{
		return Type.WATER;
	}


	@Override
	public String toString()
	{
//		return getName();
		String qty;

		if (getQuantity().get(Quantity.Unit.LITRES) < 1)
		{
			qty = getQuantity().describe(Quantity.Unit.MILLILITRES);
		}
		else
		{
			qty = getQuantity().describe(Quantity.Unit.LITRES);
		}


		return StringUtils.getUiString("water.addition.toString",
			getName(),
			qty,
			getTime().get(Quantity.Unit.MINUTES));
	}
}
