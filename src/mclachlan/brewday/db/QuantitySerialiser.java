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
	@Override
	public Map<String, ?> toMap(Quantity quantity)
	{
		if (quantity.isEstimated())
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

			case GRAMS_PER_LITRE:
			case VOLUMES:
				return new CarbonationUnit(amount, unit, estimated);

			case KPA:
			case PSI:
				return new PressureUnit(amount, unit, estimated);

			default:
				throw new BrewdayException("invalid: "+unit);

		}
	}
}
