package mclachlan.brewday.db.v2;

import java.io.IOException;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 *
 */
public class MapSingletonSilo implements V2SiloSingleton<Map>
{
	@Override
	public Map load(BufferedReader reader) throws IOException
	{
		return (Map)V2Utils.getMap(reader);
	}

	@Override
	public void save(BufferedWriter writer, Map obj) throws IOException
	{
		V2Utils.writeJson((Map)obj, writer);
	}
}
