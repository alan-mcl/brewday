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

package mclachlan.brewday.db;

import java.io.*;
import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.ReflectiveSerialiser;
import mclachlan.brewday.db.v2.SimpleSilo;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 *
 */
public class Database
{
	private static Database instance = new Database();

	private Map<String, Recipe> recipes;
	private Map<String, Recipe> processTemplates;
	private Map<String, EquipmentProfile> equipmentProfiles;

	private SimpleSilo<EquipmentProfile> equipmentSilo;
	private SimpleSilo<Recipe> recipeSilo;
	private SimpleSilo<Recipe> processTemplateSilo;

	// reference data
	private Map<String, Hop> hops;
	private Map<String, Fermentable> fermentables;
	private Map<String, Yeast> yeasts;
	private Map<String, Misc> miscs;
	private Map<String, Water> waters;
	private Map<String, Style> styles;

	private SimpleSilo<Hop> hopsSilo;
	private SimpleSilo<Fermentable> fermentableSilo;
	private SimpleSilo<Yeast> yeastsSilo;
	private SimpleSilo<Misc> miscsSilo;
	private SimpleSilo<Water> watersSilo;
	private final SimpleSilo<Style> stylesSilo;

	/*-------------------------------------------------------------------------*/
	public Database()
	{
		equipmentSilo = new SimpleSilo<>(new EquipmentProfileSerialiser());
		recipeSilo = new SimpleSilo<>(new RecipeSerialiser());
		processTemplateSilo = new SimpleSilo<>(new RecipeSerialiser());

		hopsSilo = new SimpleSilo<>(
			new ReflectiveSerialiser<>(
				Hop.class,
				"name",
				"description",
				"alphaAcid",
				"hopStorageIndex",
				"type",
				"betaAcid",
				"substitutes",
				"origin",
				"humulene",
				"caryophyllene",
				"cohumulone",
				"myrcene"));

		fermentableSilo = new SimpleSilo<>(
			new ReflectiveSerialiser<>(
				Fermentable.class,
				"name",
				"description",
				"type",
				"colour",
				"origin",
				"supplier",
				"yield",
				"addAfterBoil",
				"coarseFineDiff",
				"moisture",
				"diastaticPower",
				"protein",
				"maxInBatch",
				"recommendMash",
				"ibuGalPerLb"));

		yeastsSilo = new SimpleSilo<>(
			new ReflectiveSerialiser<>(
				Yeast.class,
				"name",
				"description",
				"type",
				"form",
				"laboratory",
				"productId",
				"minTemp",
				"maxTemp",
				"flocculation",
				"attenuation",
				"recommendedStyles"));

		miscsSilo = new SimpleSilo<>(
			new ReflectiveSerialiser<>(
				Misc.class,
				"name",
				"description",
				"type",
				"use",
				"usageRecommendation"));

		watersSilo = new SimpleSilo<>(
			new ReflectiveSerialiser<>(
				Water.class,
				"name",
				"description",
				"calcium",
				"bicarbonate",
				"sulfate",
				"chloride",
				"sodium",
				"magnesium",
				"ph"));

		stylesSilo = new SimpleSilo<Style>(
			new ReflectiveSerialiser<Style>(
				Style.class,
				"name",
				"styleGuideName",
				"category",
				"categoryNumber",
				"styleLetter",
				"styleGuide",
				"type",
				"ogMin",
				"ogMax",
				"fgMin",
				"fgMax",
				"ibuMin",
				"ibuMax",
				"colourMin",
				"colourMax",
				"carbMin",
				"carbMax",
				"abvMin",
				"abvMax",
				"notes",
				"profile",
				"ingredients",
				"examples"));
	}

	/*-------------------------------------------------------------------------*/
	public void loadAll()
	{
		try
		{
			fermentables = fermentableSilo.load(new BufferedReader(new FileReader("db/fermentables.json")));
			hops = hopsSilo.load(new BufferedReader(new FileReader("db/hops.json")));
			yeasts = yeastsSilo.load(new BufferedReader(new FileReader("db/yeasts.json")));
			miscs = miscsSilo.load(new BufferedReader(new FileReader("db/miscs.json")));
			waters = watersSilo.load(new BufferedReader(new FileReader("db/waters.json")));
			styles = stylesSilo.load(new BufferedReader(new FileReader("db/styles.json")));

			processTemplates = processTemplateSilo.load(new BufferedReader(new FileReader("db/processtemplates.json")));
			equipmentProfiles = equipmentSilo.load(new BufferedReader(new FileReader("db/equipmentprofiles.json")));
			recipes = recipeSilo.load(new BufferedReader(new FileReader("db/recipes.json")));
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void saveAll()
	{
		try
		{
			processTemplateSilo.save(
				new BufferedWriter(new FileWriter("db/processtemplates.json")),
				this.processTemplates);

			equipmentSilo.save(
				new BufferedWriter(new FileWriter("db/equipmentprofiles.json")),
				this.equipmentProfiles);

			recipeSilo.save(
				new BufferedWriter(new FileWriter("db/recipes.json")),
				this.recipes);
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
	public Map<String, Hop> getHops()
	{
		return hops;
	}

	public Map<String, Fermentable> getFermentables()
	{
		return fermentables;
	}

	public Map<String, Yeast> getYeasts()
	{
		return yeasts;
	}

	public Map<String, Misc> getMiscs()
	{
		return miscs;
	}

	public Map<String, Water> getWaters()
	{
		return waters;
	}

	public Map<String, Style> getStyles()
	{
		return styles;
	}

	public Map<String, EquipmentProfile> getEquipmentProfiles()
	{
		return equipmentProfiles;
	}
}
