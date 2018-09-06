package mclachlan.brewday.math;

/**
 *
 */
public class Convert
{
	public static double gramsToLbs(double grams)
	{
		return grams / Const.GRAMS_PER_POUND;
	}

	public static double mlToGallons(double ml)
	{
		return ml / 1000 / Const.LITERS_PER_GALLON;
	}
}
