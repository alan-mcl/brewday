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
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class VolumeSerialiser implements V2SerialiserMap<Volume>
{
	private final QuantitySerialiser quantitySerialiser =
		new QuantitySerialiser(true);

	private final IngredientAdditionSerialiser ingredientAdditionSerialiser =
		new IngredientAdditionSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Volume volume, Database db)
	{
		Map result = new HashMap();

		result.put("name", volume.getName());
		result.put("type", volume.getType().name());

		result.put("metrics",
			V2Utils.serialiseMap(volume.getMetrics(), quantitySerialiser, db));

		result.put("ingredientAdditions",
			V2Utils.serialiseList(volume.getIngredientAdditions(), ingredientAdditionSerialiser, db));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Volume fromMap(Map<String, ?> map, Database db)
	{
		String name = (String)map.get("name");
		Volume.Type type = Volume.Type.valueOf((String)map.get("type"));

		Map<String, Object> stringMap = (Map<String, Object>)map.get("metrics");
		Map<Volume.Metric, Object> metricsMap = new HashMap<>();
		for (Map.Entry<String, Object> e : stringMap.entrySet())
		{
			metricsMap.put(Volume.Metric.valueOf(e.getKey()), e.getValue());
		}

		Map metrics = V2Utils.deserialiseMap(metricsMap, quantitySerialiser, db);

		List list = (List)map.get("ingredientAdditions");

		List ingredients = V2Utils.deserialiseList(list, ingredientAdditionSerialiser, db);

		return new Volume(name, type, metrics, ingredients);
	}
}
