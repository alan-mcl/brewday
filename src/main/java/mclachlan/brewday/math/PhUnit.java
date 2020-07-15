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
public class PhUnit extends Quantity
{
	double ph;

	public PhUnit()
	{
	}

	public PhUnit(double ph)
	{
		this.ph = ph;
	}

	public PhUnit(double amount, boolean estimated)
	{
		this(amount);
		setEstimated(estimated);
	}

	@Override
	public double get()
	{
		return ph;
	}

	@Override
	public double get(Unit unit)
	{
		if (unit != Unit.PH)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		return ph;
	}

	@Override
	public void set(double amount)
	{
		this.ph = amount;
	}

	@Override
	public void set(double amount, Unit unit)
	{
		if (unit != Unit.PH)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		this.ph = amount;
	}

	@Override
	public Unit getUnit()
	{
		return Unit.PH;
	}

	@Override
	public Type getType()
	{
		return Type.OTHER;
	}

	@Override
	public String toString()
	{
		return "PhUnit{ph=" + ph + "}";
	}
}
