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
public class LengthUnit extends Quantity
{
	/**
	 * Length in mm
	 */
	private double len;

	public LengthUnit()
	{
	}

	/**
	 * @param mm in mm
	 */
	public LengthUnit(double mm)
	{
		this.len = mm;
	}

	public LengthUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	public LengthUnit(double amount, Unit unit)
	{
		this(amount, unit, false);
	}

	/**
	 * @return length in mm
	 */
	public double get()
	{
		return len;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this weight in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case MILLIMETRE:
				return len;
			case CENTIMETRE:
				return len / 10;
			case METRE:
				return len / 100;
			case KILOMETER:
				return len / 1000;

			case INCH:
				return len * 0.03937007874;
			case FOOT:
				return len * 0.0032808;
			case YARD:
				return len * 0.0010936;
			case MILE:
				return len * 0.00000062137;
			default:
				throw new BrewdayException("Invalid: " + unit);
		}
	}

	/**
	 * @param mm the length in mm
	 */
	public void set(double mm)
	{
		this.len = mm;
	}

	/**
	 * @param amount the weight
	 * @param unit   the unit of the amount
	 */
	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case MILLIMETRE:
				len = amount;
				break;
			case CENTIMETRE:
				len = amount * 10;
				break;
			case METRE:
				len = amount * 100;
				break;
			case KILOMETER:
				len = amount * 1000;
				break;

			case INCH:
				len = amount / 0.03937007874;
				break;
			case FOOT:
				len = amount / 0.0032808;
				break;
			case YARD:
				len = amount / 0.0010936;
				break;
			case MILE:
				len = amount / 0.00000062137;
				break;
			default:
				throw new BrewdayException("Invalid: " + unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.MILLIMETRE;
	}

	@Override
	public Type getType()
	{
		return Type.LENGTH;
	}

}
