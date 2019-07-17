package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class IngredientSerialiser implements V2SerialiserMap<IngredientAddition>
{
	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(IngredientAddition ingredientAddition)
	{
		Map result = new HashMap();

		result.put("name", ingredientAddition.getName());
		result.put("weight", ingredientAddition.getWeight());
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
				result.put("temperature",
					((WaterAddition)ingredientAddition).getTemperature());
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
		Double weight = (Double)map.get("weight");
		Double time = (Double)map.get("time");
		IngredientAddition.Type type = IngredientAddition.Type.valueOf((String)map.get("type"));

		switch (type)
		{
			case FERMENTABLES:
				return new FermentableAddition(
					Database.getInstance().getFermentables().get((String)map.get("fermentable")),
					weight,
					time);
			case HOPS:
				return new HopAddition(
					Database.getInstance().getHops().get((String)map.get("hop")),
					weight,
					time);
			case WATER:
				return new WaterAddition(
					name,
					weight,
					(Double)map.get("temperature"),
					time);
			case YEAST:
				return new YeastAddition(
					Database.getInstance().getYeasts().get((String)map.get("yeast")),
					weight,
					time);
			case MISC:
				return new MiscAddition(
					Database.getInstance().getMiscs().get((String)map.get("misc")),
					weight,
					time);
			default:
				throw new BrewdayException("Invalid type "+type);
		}
	}
}
