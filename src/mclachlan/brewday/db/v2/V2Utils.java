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
	public static List serialiseList(List list, V2SerialiserMap serialiser)
	{
		List result = new ArrayList();

		for (Object item : list)
		{
			result.add(serialiser.toMap(item));
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
