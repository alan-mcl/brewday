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
public class ColourUnit extends Quantity
{
	/**
	 * Colour in SRM
	 */
	private double srm;

	public ColourUnit()
	{
	}

	/**
	 * @param srm in SRM
	 */
	public ColourUnit(double srm)
	{
		this.srm = srm;
	}

	public ColourUnit(ColourUnit other)
	{
		this(other.srm);
		this.setEstimated(other.isEstimated());
	}

	public ColourUnit(double amount, Unit unit, boolean estimated)
	{
		this.setEstimated(estimated);
		this.set(amount, unit);
	}

	public ColourUnit(double amount, Unit unit)
	{
		this.set(amount, unit);
		this.setEstimated(true);
	}

	/**
	 * @return
	 * 	colour in SRM
	 */
	public double get()
	{
		return srm;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this colour in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case SRM:
				return srm;
			case LOVIBOND:
				return (srm + 0.6) / 1.3546;
			case EBC:
				return srm * 1.97;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param c the colour in SRM
	 */
	public void set(double c)
	{
		this.srm = c;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case SRM:
				srm = amount;
				break;
			case LOVIBOND:
				srm = (1.3546 * amount) - 0.6;
				break;
			case EBC:
				srm = amount / 19.7;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public Unit getUnit()
	{
		return Unit.SRM;
	}

	@Override
	public Type getType()
	{
		return Type.COLOUR;
	}

	@Override
	public String toString()
	{
		return "ColourUnit{colour=" + srm + "SRM}";
	}

	public static void main(String[] args)
	{
		ColourUnit test = new ColourUnit(10, Unit.SRM);
		System.out.println("10 SRM in LOVIBOND) = " + test.get(Unit.LOVIBOND));
		System.out.println("10 SRM in EBC) = " + test.get(Unit.EBC));

		test = new ColourUnit(20, Unit.SRM);
		System.out.println("20 SRM in LOVIBOND) = " + test.get(Unit.LOVIBOND));
		System.out.println("20 SRM in EBC) = " + test.get(Unit.EBC));

		test = new ColourUnit(50, Unit.SRM);
		System.out.println("50 SRM in LOVIBOND) = " + test.get(Unit.LOVIBOND));
		System.out.println("50 SRM in EBC) = " + test.get(Unit.EBC));

		test = new ColourUnit(100, Unit.SRM);
		System.out.println("100 SRM in LOVIBOND) = " + test.get(Unit.LOVIBOND));
		System.out.println("100 SRM in EBC) = " + test.get(Unit.EBC));


	}
}
