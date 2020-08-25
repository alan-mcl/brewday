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

package mclachlan.brewday.math;

import mclachlan.brewday.BrewdayException;

/**
 *
 */
public class PowerUnit extends Quantity
{
	/**
	 * Pressure in kW
	 */
	private double power;

	public PowerUnit()
	{
	}

	/**
	 * @param power in kW
	 */
	public PowerUnit(double power)
	{
		this.power = power;
	}

	public PowerUnit(PowerUnit other)
	{
		this(other.power);
		this.setEstimated(other.isEstimated());
	}

	public PowerUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	/**
	 * @return
	 * 	pressure in kW
	 */
	public double get()
	{
		return power;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this pressure in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case KILOWATT:
				return power;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the pressure in kW
	 */
	public void set(double c)
	{
		this.power = c;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case KILOWATT:
				power = amount;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.KILOWATT;
	}

	@Override
	public Type getType()
	{
		return Type.POWER;
	}
}
