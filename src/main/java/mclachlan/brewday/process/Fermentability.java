package mclachlan.brewday.process;

import mclachlan.brewday.math.PercentageUnit;

public class Fermentability
{
	public static PercentageUnit
		LOW = new PercentageUnit(.55D),
		MEDIUM = new PercentageUnit(.65),
		HIGH = new PercentageUnit(.75);
}
