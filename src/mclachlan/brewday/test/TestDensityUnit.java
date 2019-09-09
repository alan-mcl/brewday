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
