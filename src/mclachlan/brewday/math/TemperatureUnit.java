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
public class TemperatureUnit extends Quantity
{
	/**
	 * Temperature in C
	 */
	private double temperature;

	/**
	 * @param temperature
	 * 	in C
	 */
	public TemperatureUnit(double temperature)
	{
		this.temperature = temperature;
	}

	public TemperatureUnit(TemperatureUnit other)
	{
		this(other.temperature);
		this.setEstimated(other.isEstimated());
	}

	public TemperatureUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	/**
	 * @return
	 * 	temp in C
	 */
	public double get()
	{
		return temperature;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this temp in the given unit
	 */
	public double get(Quantity.Unit unit)
	{
		switch (unit)
		{
			case CELSIUS:
				return this.temperature;
			case KELVIN:
				return this.temperature + 273.15D;
			case FAHRENHEIT:
				return this.temperature*9D/5D +32;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the temp in C
	 */
	public void set(double c)
	{
		this.temperature = c;
	}

	public void set(double amount, Quantity.Unit unit)
	{
		switch (unit)
		{
			case CELSIUS:
				this.temperature = amount;
				break;
			case KELVIN:
				this.temperature = amount - 273.15D;
				break;
			case FAHRENHEIT:
				this.temperature = (amount -32) * 5D/9D;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.CELSIUS;
	}
}
