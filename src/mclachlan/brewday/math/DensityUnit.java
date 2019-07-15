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
public class DensityUnit
{
	/**
	 * Density in GU
	 */
	private double density;

	public DensityUnit()
	{
	}

	/**
	 * @param density
	 * 	in GU
	 */
	public DensityUnit(double density)
	{
		this.density = density;
	}

	public DensityUnit(double amount, Unit unit)
	{
		this.set(amount, unit);
	}

	/**
	 * @return
	 * 	density in GU
	 */
	public double getDensity()
	{
		return density;
	}

	/**
	 * @param unit the unit to return a value in
	 * @return this density in the given unit
	 */
	public double get(Unit unit)
	{
		switch (unit)
		{
			case GU:
				return density;
			case SPECIFIC_GRAVITY:
				return (1000+density)/1000;
			case PLATO:
				double sg = (1000+density)/1000;
				return
					135.997*Math.pow(sg, 3) -
					630.272*Math.pow(sg, 2) +
					1111.14*sg -
					616.868;

			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	/**
	 * @param gu the density in GU
	 */
	public void setDensity(double gu)
	{
		this.density = gu;
	}

	public void set(double amount, Unit unit)
	{
		switch (unit)
		{
			case GU:
				this.density = amount;
				break;
			case SPECIFIC_GRAVITY:
				this.density = 1000 * amount -1000;
				break;
			case PLATO:
				this.density = 1000*(1 + (amount / (258.6 - 227.1*(amount/258.2)))) -1000;
				break;
			default:
				throw new BrewdayException("Invalid: "+unit);
		}
	}

	@Override
	public String toString()
	{
		return density+"(GU)";
	}

	public static enum Unit
	{
		GU,
		SPECIFIC_GRAVITY,
		PLATO,
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		DensityUnit test = new DensityUnit(50);

		System.out.println("1050");
		System.out.println("GU: "+test.get(Unit.GU));
		System.out.println("P: "+test.get(Unit.PLATO));

		test.set(10, Unit.PLATO);

		System.out.println("10P");
		System.out.println("GU: "+test.get(Unit.GU));
		System.out.println("P: "+test.get(Unit.PLATO));
	}
}
