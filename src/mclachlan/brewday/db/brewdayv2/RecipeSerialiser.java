package mclachlan.brewday.db.brewdayv2;

import java.util.*;
import mclachlan.brewday.db.v2.V2Serialiser;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class RecipeSerialiser implements V2Serialiser<Recipe>
{
	private StepSerialiser stepSerialiser = new StepSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Recipe recipe)
	{
		Map result = new HashMap();

		result.put("name", recipe.getName());
		result.put("steps",
			V2Utils.serialiseList(recipe.getSteps(), stepSerialiser));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Recipe fromMap(Map<String, ?> map)
	{
		String name = (String)map.get("name");
		List<ProcessStep> steps = V2Utils.deserialiseList(
			(List)map.get("steps"), stepSerialiser);

		return new Recipe(name, steps);
	}
}
