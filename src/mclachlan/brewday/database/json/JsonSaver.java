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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class JsonSaver
{
	public void saveRecipes(Map<String, Recipe> recipes) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.enableDefaultTyping();

		Recipe[] list = new ArrayList<Recipe>(
			recipes.values()).toArray(
			new Recipe[new ArrayList<Recipe>(recipes.values()).size()]);

		File f = new File("db/recipes.json");
		FileWriter fw = new FileWriter(f);

		try
		{
			fw.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list));
		}
		finally
		{
			fw.flush();
			fw.close();
		}
	}

	public void saveProcessTemplates(Map<String, Recipe> recipes) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.enableDefaultTyping();

		Recipe[] list = new ArrayList<Recipe>(
			recipes.values()).toArray(
			new Recipe[new ArrayList<Recipe>(recipes.values()).size()]);

		File f = new File("db/process_templates.json");
		FileWriter fw = new FileWriter(f);

		try
		{
			fw.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list));
		}
		finally
		{
			fw.flush();
			fw.close();
		}
	}

}
