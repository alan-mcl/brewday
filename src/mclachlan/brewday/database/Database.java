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
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.process.Recipe;

/**
 *
 */
public class Database
{
	private HardcodedLoader loader = new HardcodedLoader();
	private static Database instance = new Database();

	public static Database getInstance()
	{
		return instance;
	}

	/*-------------------------------------------------------------------------*/
	public Map<String, Recipe> getBatches()
	{
		return loader.getBatches();
	}

	/*-------------------------------------------------------------------------*/
	public Map<String, Hop> getReferenceHops()
	{
		return loader.getReferenceHops();
	}

	public Map<String, Fermentable> getReferenceFermentables()
	{
		return loader.getReferenceFermentables();
	}

	public Map<String, Yeast> getReferenceYeasts()
	{
		return loader.getReferenceYeasts();
	}

	public Map<String, Misc> getReferenceMiscs()
	{
		return loader.getReferenceMiscs();
	}

	public Map<String, Water> getReferenceWaters()
	{
		return loader.getReferenceWaters();
	}
}
