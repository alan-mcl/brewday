package mclachlan.brewday.math;

/**
 *
 */
public interface Quantity
{
	double get();

	double get(Unit unit);

	void set(double amount);

	void set(double amount, Unit unit);

	public static enum Unit
	{
		// weight units
		GRAMS,
		KILOGRAMS,
		OUNCES,
		POUNDS,

		// volume units
		MILLILITRES,
		LITRES,
		US_FLUID_OUNCE,
		US_GALLON,

		// temperature units
		CELSIUS,
		KELVIN,
		FAHRENHEIT,

		// fluid density units
		GU,
		SPECIFIC_GRAVITY,
		PLATO,
	}
}
