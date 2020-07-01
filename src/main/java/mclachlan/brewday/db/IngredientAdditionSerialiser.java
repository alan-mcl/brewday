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
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class IngredientAdditionSerialiser implements V2SerialiserMap<IngredientAddition>
{
	QuantitySerialiser quantitySerialiser = new QuantitySerialiser(false);

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(IngredientAddition ingredientAddition)
	{
		Map result = new HashMap();

		result.put("name", ingredientAddition.getName());
		result.put("quantity", quantitySerialiser.toMap(ingredientAddition.getQuantity()));
		result.put("time", ingredientAddition.getTime().get(Quantity.Unit.SECONDS));
		result.put("type", ingredientAddition.getType().name());

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
				result.put("misc",
					((MiscAddition)ingredientAddition).getMisc().getName());
				break;
			default:
				throw new BrewdayException("Invalid type "+ingredientAddition.getType());
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public IngredientAddition fromMap(Map<String, ?> map)
	{
		String name = (String)map.get("name");
		TimeUnit time = new TimeUnit((Double)map.get("time"), Quantity.Unit.SECONDS, false);
		IngredientAddition.Type type = IngredientAddition.Type.valueOf((String)map.get("type"));
		Quantity quantity = quantitySerialiser.fromMap((Map<String, ?>)map.get("quantity"));

		switch (type)
		{
			case FERMENTABLES:
				return new FermentableAddition(
					Database.getInstance().getFermentables().get((String)map.get("fermentable")),
					(WeightUnit)quantity,
					time);
			case HOPS:
				return new HopAddition(
					Database.getInstance().getHops().get((String)map.get("hop")),
					(WeightUnit)quantity,
					time);
			case WATER:
				return new WaterAddition(
					Database.getInstance().getWaters().get((String)map.get("water")),
					(VolumeUnit)quantity,
					new TemperatureUnit((Double)map.get("temperature"), Quantity.Unit.CELSIUS, false),
					time);
			case YEAST:
				return new YeastAddition(
					Database.getInstance().getYeasts().get((String)map.get("yeast")),
					(WeightUnit)quantity,
					time);
			case MISC:
				return new MiscAddition(
					Database.getInstance().getMiscs().get((String)map.get("misc")),
					(WeightUnit)quantity,
					time);
			default:
				throw new BrewdayException("Invalid type "+type);
		}
	}
}
