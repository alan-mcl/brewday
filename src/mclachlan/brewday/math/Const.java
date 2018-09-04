package mclachlan.brewday.math;

/**
 *
 */
public class Const
{
	/** boil off rate in l per h */
	public static double BOIL_OFF_PER_HOUR = 3D;

	/** used in ABV equation */
	public static double ABV_CONST = 1.05D / 0.79D;

	/** volume shrinkage in % per deg C */
	public static double COOLING_SHRINKAGE = 0.04D/80;

	/** heat loss assuming an ambient temperature of 30C, in deg C per h */
	public static double HEAT_LOSS = 30D;

	/** specific heat of water, in Kj/(kg*K) */
	public static double SPECIFIC_HEAT_OF_WATER = 4.2D;
}
