package mclachlan.brewday.math;

import mclachlan.brewday.BrewdayException;

/**
 *
 */
public abstract class Quantity
{
	private boolean estimated = true;

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

		// carbonation
		GRAMS_PER_LITRE,
		VOLUMES,

		// pressure
		KPA,
		PSI,
		BAR,

		// time
		SECONDS,
		MINUTES,
		HOURS,
		DAYS,

		// specific heat
		JOULE_PER_KG_CELSIUS,

		// other
		PERCENTAGE,
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param str  String for parsing
	 * @param unit The desired output unit
	 * @return A Quantity in the given output unit.
	 */
	public static Quantity parseQuantity(String str, Unit unit)
	{
		double amount = Double.parseDouble(str);

		switch (unit)
		{
			case GRAMS:
			case KILOGRAMS:
			case OUNCES:
			case POUNDS:
				return new WeightUnit(amount, unit, false);

			case MILLILITRES:
			case LITRES:
			case US_FLUID_OUNCE:
			case US_GALLON:
				return new VolumeUnit(amount, unit, false);

			case CELSIUS:
			case KELVIN:
			case FAHRENHEIT:
				return new TemperatureUnit(amount, unit, false);

			case GU:
			case SPECIFIC_GRAVITY:
			case PLATO:
				return new DensityUnit(amount, unit, false);

			case SRM:
				return new ColourUnit(amount, unit, false);

			case IBU:
				return new BitternessUnit(amount, unit, false);

			case PERCENTAGE:
				return new PercentageUnit(amount, false);

			case GRAMS_PER_LITRE:
			case VOLUMES:
				return new CarbonationUnit(amount, unit, false);

			case KPA:
			case PSI:
				return new PressureUnit(amount, unit, false);

			case SECONDS:
			case MINUTES:
			case HOURS:
			case DAYS:
				return new TimeUnit(amount, unit, false);

			case JOULE_PER_KG_CELSIUS:
				return new ArbitraryPhysicalQuantity(amount, unit);

			default:
				throw new BrewdayException("invalid: " + unit);
		}
	}
}
