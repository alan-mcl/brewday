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

import java.io.*;
import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.database.json.JsonLoader;
import mclachlan.brewday.database.json.JsonSaver;
import mclachlan.brewday.db.brewdayv2.EquipmentProfileSerialiser;
import mclachlan.brewday.db.v2.SimpleSilo;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Database
{
	private JsonLoader loader;
	private JsonSaver saver;
	private static Database instance = new Database();

	private Map<String, Recipe> recipes;
	private Map<String, Recipe> processTemplates;
	private Map<String, EquipmentProfile> equipmentProfiles;

	SimpleSilo<EquipmentProfile> equipmentSilo = new SimpleSilo<EquipmentProfile>(
		new EquipmentProfileSerialiser());


	/*-------------------------------------------------------------------------*/
	public void loadAll()
	{
		loader = new JsonLoader();

		try
		{
			recipes = loader.loadRecipes();
			processTemplates = loader.getProcessTemplates();
			equipmentProfiles = equipmentSilo.load(new BufferedReader(new FileReader("db/equipmentprofiles.json")));
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void saveAll()
	{
		saver = new JsonSaver();

		try
		{
			saver.saveRecipes(this.recipes);
			saver.saveProcessTemplates(this.processTemplates);
			equipmentSilo.save(
				new BufferedWriter(new FileWriter("db/equipmentprofiles.json")),
				this.equipmentProfiles);
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static Database getInstance()
	{
		return instance;
	}

	/*-------------------------------------------------------------------------*/
	public Map<String, Recipe> getRecipes()
	{
		return recipes;
	}

	public Map<String, Recipe> getProcessTemplates()
	{
		return processTemplates;
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

	public Map<String, EquipmentProfile> getEquipmentProfiles()
	{
		return equipmentProfiles;
	}
}
