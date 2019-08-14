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
public class ColourUnit extends Quantity
{
	/**
	 * Colour in SRM
	 */
	private double colour;

	/**
	 * @param colour in SRM
	 */
	public ColourUnit(double colour)
	{
		this.colour = colour;
	}

	public ColourUnit(ColourUnit other)
	{
		this(other.colour);
	}

	public ColourUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	/**
	 * @return
	 * 	colour in SRM
	 */
	public double get()
	{
		return colour;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this colour in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case SRM:
				return colour;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the colour in SRM
	 */
	public void set(double c)
	{
		this.colour = c;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case SRM:
				colour = amount;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.SRM;
	}
}
