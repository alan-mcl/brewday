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
		if (obj == null)
		{
			return null;
		}

		Map<String, String> map = (Map)obj;

		double amount = Double.valueOf(map.get("amount"));
		Quantity.Unit unit = Quantity.Unit.valueOf(map.get("unit"));

		return new ArbitraryPhysicalQuantity(amount, unit);
	}
}
