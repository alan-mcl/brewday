package mclachlan.brewday.db.brewdayv2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import mclachlan.brewday.db.v2.V2Serialiser;
import mclachlan.brewday.db.v2.V2SiloMap;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class RecipesSilo implements V2SiloMap<Recipe>
{
	private V2Serialiser<Recipe> recipeSerialiser = new RecipeSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, Recipe> load(BufferedReader reader) throws IOException
	{
		Map<String, Recipe> result = new HashMap<String, Recipe>();

		List<Map> objects = V2Utils.getObjects(reader);

		for (Map map : objects)
		{
			Recipe recipe = recipeSerialiser.fromMap(map);

			result.put(recipe.getName(), recipe);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void save(BufferedWriter writer, Map<String, Recipe> map) throws IOException
	{
		List<Map> list = new ArrayList<>();
		for (Recipe recipe : map.values())
		{
			list.add(recipeSerialiser.toMap(recipe));
		}

		V2Utils.writeJson(list, writer);
	}
}
