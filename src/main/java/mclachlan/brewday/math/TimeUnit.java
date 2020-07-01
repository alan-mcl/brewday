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
public class TimeUnit extends Quantity
{
	/** time in seconds */
	double time;

	public TimeUnit()
	{
	}

	public TimeUnit(double time)
	{
		this.time = time;
	}

	public TimeUnit(double amount, Unit unit, boolean estimated)
	{
		this.set(amount, unit);
		setEstimated(estimated);
	}

	public TimeUnit(double amount, boolean estimated)
	{
		this(amount);
		setEstimated(estimated);
	}

	@Override
	public double get()
	{
		return time;
	}

	@Override
	public double get(Unit unit)
	{
		switch (unit)
		{
			case SECONDS:
				return time;
			case MINUTES:
				return time/60D;
			case HOURS:
				return time/60D/60D;
			case DAYS:
				return time/60D/60D/24D;
			default:
				throw new BrewdayException("invalid: "+unit);
		}
	}

	/**
	 * @param seconds in seconds
	 */
	@Override
	public void set(double seconds)
	{
		this.time = seconds;
	}

	@Override
	public void set(double time, Unit unit)
	{
		switch (unit)
		{
			case SECONDS:
				this.time = time;
				break;
			case MINUTES:
				this.time = time*60D;
				break;
			case HOURS:
				this.time = time*60D*60D;
				break;
			case DAYS:
				this.time = time*60D*60D*24D;
				break;
			default:
				throw new BrewdayException("invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.SECONDS;
	}
}
