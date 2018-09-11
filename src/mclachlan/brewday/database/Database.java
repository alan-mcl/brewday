package mclachlan.brewday.database;

import java.util.*;
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.test.ProcessRunner;

/**
 *
 */
public class Database
{
	private static Database instance = new Database();

	public static Database getInstance()
	{
		return instance;
	}

	public Map<String, Batch> getBatches()
	{
		Map<String, Batch> result = new HashMap<String, Batch>();

		result.put("Test Batch 1", ProcessRunner.getBatch());

		return result;
	}
}
