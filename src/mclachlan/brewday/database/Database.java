/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

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
