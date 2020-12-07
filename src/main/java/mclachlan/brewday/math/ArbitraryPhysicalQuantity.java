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
 * Container for an arbitrary physical quantity. Defaults to grams
 */
public class ArbitraryPhysicalQuantity extends Quantity
{
	/** This defaults to grams */
	private double amount;

	/** The unit this is stored in */
	private Unit unit;

	public ArbitraryPhysicalQuantity()
	{
	}

	public ArbitraryPhysicalQuantity(double amount)
	{
		this.amount = amount;
	}

	public ArbitraryPhysicalQuantity(double amount, Unit unit)
	{
		set(amount, unit);
	}

	public ArbitraryPhysicalQuantity(WeightUnit weight)
	{
		this(weight.get(Unit.GRAMS), Unit.GRAMS);
	}

	public ArbitraryPhysicalQuantity(Quantity quantity)
	{
		this(quantity.get(), quantity.getUnit());
	}

	/**
	 * @return
	 * 	amount in whatever unit this contains
	 */
	@Override
	public double get()
	{
		return amount;
	}

	@Override
	public double get(Unit unit)
	{
		if (unit != this.unit)
		{
			throw new BrewdayException("Invalid: "+unit);
		}

		return amount;
	}

	/**
	 * @param amount in grams
	 */
	@Override
	public void set(double amount)
	{
		this.amount = amount;
	}

	@Override
	public void set(double amount, Unit unit)
	{
		this.amount = amount;
		this.unit = unit;
	}

	public Unit getUnit()
	{
		return unit;
	}

	@Override
	public Type getType()
	{
		return Type.OTHER;
	}
}
