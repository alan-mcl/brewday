package mclachlan.brewday.db.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class PropertiesSilo implements V2SiloSingleton<Properties>
{
	@Override
	public Properties load(BufferedReader reader) throws IOException
	{
		Properties result = new Properties();
		result.load(reader);
		return result;
	}

	@Override
	public void save(BufferedWriter writer, Properties obj) throws IOException
	{
		obj.store(writer, null);
	}
}
