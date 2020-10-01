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
public class PpmUnit extends Quantity
{
	private double ppm;

	public PpmUnit()
	{
	}

	public PpmUnit(double ppm)
	{
		this.ppm = ppm;
	}

	public PpmUnit(double amount, boolean estimated)
	{
		this(amount);
		setEstimated(estimated);
	}

	@Override
	public double get()
	{
		return ppm;
	}

	@Override
	public double get(Unit unit)
	{
		if (unit != Unit.PPM)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		return ppm;
	}

	@Override
	public void set(double amount)
	{
		this.ppm = amount;
	}

	@Override
	public void set(double amount, Unit unit)
	{
		if (unit != Unit.PPM)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		this.ppm = amount;
	}

	@Override
	public Unit getUnit()
	{
		return Unit.PPM;
	}

	@Override
	public Type getType()
	{
		return Type.OTHER;
	}

	@Override
	public String toString()
	{
		return "PpmUnit{ppm=" + ppm + "}";
	}
}
