package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.db.v2.V2SerialiserObject;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.Quantity;

/**
 *
 */
public class ArbitraryPhysicalQuantitySerialiser implements V2SerialiserObject<ArbitraryPhysicalQuantity>
{
	@Override
	public Object toObj(ArbitraryPhysicalQuantity quantity)
	{
		Map<String, String> result = new HashMap<>();
		result.put("amount", String.valueOf(quantity.get()));
		result.put("unit", quantity.getUnit().name());
		return result;
	}

	@Override
	public ArbitraryPhysicalQuantity fromObj(Object obj)
	{
		Map<String, String> map = (Map)obj;

		double amount = Double.valueOf(map.get("amount"));
		Quantity.Unit unit = Quantity.Unit.valueOf(map.get("unit"));

		return new ArbitraryPhysicalQuantity(amount, unit);
	}
}
