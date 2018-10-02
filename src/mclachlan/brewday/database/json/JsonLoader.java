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

package mclachlan.brewday.database.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import mclachlan.brewday.database.beerxml.ImportXml;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.process.Recipe;

/**
 *
 */
public class JsonLoader
{
	public Map<String, Recipe> loadRecipes() throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.enableDefaultTyping();

		BufferedReader reader;
		File file = new File("db/recipes.json");
		reader = new BufferedReader(new FileReader(file));

		StringBuilder sb = new StringBuilder();
		String line = reader.readLine();
		while (line != null && !line.equals("@"))
		{
			sb.append(line).append("\n");
			line = reader.readLine();
		}

		Recipe[] list = mapper.readValue(sb.toString(), Recipe[].class);

		Map<String, Recipe> result = new HashMap<String, Recipe>();
		for (Recipe r : list)
		{
			result.put(r.getName(), r);
		}

		return result;
	}

	public Map<String, Hop> getReferenceHops()
	{
		Map<String, Hop> result = new HashMap<String, Hop>();
		List<Hop> hops = new ImportXml("beerxml/hops.xml", "hops").beerXmlHopsHandler.getResult();

		for (Hop h : hops)
		{
			result.put(h.getName(), h);
		}

		return result;
	}

	public Map<String, Fermentable> getReferenceFermentables()
	{
		Map<String, Fermentable> result = new HashMap<String, Fermentable>();
		List<Fermentable> ferms = new ImportXml("beerxml/fermentables.xml", "fermentables").
			beerXmlFermentablesHandler.getResult();

		for (Fermentable f : ferms)
		{
			result.put(f.getName(), f);
		}

		return result;
	}

	public Map<String, Yeast> getReferenceYeasts()
	{
		Map<String, Yeast> result = new HashMap<String, Yeast>();
		List<Yeast> ferms = new ImportXml("beerxml/yeasts.xml", "yeasts").
			beerXmlYeastsHandler.getResult();

		for (Yeast f : ferms)
		{
			result.put(f.getName(), f);
		}

		return result;
	}

	public Map<String, Misc> getReferenceMiscs()
	{
		Map<String, Misc> result = new HashMap<String, Misc>();
		List<Misc> ferms = new ImportXml("beerxml/miscs.xml", "miscs").
			beerXmlMiscsHandler.getResult();

		for (Misc f : ferms)
		{
			result.put(f.getName(), f);
		}

		return result;
	}

	public Map<String, Water> getReferenceWaters()
	{
		Map<String, Water> result = new HashMap<String, Water>();
		List<Water> ferms = new ImportXml("beerxml/waters.xml", "waters").
			beerXmlWatersHandler.getResult();

		for (Water f : ferms)
		{
			result.put(f.getName(), f);
		}

		return result;
	}
}
