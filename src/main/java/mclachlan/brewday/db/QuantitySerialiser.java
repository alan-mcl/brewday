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

package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.math.*;

/**
 *
 */
public class QuantitySerialiser implements V2SerialiserMap<Quantity>
{
	private boolean ignoreEstimated;

	public QuantitySerialiser(boolean ignoreEstimated)
	{
		this.ignoreEstimated = ignoreEstimated;
	}

	@Override
	public Map<String, ?> toMap(Quantity quantity)
	{
		if (ignoreEstimated && quantity.isEstimated())
		{
			return null;
		}
		Map<String, Object> result = new HashMap<>();
		result.put("amount", String.valueOf(quantity.get()));
		result.put("unit", quantity.getUnit().name());
		result.put("estimate", quantity.isEstimated());
		return result;
	}

	@Override
	public Quantity fromMap(Map<String, ?> map)
	{
		double amount = Double.valueOf((String)map.get("amount"));
		Quantity.Unit unit = Quantity.Unit.valueOf((String)map.get("unit"));
		boolean estimated = (Boolean)map.get("estimate");

		switch (unit)
		{
			case GRAMS:
			case KILOGRAMS:
			case OUNCES:
			case POUNDS:
				return new WeightUnit(amount, unit, estimated);

			case MILLILITRES:
			case LITRES:
			case US_FLUID_OUNCE:
			case US_GALLON:
				return new VolumeUnit(amount, unit, estimated);

			case CELSIUS:
			case KELVIN:
			case FAHRENHEIT:
				return new TemperatureUnit(amount, unit, estimated);

			case GU:
			case SPECIFIC_GRAVITY:
			case PLATO:
				return new DensityUnit(amount, unit, estimated);

			case SRM:
				return new ColourUnit(amount, unit, estimated);

			case IBU:
				return new BitternessUnit(amount, unit, estimated);

			case PERCENTAGE:
				return new PercentageUnit(amount, estimated);
			case PPM:
				return new PpmUnit(amount, estimated);
			case PH:
				return new PhUnit(amount, estimated);

			case GRAMS_PER_LITRE:
			case VOLUMES:
				return new CarbonationUnit(amount, unit, estimated);

			case KPA:
			case PSI:
				return new PressureUnit(amount, unit, estimated);

			case SECONDS:
			case MINUTES:
			case HOURS:
			case DAYS:
				return new TimeUnit(amount, unit, estimated);

			case JOULE_PER_KG_CELSIUS:
				return new ArbitraryPhysicalQuantity(amount, unit);

			case LINTNER:
				return new DiastaticPowerUnit(amount, estimated);

			default:
				throw new BrewdayException("invalid: "+unit);

		}
	}
}
