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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.ReflectiveSerialiser;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.*;

import static mclachlan.brewday.math.Quantity.Unit.PERCENTAGE;
import static mclachlan.brewday.math.Quantity.Unit.SECONDS;

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
		result.put("type", ingredientAddition.getType().name());
		result.put("unit", ingredientAddition.getUnit().name());

		IngredientAddition.Type type = ingredientAddition.getType();
		if (type != IngredientAddition.Type.YEAST && type != IngredientAddition.Type.YEAST_CULTURE)
		{
			result.put("time", ingredientAddition.getTime().get(SECONDS));
		}

		switch (type)
		{
			case FERMENTABLES:
				result.put("fermentable",
					((FermentableAddition)ingredientAddition).getFermentable().getName());
				break;
			case HOPS:
				HopAddition ha = (HopAddition)ingredientAddition;
				result.put("hop", ha.getHop().getName());
				if (ha.getFormOverride() != null)
				{
					result.put("form", ha.getFormOverride().name());
				}
				break;
			case WATER:
				Water water = ((WaterAddition)ingredientAddition).getWater();
				String waterName = water.getName();

				if (db.getWaters().get(waterName) == null)
				{
					// this is probably a combined water profile
					// store out all the details directly

					ReflectiveSerialiser<Water> waterSerialiser = db.getWaterSerialiser();

					Map map = waterSerialiser.toMap(water, db);

					result.putAll(map);

					result.put("isCombinedWater", "true");
				}
				else
				{
					result.put("water", waterName);
					result.put("isCombinedWater", "false");
				}

				result.put("temperature",
					((WaterAddition)ingredientAddition).getTemperature().get(Quantity.Unit.CELSIUS));

				break;
			case YEAST:
			{
				YeastAddition yeastAddition = (YeastAddition)ingredientAddition;
				result.put("yeast", yeastAddition.getYeast().getName());
				if (yeastAddition.getAddToSecondary())
				{
					result.put("addToSecondary", "true");
				}
				break;
			}
			case YEAST_CULTURE:
			{
				YeastCulture culture = (YeastCulture)ingredientAddition;
				result.put("yeast", culture.getYeast().getName());
				if (culture.getCellCount() != 0)
				{
					result.put("cellCount", culture.getCellCount());
				}
				if (culture.getViability() != null)
				{
					result.put("viability", culture.getViability().get(PERCENTAGE));
				}
				if (culture.getGeneration() != 0)
				{
					result.put("generation", culture.getGeneration());
				}
				if (culture.getActivityState() != YeastActivityState.ACTIVE)
				{
					result.put("activityState", culture.getActivityState().name());
				}
				if (culture.getSourceType() != YeastSourceType.DIRECT_PITCH)
				{
					result.put("sourceType", culture.getSourceType().name());
				}
				break;
			}
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
		TimeUnit time = readTime(map);
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
				HopAddition hopAdd = new HopAddition(
					db.getHops().get((String)map.get("hop")),
					quantity,
					unit,
					time);
				if (map.get("form") != null)
				{
					hopAdd.setForm(Hop.Form.fromString((String)map.get("form")));
				}
				result = hopAdd;
				break;

			case WATER:

				TemperatureUnit temp = new TemperatureUnit((Double)map.get("temperature"), Quantity.Unit.CELSIUS, false);
				Water water;

				if (Boolean.parseBoolean((String)map.get("isCombinedWater")))
				{
					water = db.getWaterSerialiser().fromMap(map, db);
				}
				else
				{
					water = db.getWaters().get((String)map.get("water"));
					if (water == null)
					{
						throw new BrewdayException(
							"Water profile not found: \"" + map.get("water")
							+ "\" (referenced by ingredient addition \""
							+ name + "\")");
					}
				}

				result = new WaterAddition(water, (VolumeUnit)quantity, unit, temp, time);

				break;

			case YEAST:
			{
				YeastAddition yeastAddition = new YeastAddition(
					db.getYeasts().get((String)map.get("yeast")),
					quantity,
					unit,
					time);
				if (Boolean.parseBoolean(String.valueOf(map.get("addToSecondary"))))
				{
					yeastAddition.setAddToSecondary(true);
				}
				result = yeastAddition;
				break;
			}

			case YEAST_CULTURE:
				result = new YeastCulture(
					db.getYeasts().get((String)map.get("yeast")),
					quantity,
					unit,
					readLong(map, "cellCount", 0L),
					readViability(map),
					readInt(map, "generation", 0),
					readActivityState(map),
					readSourceType(map));
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

	/*-------------------------------------------------------------------------*/
	private static TimeUnit readTime(Map<String, ?> map)
	{
		Double timeSeconds = (Double)map.get("time");
		if (timeSeconds == null)
		{
			return new TimeUnit(0, SECONDS, false);
		}
		return new TimeUnit(timeSeconds, SECONDS, false);
	}

	/*-------------------------------------------------------------------------*/
	private static long readLong(Map<String, ?> map, String key, long defaultValue)
	{
		Object value = map.get(key);
		if (value == null)
		{
			return defaultValue;
		}
		if (value instanceof Double)
		{
			return ((Double)value).longValue();
		}
		if (value instanceof Number)
		{
			return ((Number)value).longValue();
		}
		return defaultValue;
	}

	/*-------------------------------------------------------------------------*/
	private static int readInt(Map<String, ?> map, String key, int defaultValue)
	{
		Object value = map.get(key);
		if (value == null)
		{
			return defaultValue;
		}
		if (value instanceof Double)
		{
			return ((Double)value).intValue();
		}
		if (value instanceof Number)
		{
			return ((Number)value).intValue();
		}
		return defaultValue;
	}

	/*-------------------------------------------------------------------------*/
	private static PercentageUnit readViability(Map<String, ?> map)
	{
		Double viability = (Double)map.get("viability");
		return viability == null ? null : new PercentageUnit(viability, false);
	}

	/*-------------------------------------------------------------------------*/
	private static YeastActivityState readActivityState(Map<String, ?> map)
	{
		String value = (String)map.get("activityState");
		return value == null ? YeastActivityState.ACTIVE : YeastActivityState.valueOf(value);
	}

	/*-------------------------------------------------------------------------*/
	private static YeastSourceType readSourceType(Map<String, ?> map)
	{
		String value = (String)map.get("sourceType");
		return value == null ? YeastSourceType.DIRECT_PITCH : YeastSourceType.valueOf(value);
	}
}
