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
public class PercentageUnit extends Quantity
{
	double percentage;

	public PercentageUnit()
	{
	}

	public PercentageUnit(double percentage)
	{
		this.percentage = percentage;
	}

	public PercentageUnit(double amount, boolean estimated)
	{
		this(amount);
		setEstimated(estimated);
	}

	public PercentageUnit(PercentageUnit other)
	{
		this(other.percentage);
		this.setEstimated(other.isEstimated());
	}

	@Override
	public double get()
	{
		return percentage;
	}

	@Override
	public double get(Unit unit)
	{
		if (unit == Unit.PERCENTAGE)
		{
			return percentage;
		}
		else if (unit == Unit.PERCENTAGE_DISPLAY)
		{
			return percentage * 100D;
		}
		else
		{
			throw new BrewdayException("invalid: "+unit);
		}
	}

	@Override
	public void set(double amount)
	{
		this.percentage = amount;
	}

	@Override
	public void set(double amount, Unit unit)
	{
		if (unit == Unit.PERCENTAGE)
		{
			this.percentage = amount;
		}
		else if (unit == Unit.PERCENTAGE_DISPLAY)
		{
			this.percentage = amount/100D;
		}
		else
		{
			throw new BrewdayException("invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.PERCENTAGE;
	}

	@Override
	public Type getType()
	{
		return Type.OTHER;
	}

	@Override
	public String toString()
	{
		return "PercentageUnit{percentage=" + percentage + "}";
	}
}
