package mclachlan.brewday.math;

/**
 *
 */
public abstract class Quantity
{
	private boolean estimated;

	public boolean isEstimated()
	{
		return estimated;
	}

	public void setEstimated(boolean estimated)
	{
		this.estimated = estimated;
	}

	public abstract double get();

	public abstract double get(Unit unit);

	public abstract void set(double amount);

	public abstract void set(double amount, Unit unit);

	public abstract Unit getUnit();

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

		// colour units
		SRM,

		// bitterness
		IBU,

		// other
		PERCENTAGE,
	}
}
