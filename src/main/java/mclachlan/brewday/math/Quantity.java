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

package mclachlan.brewday.math;

import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;

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

	/*-------------------------------------------------------------------------*/
	// these package private to make the classes immutable outside of the package
	abstract void set(double amount);

	abstract void set(double amount, Unit unit);
	/*-------------------------------------------------------------------------*/

	public abstract Unit getUnit();

	public abstract Type getType();

	public enum Type
	{
		WEIGHT,
		LENGTH,
		VOLUME,
		TEMPERATURE,
		FLUID_DENSITY,
		COLOUR,
		BITTERNESS,
		CARBONATION,
		PRESSURE,
		TIME,
		SPECIFIC_HEAT,
		DIASTATIC_POWER,
		POWER,
		OTHER
	}

	public enum Unit
	{
		// weight units
		GRAMS,
		KILOGRAMS,
		OUNCES,
		POUNDS,

		// length units
		MILLIMETRE,
		CENTIMETRE,
		METRE,
		KILOMETER,
		INCH,
		FOOT,
		YARD,
		MILE,

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
		LOVIBOND,
		EBC,

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

		// diastatic power
		LINTNER,

		// percentage
		PERCENTAGE,
		PERCENTAGE_DISPLAY, // used for UI formatting

		// power
		KILOWATT,

		// other
		PPM,
		PH;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("quantity.unit." + name());
		}
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

			case MILLIMETRE:
			case CENTIMETRE:
			case METRE:
			case KILOMETER:
			case INCH:
			case FOOT:
			case YARD:
			case MILE:
				return new LengthUnit(amount, unit, false);

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
			case LOVIBOND:
			case EBC:
				return new ColourUnit(amount, unit, false);

			case IBU:
				return new BitternessUnit(amount, unit, false);

			case PERCENTAGE:
				return new PercentageUnit(amount, false);
			case PERCENTAGE_DISPLAY:
				return new PercentageUnit(amount / 100D, false);

			case PPM:
				return new PpmUnit(amount, false);
			case PH:
				return new PhUnit(amount, false);

			case GRAMS_PER_LITRE:
			case VOLUMES:
				return new CarbonationUnit(amount, unit, false);

			case KPA:
			case PSI:
				return new PressureUnit(amount, unit, false);

			case KILOWATT:
				return new PowerUnit(amount, unit, false);

			case SECONDS:
			case MINUTES:
			case HOURS:
			case DAYS:
				return new TimeUnit(amount, unit, false);

			case JOULE_PER_KG_CELSIUS:
				return new ArbitraryPhysicalQuantity(amount, unit);

			case LINTNER:
				return new DiastaticPowerUnit(amount, false);

			default:
				throw new BrewdayException("invalid: " + unit);
		}
	}
}
