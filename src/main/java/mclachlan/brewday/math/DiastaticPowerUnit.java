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
public class DiastaticPowerUnit extends Quantity
{
	double lintner;

	public DiastaticPowerUnit()
	{
	}

	public DiastaticPowerUnit(double lintner)
	{
		this.lintner = lintner;
	}

	public DiastaticPowerUnit(double amount, boolean estimated)
	{
		this(amount);
		setEstimated(estimated);
	}

	@Override
	public double get()
	{
		return lintner;
	}

	@Override
	public double get(Unit unit)
	{
		if (unit != Unit.LINTNER)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		return lintner;
	}

	@Override
	public void set(double amount)
	{
		this.lintner = amount;
	}

	@Override
	public void set(double amount, Unit unit)
	{
		if (unit != Unit.LINTNER)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		this.lintner = amount;
	}

	@Override
	public Unit getUnit()
	{
		return Unit.LINTNER;
	}

	@Override
	public Type getType()
	{
		return Type.DIASTATIC_POWER;
	}

	@Override
	public String toString()
	{
		return "DiastaticPowerUnit{lintner=" + lintner + "}";
	}
}
