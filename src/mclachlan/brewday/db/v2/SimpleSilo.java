package mclachlan.brewday.db.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class SimpleSilo<V extends V2DataObject> implements V2SiloMap<V>
{
	private V2SerialiserMap<V> serialiser;

	/*-------------------------------------------------------------------------*/
	public SimpleSilo(V2SerialiserMap<V> serialiser)
	{
		this.serialiser = serialiser;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, V> load(BufferedReader reader) throws IOException
	{
		Map<String, V> result = new HashMap<String, V>();

		List<Map> objects = V2Utils.getObjects(reader);

		for (Map map : objects)
		{
			V v = (V)serialiser.fromMap(map);

			result.put(v.getName(), v);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void save(BufferedWriter writer, Map<String, V> map) throws IOException
	{
		List<Map> list = new ArrayList<>();
		for (V v : map.values())
		{
			list.add(serialiser.toMap(v));
		}

		V2Utils.writeJson(list, writer);
	}
}
