/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.Settings.MashPhModel;
import mclachlan.brewday.process.PhVolumes;
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
		Object legacyBitterness = null;
		Object legacyPh = null;
		Map<Volume.Metric, Object> metricsMap = new HashMap<>();
		for (Map.Entry<String, Object> e : stringMap.entrySet())
		{
			if ("BITTERNESS".equals(e.getKey()))
			{
				legacyBitterness = e.getValue();
				continue;
			}
			if ("PH".equals(e.getKey()))
			{
				legacyPh = e.getValue();
				continue;
			}
			metricsMap.put(Volume.Metric.valueOf(e.getKey()), e.getValue());
		}

		Settings settings = db.getSettings();

		if (legacyBitterness != null && !hasPerFormulaBitterness(metricsMap))
		{
			Settings.migrateLegacyHopBitternessSettings(settings.getSettings());
			List<HopBitternessFormula> formulas = Settings.parseReportedFormulas(settings);
			HopBitternessFormula target = formulas.get(0);
			metricsMap.put(target.toMetric(), legacyBitterness);
		}

		if (legacyPh != null && !PhVolumes.hasPerModelPh(metricsMap))
		{
			Settings.migrateLegacyMashPhSettings(settings.getSettings());
			List<MashPhModel> models = Settings.parseReportedModels(settings);
			MashPhModel target = models.get(0);
			metricsMap.put(target.toMetric(), legacyPh);
		}

		Map metrics = V2Utils.deserialiseMap(metricsMap, quantitySerialiser, db);

		List list = (List)map.get("ingredientAdditions");

		List ingredients = V2Utils.deserialiseList(list, ingredientAdditionSerialiser, db);

		return new Volume(name, type, metrics, ingredients);
	}

	/*-------------------------------------------------------------------------*/
	private static boolean hasPerFormulaBitterness(Map<Volume.Metric, Object> metricsMap)
	{
		for (Volume.Metric m : metricsMap.keySet())
		{
			if (Settings.HopBitternessFormula.isBitternessMetric(m))
			{
				return true;
			}
		}
		return false;
	}
}
