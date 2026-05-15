/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.test;

import mclachlan.brewday.math.*;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Manual check: metric inputs equivalent to Mike Soltys spreadsheet defaults → ~3.47 m hose.
 */
public class TestKegLineLengthCalculator
{
	private static final double TARGET_LENGTH_M = 3.47D;
	private static final double LENGTH_TOLERANCE_M = 0.02D;

	/*-------------------------------------------------------------------------*/

	public static void main(String[] args)
	{
		DensityUnit sg = new DensityUnit(1.05D, SPECIFIC_GRAVITY);
		PressureUnit pressure = new PressureUnit(14D * 6.89475728D, KPA, false);
		LengthUnit hoseId = new LengthUnit(0.1875D / 0.03937007874D, MILLIMETRE);
		LengthUnit tapHeight = new LengthUnit(1.5D / 3.2808D, METRE);
		TimeUnit pourTime = new TimeUnit(10D);

		KegLineLengthCalculator.Result result = KegLineLengthCalculator.calculate(
			sg, pressure, hoseId, tapHeight, pourTime, null);

		double lengthM = result.hoseLength().get(METRE);
		System.out.printf("Hose length: %.3f m%n", lengthM);
		System.out.printf("Re: %.1f  f: %.5f%n", result.reynoldsNumber(), result.frictionFactor());

		if (Math.abs(lengthM - TARGET_LENGTH_M) > LENGTH_TOLERANCE_M)
		{
			throw new RuntimeException(
				"Expected ~" + TARGET_LENGTH_M + " m (+/- " + LENGTH_TOLERANCE_M + "), got " + lengthM);
		}

		System.out.println("OK");
	}
}
