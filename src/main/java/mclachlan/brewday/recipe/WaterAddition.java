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

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.*;

/**
 *
 */
public class WaterAddition extends IngredientAddition
{
	private Water water;
	private TemperatureUnit temperature;
	private VolumeUnit volume;

	/*-------------------------------------------------------------------------*/
	public WaterAddition()
	{
	}

	/*-------------------------------------------------------------------------*/
	public WaterAddition(
		Water water,
		VolumeUnit volume,
		TemperatureUnit temperature,
		TimeUnit time)
	{
		this.water = water;
		setVolume(volume);
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
	public Quantity getQuantity()
	{
		return new VolumeUnit(this.volume.get(Quantity.Unit.MILLILITRES));
	}

	@Override
	public void setQuantity(Quantity weight)
	{
		this.volume = new VolumeUnit(weight.get(Quantity.Unit.GRAMS));
	}

	@Override
	public IngredientAddition clone()
	{
		return new WaterAddition(
			this.water,
			new VolumeUnit(this.volume.get()),
			new TemperatureUnit(this.temperature.get()),
			this.getTime());
	}

	public String describe()
	{
		return String.format("Water: %s, %.1fl at %.1fC", getName(),
			volume.get(Quantity.Unit.LITRES), temperature.get(Quantity.Unit.CELSIUS));
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

	public WaterAddition getCombination(String name, WaterAddition other)
	{
		return new WaterAddition(
			water,
			new VolumeUnit(this.getVolume().get()+other.getVolume().get()),
			Equations.calcNewFluidTemperature(
				this.getVolume(),
				this.getTemperature(),
				other.getVolume(),
				other.getTemperature()),
			new TimeUnit(0));
	}

	public void combineWith(WaterAddition other)
	{
		this.setVolume(new VolumeUnit(getVolume().get() + other.getVolume().get()));
		this.temperature =
			Equations.calcNewFluidTemperature(
				this.getVolume(),
				this.getTemperature(),
				other.getVolume(),
				other.getTemperature());
	}

	@Override
	public Type getType()
	{
		return Type.WATER;
	}

	public VolumeUnit getVolume()
	{
		return volume;
	}

	public void setVolume(VolumeUnit volume)
	{
		this.volume = volume;
	}

	@Override
	public String toString()
	{
		return StringUtils.getUiString("water.addition.toString",
			getName(),
			getQuantity().get(Quantity.Unit.LITRES),
			getTime().get(Quantity.Unit.MINUTES));
	}
}
