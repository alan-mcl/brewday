package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class IngredientAdditionSerialiser implements V2SerialiserMap<IngredientAddition>
{
	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(IngredientAddition ingredientAddition)
	{
		Map result = new HashMap();

		result.put("name", ingredientAddition.getName());
		result.put("weight", ingredientAddition.getWeight().get(Quantity.Unit.GRAMS));
		result.put("time", ingredientAddition.getTime());
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
		Double time = (Double)map.get("time");
		IngredientAddition.Type type = IngredientAddition.Type.valueOf((String)map.get("type"));

		switch (type)
		{
			case FERMENTABLES:
				WeightUnit weight = new WeightUnit((Double)map.get("weight"), Quantity.Unit.GRAMS, false);
				return new FermentableAddition(
					Database.getInstance().getFermentables().get((String)map.get("fermentable")),
					weight,
					time);
			case HOPS:
				weight = new WeightUnit((Double)map.get("weight"), Quantity.Unit.GRAMS, false);
				return new HopAddition(
					Database.getInstance().getHops().get((String)map.get("hop")),
					weight,
					time);
			case WATER:
				VolumeUnit vol = new VolumeUnit((Double)map.get("weight"), Quantity.Unit.MILLILITRES, false);
				return new WaterAddition(
					Database.getInstance().getWaters().get((String)map.get("water")),
					vol,
					new TemperatureUnit((Double)map.get("temperature"), Quantity.Unit.CELSIUS, false),
					time);
			case YEAST:
				weight = new WeightUnit((Double)map.get("weight"), Quantity.Unit.GRAMS, false);
				return new YeastAddition(
					Database.getInstance().getYeasts().get((String)map.get("yeast")),
					weight,
					time);
			case MISC:
				weight = new WeightUnit((Double)map.get("weight"), Quantity.Unit.GRAMS, false);
				return new MiscAddition(
					Database.getInstance().getMiscs().get((String)map.get("misc")),
					weight,
					time);
			default:
				throw new BrewdayException("Invalid type "+type);
		}
	}
}
