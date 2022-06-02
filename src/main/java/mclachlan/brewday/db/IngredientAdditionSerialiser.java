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
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class IngredientAdditionSerialiser implements V2SerialiserMap<IngredientAddition>
{
	private final QuantitySerialiser quantitySerialiser = new QuantitySerialiser(false);

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(IngredientAddition ingredientAddition,
		Database db)
	{
		Map result = new HashMap();

		result.put("name", ingredientAddition.getName());
		result.put("quantity", quantitySerialiser.toMap(ingredientAddition.getQuantity(), db));
		result.put("time", ingredientAddition.getTime().get(Quantity.Unit.SECONDS));
		result.put("type", ingredientAddition.getType().name());
		result.put("unit", ingredientAddition.getUnit().name());

		switch (ingredientAddition.getType())
		{
			case FERMENTABLES:
				result.put("fermentable",
					((FermentableAddition)ingredientAddition).getFermentable().getName());
				break;
			case HOPS:
				result.put("hop",
					((HopAddition)ingredientAddition).getHop().getName());
				break;
			case WATER:
				result.put("water",
					((WaterAddition)ingredientAddition).getWater().getName());
				result.put("temperature",
					((WaterAddition)ingredientAddition).getTemperature().get(Quantity.Unit.CELSIUS));
				break;
			case YEAST:
				result.put("yeast",
					((YeastAddition)ingredientAddition).getYeast().getName());
				break;
			case MISC:
				result.put("misc", ((MiscAddition)ingredientAddition).getMisc().getName());
				break;
			default:
				throw new BrewdayException("Invalid type "+ingredientAddition.getType());
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public IngredientAddition fromMap(Map<String, ?> map,
		Database db)
	{
		String name = (String)map.get("name");
		TimeUnit time = new TimeUnit((Double)map.get("time"), Quantity.Unit.SECONDS, false);
		IngredientAddition.Type type = IngredientAddition.Type.valueOf((String)map.get("type"));
		Quantity quantity = quantitySerialiser.fromMap((Map<String, ?>)map.get("quantity"), db);
		Quantity.Unit unit;
		if (map.get("unit") == null)
		{
			unit = quantity.getUnit();
		}
		else
		{
			unit = Quantity.Unit.valueOf((String)map.get("unit"));
		}

		IngredientAddition result;

		switch (type)
		{
			case FERMENTABLES:
				result = new FermentableAddition(
					db.getFermentables().get((String)map.get("fermentable")),
					quantity,
					unit,
					time);
				break;

			case HOPS:
				result = new HopAddition(
					db.getHops().get((String)map.get("hop")),
					quantity,
					unit,
					time);
				break;

			case WATER:
				result = new WaterAddition(
					db.getWaters().get((String)map.get("water")),
					(VolumeUnit)quantity,
					unit,
					new TemperatureUnit((Double)map.get("temperature"), Quantity.Unit.CELSIUS, false),
					time);
				break;

			case YEAST:
				result = new YeastAddition(
					db.getYeasts().get((String)map.get("yeast")),
					quantity,
					unit,
					time);
				break;

			case MISC:
				result = new MiscAddition(
					db.getMiscs().get((String)map.get("misc")),
					quantity,
					unit,
					time);
				break;

			default:
				throw new BrewdayException("Invalid type "+type);
		}

		if (unit != null)
		{
			result.setUnit(unit);
		}

		return result;
	}
}
