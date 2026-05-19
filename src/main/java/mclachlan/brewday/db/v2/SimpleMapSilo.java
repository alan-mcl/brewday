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

package mclachlan.brewday.db.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import mclachlan.brewday.db.Database;

/**
 *
 */
public class SimpleMapSilo<V extends V2DataObject> implements V2SiloMap<V>
{
	private V2SerialiserMap<V> serialiser;

	/*-------------------------------------------------------------------------*/
	public SimpleMapSilo(V2SerialiserMap<V> serialiser)
	{
		this.serialiser = serialiser;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, V> load(BufferedReader reader,
		Database db) throws IOException
	{
		Map<String, V> result = new HashMap<String, V>();

		List<Map> objects = V2Utils.getObjects(reader);

		for (Map map : objects)
		{
			V v = (V)serialiser.fromMap(map, db);

			result.put(v.getName(), v);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void save(BufferedWriter writer, Map<String, V> map,
		Database db) throws IOException
	{
		List<V> values = new ArrayList<>(map.values());
		values.sort(Comparator.comparing(V2DataObject::getName));

		List<Map> list = new ArrayList<>();
		for (V v : values)
		{
			list.add(serialiser.toMap(v, db));
		}

		V2Utils.writeJson(list, writer);
	}
}
