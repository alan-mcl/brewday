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
public class BitternessUnit extends Quantity
{
	/**
	 * Bitterness in IBU
	 */
	private double bitterness;

	/**
	 * @param bitterness in IBU
	 */
	public BitternessUnit(double bitterness)
	{
		this.bitterness = bitterness;
	}

	public BitternessUnit(BitternessUnit other)
	{
		this(other.bitterness);
	}

	public BitternessUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	/**
	 * @return
	 * 	bitterness in IBU
	 */
	public double get()
	{
		return bitterness;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this bitterness in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case IBU:
				return bitterness;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the bitterness in IBU
	 */
	public void set(double c)
	{
		this.bitterness = c;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case IBU:
				bitterness = amount;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.IBU;
	}

	public void add(BitternessUnit other)
	{
		this.bitterness += other.get(Unit.IBU);
	}
}
