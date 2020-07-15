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
public class CarbonationUnit extends Quantity
{
	/**
	 * Carbonation in grams per litre
	 */
	private double carbonation;

	public CarbonationUnit()
	{
	}

	/**
	 * @param carbonation in g per l
	 */
	public CarbonationUnit(double carbonation)
	{
		this.carbonation = carbonation;
	}

	public CarbonationUnit(CarbonationUnit other)
	{
		this(other.carbonation);
		this.setEstimated(other.isEstimated());
	}

	public CarbonationUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	/**
	 * @return
	 * 	carbonation in grams per litre
	 */
	public double get()
	{
		return carbonation;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this carbonation in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case GRAMS_PER_LITRE:
				return carbonation;
			case VOLUMES:
				// conversion factor from g/l to vols CO2 is 1/1.96=0.51
				return carbonation * 0.51D;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the carbonation in grams per litre
	 */
	public void set(double c)
	{
		this.carbonation = c;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case GRAMS_PER_LITRE:
				carbonation = amount;
				break;
			case VOLUMES:
				// conversion factor from g/l to vols CO2 is 1/1.96=0.51
				carbonation = amount * 1.96D;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.GRAMS_PER_LITRE;
	}

	@Override
	public Type getType()
	{
		return Type.CARBONATION;
	}
}
