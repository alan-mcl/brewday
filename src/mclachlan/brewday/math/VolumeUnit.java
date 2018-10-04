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

package mclachlan.brewday.math;

import mclachlan.brewday.BrewdayException;

/**
 *
 */
public class VolumeUnit
{
	/**
	 * Volume in ml
	 */
	private double volume;

	/**
	 * @param volume in ml
	 */
	public VolumeUnit(double volume)
	{
		this.volume = volume;
	}

	/**
	 * @return
	 * 	temp in ml
	 */
	public double get()
	{
		return volume;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this volume in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case MILLILITRES:
				return volume;
			case LITRES:
				return volume / 1000;
			case US_FLUID_OUNCE:
				return volume / Const.ML_PER_US_FL_OZ;
			case US_GALLON:
				return volume / Const.ML_PER_US_GALLON;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the volume in ml
	 */
	public void set(double c)
	{
		this.volume = c;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case MILLILITRES:
				volume = amount;
				break;
			case LITRES:
				volume  = amount * 1000;
				break;
			case US_FLUID_OUNCE:
				volume  = amount * Const.ML_PER_US_FL_OZ;
				break;
			case US_GALLON:
				volume = amount * Const.ML_PER_US_GALLON;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	public static enum Unit
	{
		MILLILITRES,
		LITRES,
		US_FLUID_OUNCE,
		US_GALLON
	}
}
