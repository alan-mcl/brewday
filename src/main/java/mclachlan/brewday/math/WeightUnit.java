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
public class WeightUnit extends Quantity
{
	/**
	 * Weight in grams
	 */
	private double weight;

	/**
	 * @param weight
	 * 	in grams
	 */
	public WeightUnit(double weight)
	{
		this.weight = weight;
	}

	public WeightUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	public WeightUnit(double amount, Unit unit)
	{
		this(amount, unit, false);
	}

	/**
	 * @return
	 * 	weight in grams
	 */
	public double get()
	{
		return weight;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this weight in the given unit
	 */
	public double get(Quantity.Unit unit)
	{
		switch (unit)
		{
			case GRAMS:
				return weight;
			case KILOGRAMS:
				return weight / 1000;
			case OUNCES:
				return weight / Const.GRAMS_PER_OUNCE;
			case POUNDS:
				return weight / Const.GRAMS_PER_POUND;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param grams the weight in grams
	 */
	public void set(double grams)
	{
		this.weight = grams;
	}

	/**
	 * @param amount the weight
	 * @param unit the unit of the amount
	 */
	public void set(double amount, Quantity.Unit unit)
	{
		switch (unit)
		{
			case GRAMS:
				weight = amount;
				break;
			case KILOGRAMS:
				weight = amount * 1000;
				break;
			case OUNCES:
				weight = amount * Const.GRAMS_PER_OUNCE;
				break;
			case POUNDS:
				weight = amount * Const.GRAMS_PER_POUND;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.GRAMS;
	}

	@Override
	public Type getType()
	{
		return Type.WEIGHT;
	}

}
