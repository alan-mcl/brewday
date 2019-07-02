package mclachlan.brewday.db.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import mclachlan.brewday.database.Database;
import mclachlan.brewday.db.brewdayv2.RecipesSilo;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class V2Utils
{
	/*-------------------------------------------------------------------------*/
	public static List<Map> getObjects(BufferedReader reader)
	{
		Gson gson = new Gson();
		Type type = new TypeToken<List<Map>>(){}.getType();
		return gson.fromJson(new JsonReader(reader), type);
	}

	/*-------------------------------------------------------------------------*/
	public static String getJson(List<Map> list)
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Type type = new TypeToken<List<Map>>(){}.getType();
		return gson.toJson(list, type);
	}

	/*-------------------------------------------------------------------------*/
	public static void writeJson(List<Map> list, Writer writer) throws IOException
	{
		writer.write(getJson(list));
		writer.flush();
	}

	/*-------------------------------------------------------------------------*/
	public static List serialiseList(List list, V2Serialiser serialiser)
	{
		List result = new ArrayList();

		for (Object item : list)
		{
			result.add(serialiser.toMap(item));
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static List deserialiseList(List list, V2Serialiser serialiser)
	{
		List result = new ArrayList();

		for (Object item : list)
		{
			result.add(serialiser.fromMap((Map)item));
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
/*
		List<Map> objects = getObjects(new BufferedReader(new FileReader("db/recipes2.json")));
		System.out.println("objects = [" + objects + "]");
		String json = getJson(objects);
		System.out.println("json = [" + json + "]");
*/
		Database.getInstance().loadAll();
//		Map<String, Recipe> recipes = Database.getInstance().getRecipes();
//
//		System.out.println("recipes = [" + recipes + "]");

		RecipesSilo rs = new RecipesSilo();
		Map<String, Recipe> load = rs.load(new BufferedReader(new FileReader("db/recipes2.json")));

		System.out.println("load = [" + load + "]");

		rs.save(new BufferedWriter(new FileWriter("db/recipes2.json")), load);


	}
}
