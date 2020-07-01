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
public class PressureUnit extends Quantity
{
	/**
	 * Pressure in kPa
	 */
	private double pressure;

	/**
	 * @param pressure in kPa
	 */
	public PressureUnit(double pressure)
	{
		this.pressure = pressure;
	}

	public PressureUnit(PressureUnit other)
	{
		this(other.pressure);
		this.setEstimated(other.isEstimated());
	}

	public PressureUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	/**
	 * @return
	 * 	pressure in kPa
	 */
	public double get()
	{
		return pressure;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this pressure in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case KPA:
				return pressure;
			case PSI:
				return pressure / 6.89475728D;
			case BAR:
				return pressure / 100;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the pressure in kPa
	 */
	public void set(double c)
	{
		this.pressure = c;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case KPA:
				pressure = amount;
				break;
			case PSI:
				pressure = amount * 6.89475728D;
				break;
			case BAR:
				pressure = amount * 100;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.KPA;
	}
}
