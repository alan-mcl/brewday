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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.v2.MapSingletonSilo;
import mclachlan.brewday.db.v2.PropertiesSilo;
import mclachlan.brewday.db.v2.ReflectiveSerialiser;
import mclachlan.brewday.db.v2.SimpleMapSilo;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 *
 */
public class Database
{
	private static Database instance = new Database();

	// non-beery data
	private Settings settings;
	private MapSingletonSilo settingsSilo;

	private Properties uiStrings, processStrings;
	private PropertiesSilo stringsSilo;

	// beery data
	private Map<String, InventoryLineItem> inventory;
	private Map<String, EquipmentProfile> equipmentProfiles;
	private Map<String, Recipe> processTemplates;
	private Map<String, Recipe> recipes;
	private Map<String, Batch> batches;

	private SimpleMapSilo<Recipe> recipeSilo;
	private SimpleMapSilo<Recipe> processTemplateSilo;
	private SimpleMapSilo<EquipmentProfile> equipmentSilo;
	private SimpleMapSilo<InventoryLineItem> inventorySilo;
	private SimpleMapSilo<Batch> batchSilo;

	// reference data
	private Map<String, Hop> hops;
	private Map<String, Fermentable> fermentables;
	private Map<String, Yeast> yeasts;
	private Map<String, Misc> miscs;
	private Map<String, Water> waters;
	private Map<String, Style> styles;

	private SimpleMapSilo<Hop> hopsSilo;
	private SimpleMapSilo<Fermentable> fermentableSilo;
	private SimpleMapSilo<Yeast> yeastsSilo;
	private SimpleMapSilo<Misc> miscsSilo;
	private SimpleMapSilo<Water> watersSilo;
	private final SimpleMapSilo<Style> stylesSilo;

	/*-------------------------------------------------------------------------*/
	public Database()
	{
		settingsSilo = new MapSingletonSilo();
		stringsSilo = new PropertiesSilo();

		recipeSilo = new SimpleMapSilo<>(new RecipeSerialiser());
		processTemplateSilo = new SimpleMapSilo<>(new RecipeSerialiser());
		batchSilo = new SimpleMapSilo<>(new BatchSerialiser());

		ReflectiveSerialiser<InventoryLineItem> inventoryLineItemSerialiser =
			new ReflectiveSerialiser<>(
				InventoryLineItem.class,
				"id",
				"ingredient",
				"type",
				"amount",
				"price");
		inventoryLineItemSerialiser.addCustomSerialiser(
			ArbitraryPhysicalQuantity.class, new ArbitraryPhysicalQuantitySerialiser());
		inventorySilo = new SimpleMapSilo<>(inventoryLineItemSerialiser);

		equipmentSilo = new SimpleMapSilo<>(new ReflectiveSerialiser<>(
			EquipmentProfile.class,
			"name",
			"description",
			"mashEfficiency",
			"mashTunVolume",
			"mashTunWeight",
			"mashTunSpecificHeat",
			"boilKettleVolume",
			"boilEvapourationRate",
			"hopUtilisation",
			"fermenterVolume",
			"lauterLoss",
			"trubAndChillerLoss"));

		hopsSilo = new SimpleMapSilo<>(
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

		fermentableSilo = new SimpleMapSilo<>(
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

		yeastsSilo = new SimpleMapSilo<>(
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

		miscsSilo = new SimpleMapSilo<>(
			new ReflectiveSerialiser<>(
				Misc.class,
				"name",
				"description",
				"type",
				"use",
				"usageRecommendation"));

		watersSilo = new SimpleMapSilo<>(
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

		ReflectiveSerialiser<Style> stylesSerialiser = new ReflectiveSerialiser<>(
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
			"examples");
		stylesSerialiser.addCustomSerialiser(DensityUnit.class, new DensityUnitSerialiser());
		stylesSilo = new SimpleMapSilo<>(stylesSerialiser);
	}

	/*-------------------------------------------------------------------------*/
	public void loadAll()
	{
		try
		{
			settings = new Settings(settingsSilo.load(getFileReader("db/settings.json")));
			uiStrings = stringsSilo.load(getFileReader("strings/ui.properties"));
			processStrings = stringsSilo.load(getFileReader("strings/process.properties"));

			fermentables = fermentableSilo.load(getFileReader("db/fermentables.json"));
			hops = hopsSilo.load(getFileReader("db/hops.json"));
			yeasts = yeastsSilo.load(getFileReader("db/yeasts.json"));
			miscs = miscsSilo.load(getFileReader("db/miscs.json"));
			waters = watersSilo.load(getFileReader("db/waters.json"));
			styles = stylesSilo.load(getFileReader("db/styles.json"));

			inventory = inventorySilo.load(getFileReader("db/inventory.json"));
			processTemplates = processTemplateSilo.load(getFileReader("db/processtemplates.json"));
			equipmentProfiles = equipmentSilo.load(getFileReader("db/equipmentprofiles.json"));
			recipes = recipeSilo.load(getFileReader("db/recipes.json"));
			batches = batchSilo.load(getFileReader("db/batches.json"));
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private BufferedReader getFileReader(String fileName) throws FileNotFoundException
	{
		return new BufferedReader(new FileReader(fileName));
	}

	/*-------------------------------------------------------------------------*/
	public void saveAll()
	{
		StringWriter settingsBuffer = new StringWriter();
		StringWriter inventoryBuffer = new StringWriter();
		StringWriter equipmentBuffer = new StringWriter();
		StringWriter recipesBuffer = new StringWriter();
		StringWriter batchesBuffer = new StringWriter();
		StringWriter processBuffer = new StringWriter();

		try
		{
			// back up the current database
			backupDb();

			// marshall into memory. errors here will not overwrite any file contents
			settingsSilo.save(new BufferedWriter(settingsBuffer), this.settings.getSettings());
			inventorySilo.save(new BufferedWriter(inventoryBuffer), this.inventory);
			processTemplateSilo.save(new BufferedWriter(processBuffer), this.processTemplates);
			equipmentSilo.save(new BufferedWriter(equipmentBuffer), this.equipmentProfiles);
			recipeSilo.save(new BufferedWriter(recipesBuffer), this.recipes);
			batchSilo.save(new BufferedWriter(batchesBuffer), this.batches);
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}

		try
		{
			// write to disk
			writeToDisk("db/settings.json", settingsBuffer.toString());
			writeToDisk("db/inventory.json", inventoryBuffer.toString());
			writeToDisk("db/processtemplates.json", processBuffer.toString());
			writeToDisk("db/equipmentprofiles.json", equipmentBuffer.toString());
			writeToDisk("db/recipes.json", recipesBuffer.toString());
			writeToDisk("db/batches.json", batchesBuffer.toString());
		}
		catch (IOException e)
		{
			// At this point we assume that the data on disk is corrupt.
			// Roll back to the backed up db state
			try
			{
				restoreDb();
			}
			catch (IOException ex)
			{
				throw new BrewdayException(e);
			}

			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void writeToDisk(String fileName,
		String fileContents) throws IOException
	{
		FileWriter fileWriter = new FileWriter(fileName);
		fileWriter.write(fileContents);
		fileWriter.flush();
		fileWriter.close();
	}

	/*-------------------------------------------------------------------------*/
	private void backupDb() throws IOException
	{
		copyFiles("./db", "./db/backup/");
	}

	/*-------------------------------------------------------------------------*/
	private void restoreDb() throws IOException
	{
		copyFiles("./db/backup", "./db/");
	}

	/*-------------------------------------------------------------------------*/
	private void copyFiles(String src, String dest) throws IOException
	{
		File srcFile = new File(src);
		File destDir = new File(dest);
		if (!destDir.exists())
		{
			if (!destDir.mkdirs())
			{
				throw new IOException("can't create dir "+destDir.getName());
			}
		}

		File[] files = srcFile.listFiles((dir, name) -> name.endsWith(".json"));

		for (File f : files)
		{
			Files.copy(f.toPath(), new File(destDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static Database getInstance()
	{
		return instance;
	}

	/*-------------------------------------------------------------------------*/
	public Settings getSettings()
	{
		return settings;
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

	public Map<String, InventoryLineItem> getInventory()
	{
		return inventory;
	}

	public Map<String, EquipmentProfile> getEquipmentProfiles()
	{
		return equipmentProfiles;
	}

	public Map<String, Batch> getBatches()
	{
		return batches;
	}

	public Properties getStrings(String name)
	{
		if ("ui".equals(name))
		{
			return uiStrings;
		}
		else if ("process".equals(name))
		{
			return processStrings;
		}
		else
		{
			throw new BrewdayException("Invalid: ["+name+"]");
		}
	}
}
