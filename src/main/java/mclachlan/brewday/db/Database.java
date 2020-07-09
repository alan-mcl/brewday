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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.db;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 *
 */
public class Database
{
	public static final String FERMENTABLES_JSON = "fermentables.json";
	public static final String HOPS_JSON = "hops.json";
	public static final String YEASTS_JSON = "yeasts.json";
	public static final String MISCS_JSON = "miscs.json";
	public static final String WATERS_JSON = "waters.json";
	public static final String STYLES_JSON = "styles.json";
	public static final String INVENTORY_JSON = "inventory.json";
	public static final String PROCESSTEMPLATES_JSON = "processtemplates.json";
	public static final String EQUIPMENTPROFILES_JSON = "equipmentprofiles.json";
	public static final String RECIPES_JSON = "recipes.json";
	public static final String BATCHES_JSON = "batches.json";
	public static final String SETTINGS_JSON = "settings.json";
	private static Database instance = new Database();

	// non-beery data
	private Settings settings;
	private MapSingletonSilo settingsSilo;

	private Properties uiStrings, processStrings, documentStrings;
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

		ReflectiveSerialiser<EquipmentProfile> equipmentSerialiser = new ReflectiveSerialiser<>(
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
			"trubAndChillerLoss");
		equipmentSilo = new SimpleMapSilo<>(equipmentSerialiser);
		equipmentSerialiser.addCustomSerialiser(VolumeUnit.class, new QuantityValueSerialiser<>(VolumeUnit.class));
		equipmentSerialiser.addCustomSerialiser(WeightUnit.class, new QuantityValueSerialiser<>(WeightUnit.class));
		equipmentSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));
		equipmentSerialiser.addCustomSerialiser(ArbitraryPhysicalQuantity.class, new QuantityValueSerialiser<>(ArbitraryPhysicalQuantity.class));

		ReflectiveSerialiser<Hop> hopSerialiser = new ReflectiveSerialiser<>(
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
			"myrcene");
		hopsSilo = new SimpleMapSilo<>(hopSerialiser);
		hopSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));

		ReflectiveSerialiser<Fermentable> fermentableSerialiser =
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
				"ibuGalPerLb");
		fermentableSilo = new SimpleMapSilo<>(
			fermentableSerialiser);
		fermentableSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));
		fermentableSerialiser.addCustomSerialiser(ColourUnit.class, new QuantityValueSerialiser<>(ColourUnit.class));
		fermentableSerialiser.addCustomSerialiser(DiastaticPowerUnit.class, new QuantityValueSerialiser<>(DiastaticPowerUnit.class));

		ReflectiveSerialiser<Yeast> yeastSerialiser = new ReflectiveSerialiser<>(
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
			"recommendedStyles");
		yeastsSilo = new SimpleMapSilo<>(yeastSerialiser);
		yeastSerialiser.addCustomSerialiser(TemperatureUnit.class, new QuantityValueSerialiser<>(TemperatureUnit.class));
		yeastSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));

		miscsSilo = new SimpleMapSilo<>(
			new ReflectiveSerialiser<>(
				Misc.class,
				"name",
				"description",
				"type",
				"use",
				"usageRecommendation",
				"measurementType"));

		ReflectiveSerialiser<Water> waterSerialiser = new ReflectiveSerialiser<>(
			Water.class,
			"name",
			"description",
			"calcium",
			"bicarbonate",
			"sulfate",
			"chloride",
			"sodium",
			"magnesium",
			"ph");
		watersSilo = new SimpleMapSilo<>(waterSerialiser);
		waterSerialiser.addCustomSerialiser(PpmUnit.class, new QuantityValueSerialiser<>(PpmUnit.class));
		waterSerialiser.addCustomSerialiser(PhUnit.class, new QuantityValueSerialiser<>(PhUnit.class));

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
		stylesSilo = new SimpleMapSilo<>(stylesSerialiser);
		stylesSerialiser.addCustomSerialiser(DensityUnit.class, new QuantityValueSerialiser<>(DensityUnit.class));
		stylesSerialiser.addCustomSerialiser(ColourUnit.class, new QuantityValueSerialiser<>(ColourUnit.class));
		stylesSerialiser.addCustomSerialiser(BitternessUnit.class, new QuantityValueSerialiser<>(BitternessUnit.class));
		stylesSerialiser.addCustomSerialiser(CarbonationUnit.class, new QuantityValueSerialiser<>(CarbonationUnit.class));
		stylesSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));
	}

	/*-------------------------------------------------------------------------*/
	public void loadAll()
	{
		try
		{
			BufferedReader settingsReader = getFileReader("db/" + SETTINGS_JSON);
			BufferedReader uiStringsReader = getFileReader("strings/ui.properties");
			BufferedReader processStringsReader = getFileReader("strings/process.properties");
			BufferedReader documentStringsReader = getFileReader("strings/document.properties");
			BufferedReader fermentablesReader = getFileReader("db/" + FERMENTABLES_JSON);
			BufferedReader hopsReader = getFileReader("db/" + HOPS_JSON);
			BufferedReader yeastsReader = getFileReader("db/" + YEASTS_JSON);
			BufferedReader miscsReader = getFileReader("db/" + MISCS_JSON);
			BufferedReader watersReader = getFileReader("db/" + WATERS_JSON);
			BufferedReader stylesReader = getFileReader("db/" + STYLES_JSON);
			BufferedReader inventoryReader = getFileReader("db/" + INVENTORY_JSON);
			BufferedReader processTemplateReader = getFileReader("db/" + PROCESSTEMPLATES_JSON);
			BufferedReader equipmentsReader = getFileReader("db/" + EQUIPMENTPROFILES_JSON);
			BufferedReader recipesReader = getFileReader("db/" + RECIPES_JSON);
			BufferedReader batchesReader = getFileReader("db/" + BATCHES_JSON);

			settings = new Settings(settingsSilo.load(settingsReader));
			uiStrings = stringsSilo.load(uiStringsReader);
			processStrings = stringsSilo.load(processStringsReader);
			documentStrings = stringsSilo.load(documentStringsReader);

			fermentables = fermentableSilo.load(fermentablesReader);
			hops = hopsSilo.load(hopsReader);
			yeasts = yeastsSilo.load(yeastsReader);
			miscs = miscsSilo.load(miscsReader);
			waters = watersSilo.load(watersReader);
			styles = stylesSilo.load(stylesReader);

			inventory = inventorySilo.load(inventoryReader);
			processTemplates = processTemplateSilo.load(processTemplateReader);
			equipmentProfiles = equipmentSilo.load(equipmentsReader);
			recipes = recipeSilo.load(recipesReader);
			batches = batchSilo.load(batchesReader);

			settingsReader.close();
			uiStringsReader.close();
			processStringsReader.close();
			documentStringsReader.close();
			fermentablesReader.close();
			hopsReader.close();
			yeastsReader.close();
			miscsReader.close();
			watersReader.close();
			stylesReader.close();
			inventoryReader.close();
			processTemplateReader.close();
			equipmentsReader.close();
			recipesReader.close();
			batchesReader.close();
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private BufferedReader getFileReader(
		String fileName) throws FileNotFoundException
	{
		return new BufferedReader(
			new InputStreamReader(
				new FileInputStream(fileName),
				StandardCharsets.UTF_8));
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

		StringWriter fermentablesBuffer = new StringWriter();
		StringWriter hopsBuffer = new StringWriter();
		StringWriter yeastBuffer = new StringWriter();
		StringWriter waterBuffer = new StringWriter();
		StringWriter miscBuffer = new StringWriter();
		StringWriter styleBuffer = new StringWriter();

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

			fermentableSilo.save(new BufferedWriter(fermentablesBuffer), this.fermentables);
			hopsSilo.save(new BufferedWriter(hopsBuffer), this.hops);
			yeastsSilo.save(new BufferedWriter(yeastBuffer), this.yeasts);
			watersSilo.save(new BufferedWriter(waterBuffer), this.waters);
			miscsSilo.save(new BufferedWriter(miscBuffer), this.miscs);
			stylesSilo.save(new BufferedWriter(styleBuffer), this.styles);
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}

		try
		{
			// write to disk
			writeToDisk("db/" + SETTINGS_JSON, settingsBuffer.toString());
			writeToDisk("db/" + INVENTORY_JSON, inventoryBuffer.toString());
			writeToDisk("db/" + PROCESSTEMPLATES_JSON, processBuffer.toString());
			writeToDisk("db/" + EQUIPMENTPROFILES_JSON, equipmentBuffer.toString());
			writeToDisk("db/" + RECIPES_JSON, recipesBuffer.toString());
			writeToDisk("db/" + BATCHES_JSON, batchesBuffer.toString());

			writeToDisk("db/" + FERMENTABLES_JSON, fermentablesBuffer.toString());
			writeToDisk("db/" + HOPS_JSON, hopsBuffer.toString());
			writeToDisk("db/" + YEASTS_JSON, yeastBuffer.toString());
			writeToDisk("db/" + WATERS_JSON, waterBuffer.toString());
			writeToDisk("db/" + MISCS_JSON, miscBuffer.toString());
			writeToDisk("db/" + STYLES_JSON, styleBuffer.toString());
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
	public void restoreDb() throws IOException
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
				throw new IOException("can't create dir " + destDir.getName());
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
		else if ("document".equals(name))
		{
			return documentStrings;
		}
		else
		{
			throw new BrewdayException("Invalid: [" + name + "]");
		}
	}

	/*-------------------------------------------------------------------------*/
	public List<String> getDocumentTemplates()
	{
		List<String> result = new ArrayList<>();

		File templateDir = new File("./templates");
		for (File f : templateDir.listFiles((dir, name) -> name.endsWith("ftl")))
		{
			result.add(f.getName());
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public File getLocalStorageDirectory()
	{
		return new File("db");
	}

	/*-------------------------------------------------------------------------*/
	public File getLocalStorageBackupDirectory()
	{
		return new File("db", "backup");
	}
}
