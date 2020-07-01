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

package mclachlan.brewday.test;

import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;

/**
 *
 */
public class TestDensityUnit
{
	public static void main(String[] args) throws Exception
	{
		testPlatoToSg(27.7);
		testPlatoToSg(25.9);
		testPlatoToSg(20.4);
		testPlatoToSg(16.1);
		testPlatoToSg(13.3);
		testPlatoToSg(11.3);
		testPlatoToSg(9.9);
		testPlatoToSg(20.846550787987894);
	}

	private static void testPlatoToSg(double plato)
	{
		double sg = getSg(plato);
		System.out.println(plato+" Plato: "+sg);
	}

	private static double getSg(double plato)
	{
		DensityUnit test = new DensityUnit(plato, Quantity.Unit.PLATO);
		return test.get(Quantity.Unit.SPECIFIC_GRAVITY);
	}
}
