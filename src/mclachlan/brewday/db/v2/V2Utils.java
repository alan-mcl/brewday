package mclachlan.brewday.db.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.*;

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
	public static Map getMap(BufferedReader reader)
	{
		Gson gson = new Gson();
		Type type = new TypeToken<Map>(){}.getType();
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
	public static String getJson(Map obj)
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Type type = new TypeToken<Map>(){}.getType();
		return gson.toJson(obj, type);
	}

	/*-------------------------------------------------------------------------*/
	public static void writeJson(Map obj, Writer writer) throws IOException
	{
		writer.write(getJson(obj));
		writer.flush();
	}

	/*-------------------------------------------------------------------------*/
	public static Map serialiseMap(Map<?,?> map, V2SerialiserMap serialiser)
	{
		Map result = new HashMap();

		for (Map.Entry<?,?> e : map.entrySet())
		{
			if (e.getValue() != null)
			{
				result.put(e.getKey(), serialiser.toMap(e.getValue()));
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static Map deserialiseMap(Map<?, ?> map, V2SerialiserMap serialiser)
	{
		Map result = new HashMap();

		for (Map.Entry<?, ?> e : map.entrySet())
		{
			result.put(e.getKey(), serialiser.fromMap((Map<String, ?>)e.getValue()));
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static List serialiseList(List list, V2SerialiserMap serialiser)
	{
		List result = new ArrayList();

		for (Object item : list)
		{
			if (item != null)
			{
				result.add(serialiser.toMap(item));
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static List deserialiseList(List list, V2SerialiserMap serialiser)
	{
		List result = new ArrayList();

		for (Object item : list)
		{
			result.add(serialiser.fromMap((Map)item));
		}

		return result;
	}
}
