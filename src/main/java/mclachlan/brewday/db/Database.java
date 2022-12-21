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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.backends.git.GitBackend;
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
import mclachlan.brewday.util.Log;

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
	public static final String WATER_PARAMETERS_JSON = "waterparameters.json";
	public static final String STYLES_JSON = "styles.json";
	public static final String INVENTORY_JSON = "inventory.json";
	public static final String PROCESSTEMPLATES_JSON = "processtemplates.json";
	public static final String EQUIPMENTPROFILES_JSON = "equipmentprofiles.json";
	public static final String RECIPES_JSON = "recipes.json";
	public static final String BATCHES_JSON = "batches.json";
	public static final String SETTINGS_JSON = "settings.json";

	private static Database instance = new Database();

	private final String dbDir;

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

	private final SimpleMapSilo<Recipe> recipeSilo;
	private final SimpleMapSilo<Recipe> processTemplateSilo;
	private final SimpleMapSilo<EquipmentProfile> equipmentSilo;
	private final SimpleMapSilo<InventoryLineItem> inventorySilo;
	private final SimpleMapSilo<Batch> batchSilo;

	// reference data
	private Map<String, Hop> hops;
	private Map<String, Fermentable> fermentables;
	private Map<String, Yeast> yeasts;
	private Map<String, Misc> miscs;
	private Map<String, Water> waters;
	private Map<String, WaterParameters> waterParameters;
	private Map<String, Style> styles;

	private final SimpleMapSilo<Hop> hopsSilo;
	private final SimpleMapSilo<Fermentable> fermentableSilo;
	private final SimpleMapSilo<Yeast> yeastsSilo;
	private final SimpleMapSilo<Misc> miscsSilo;
	private final SimpleMapSilo<Water> watersSilo;
	private final SimpleMapSilo<WaterParameters> waterParametersSilo;
	private final SimpleMapSilo<Style> stylesSilo;

	// optional backends
	private GitBackend gitBackend;
	private ReflectiveSerialiser<Water> waterSerialiser;

	/*-------------------------------------------------------------------------*/
	public Database()
	{
		this(Brewday.getInstance().getAppConfig().getProperty(Brewday.BREWDAY_DB));
	}

	public Database(String dbDir)
	{
		this.dbDir = dbDir;

		settingsSilo = new MapSingletonSilo();
		stringsSilo = new PropertiesSilo();

		recipeSilo = new SimpleMapSilo<>(new RecipeSerialiser());
		processTemplateSilo = new SimpleMapSilo<>(new RecipeSerialiser());
		batchSilo = new SimpleMapSilo<>(new BatchSerialiser());

		InventoryLineItemSerialiser inventoryLineItemSerialiser =
			new InventoryLineItemSerialiser();
		inventorySilo = new SimpleMapSilo<>(inventoryLineItemSerialiser);

		ReflectiveSerialiser<EquipmentProfile> equipmentSerialiser = new ReflectiveSerialiser<>(
			EquipmentProfile.class,
			"name",
			"description",
			"elevation",
			"conversionEfficiency",
			"mashTunVolume",
			"mashTunWeight",
			"mashTunSpecificHeat",
			"boilKettleVolume",
			"boilEvapourationRate",
			"boilElementPower",
			"hopUtilisation",
			"fermenterVolume",
			"lauterLoss",
			"trubAndChillerLoss");
		equipmentSilo = new SimpleMapSilo<>(equipmentSerialiser);
		equipmentSerialiser.addCustomSerialiser(VolumeUnit.class, new QuantityValueSerialiser<>(VolumeUnit.class));
		equipmentSerialiser.addCustomSerialiser(WeightUnit.class, new QuantityValueSerialiser<>(WeightUnit.class));
		equipmentSerialiser.addCustomSerialiser(LengthUnit.class, new QuantityValueSerialiser<>(LengthUnit.class));
		equipmentSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));
		equipmentSerialiser.addCustomSerialiser(PowerUnit.class, new QuantityValueSerialiser<>(PowerUnit.class));
		equipmentSerialiser.addCustomSerialiser(ArbitraryPhysicalQuantity.class, new QuantityValueSerialiser<>(ArbitraryPhysicalQuantity.class));

		ReflectiveSerialiser<Hop> hopSerialiser = new ReflectiveSerialiser<>(
			Hop.class,
			"name",
			"description",
			"alphaAcid",
			"hopStorageIndex",
			"type",
			"form",
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
				"bufferingCapacity",
				"protein",
				"maxInBatch",
				"recommendMash",
				"ibuGalPerLb",
				"distilledWaterPh",
				"lacticAcidContent");
		fermentableSilo = new SimpleMapSilo<>(
			fermentableSerialiser);
		fermentableSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));
		fermentableSerialiser.addCustomSerialiser(ColourUnit.class, new QuantityValueSerialiser<>(ColourUnit.class));
		fermentableSerialiser.addCustomSerialiser(DiastaticPowerUnit.class, new QuantityValueSerialiser<>(DiastaticPowerUnit.class));
		fermentableSerialiser.addCustomSerialiser(ArbitraryPhysicalQuantity.class, new ArbitraryPhysicalQuantitySerialiser());
		fermentableSerialiser.addCustomSerialiser(PhUnit.class, new QuantityValueSerialiser<>(PhUnit.class));

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

		ReflectiveSerialiser<Misc> miscSerialiser = new ReflectiveSerialiser<>(
			Misc.class,
			"name",
			"description",
			"type",
			"use",
			"usageRecommendation",
			"measurementType",
			"waterAdditionFormula",
			"acidContent");
		miscsSilo = new SimpleMapSilo<>(miscSerialiser);
		miscSerialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<>(PercentageUnit.class));

		waterSerialiser = new ReflectiveSerialiser<>(
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

		ReflectiveSerialiser<WaterParameters> waterParametersSerialiser = new ReflectiveSerialiser<>(
			WaterParameters.class,
			"name",
			"description",
			"minCalcium",
			"maxCalcium",
			"minBicarbonate",
			"maxBicarbonate",
			"minSulfate",
			"maxSulfate",
			"minChloride",
			"maxChloride",
			"minSodium",
			"maxSodium",
			"minMagnesium",
			"maxMagnesium",
			"minAlkalinity",
			"maxAlkalinity",
			"minResidualAlkalinity",
			"maxResidualAlkalinity");
		waterParametersSilo = new SimpleMapSilo<>(waterParametersSerialiser);
		waterParametersSerialiser.addCustomSerialiser(PpmUnit.class, new QuantityValueSerialiser<>(PpmUnit.class));
		waterParametersSerialiser.addCustomSerialiser(PhUnit.class, new QuantityValueSerialiser<>(PhUnit.class));

		ReflectiveSerialiser<Style> stylesSerialiser = new ReflectiveSerialiser<>(
			Style.class,
			"name",
			"displayName",
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
			BufferedReader settingsReader = getFileReader(dbDir+"/" + SETTINGS_JSON);
			BufferedReader uiStringsReader = getFileReader("data/strings/ui.properties");
			BufferedReader processStringsReader = getFileReader("data/strings/process.properties");
			BufferedReader documentStringsReader = getFileReader("data/strings/document.properties");
			BufferedReader fermentablesReader = getFileReader(dbDir+"/" + FERMENTABLES_JSON);
			BufferedReader hopsReader = getFileReader(dbDir+"/" + HOPS_JSON);
			BufferedReader yeastsReader = getFileReader(dbDir+"/" + YEASTS_JSON);
			BufferedReader miscsReader = getFileReader(dbDir+"/" + MISCS_JSON);
			BufferedReader watersReader = getFileReader(dbDir+"/" + WATERS_JSON);
			BufferedReader waterParametersReader = getFileReader(dbDir+"/" + WATER_PARAMETERS_JSON);
			BufferedReader stylesReader = getFileReader(dbDir+"/" + STYLES_JSON);
			BufferedReader inventoryReader = getFileReader(dbDir+"/" + INVENTORY_JSON);
			BufferedReader processTemplateReader = getFileReader(dbDir+"/" + PROCESSTEMPLATES_JSON);
			BufferedReader equipmentsReader = getFileReader(dbDir+"/" + EQUIPMENTPROFILES_JSON);
			BufferedReader recipesReader = getFileReader(dbDir+"/" + RECIPES_JSON);
			BufferedReader batchesReader = getFileReader(dbDir+"/" + BATCHES_JSON);

			Brewday.getInstance().getLog().log(Log.DEBUG, "db load settings");
			settings = new Settings(settingsSilo.load(settingsReader, this));

			Brewday.getInstance().getLog().log(Log.DEBUG, "db load strings");
			uiStrings = stringsSilo.load(uiStringsReader, this);
			processStrings = stringsSilo.load(processStringsReader, this);
			documentStrings = stringsSilo.load(documentStringsReader, this);

			Brewday.getInstance().getLog().log(Log.DEBUG, "db load ref data");
			fermentables = fermentableSilo.load(fermentablesReader, this);
			hops = hopsSilo.load(hopsReader, this);
			yeasts = yeastsSilo.load(yeastsReader, this);
			miscs = miscsSilo.load(miscsReader, this);
			waters = watersSilo.load(watersReader, this);
			waterParameters = waterParametersSilo.load(waterParametersReader, this);
			styles = stylesSilo.load(stylesReader, this);

			Brewday.getInstance().getLog().log(Log.DEBUG, "db load saved data");
			inventory = inventorySilo.load(inventoryReader, this);
			processTemplates = processTemplateSilo.load(processTemplateReader, this);
			equipmentProfiles = equipmentSilo.load(equipmentsReader, this);
			recipes = recipeSilo.load(recipesReader, this);
			batches = batchSilo.load(batchesReader, this);

			settingsReader.close();
			uiStringsReader.close();
			processStringsReader.close();
			documentStringsReader.close();
			fermentablesReader.close();
			hopsReader.close();
			yeastsReader.close();
			miscsReader.close();
			watersReader.close();
			waterParametersReader.close();
			stylesReader.close();
			inventoryReader.close();
			processTemplateReader.close();
			equipmentsReader.close();
			recipesReader.close();
			batchesReader.close();

			// init backends
			// sync the git backend

			boolean aBoolean = Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED));
			if (aBoolean)
			{
				Brewday.getInstance().getLog().log(Log.DEBUG, "init git backend");
				gitBackend = new GitBackend();
			}
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
		StringWriter waterParametersBuffer = new StringWriter();
		StringWriter miscBuffer = new StringWriter();
		StringWriter styleBuffer = new StringWriter();

		try
		{
			// back up the current database
			backupDb();

			// marshall into memory. errors here will not overwrite any file contents
			settingsSilo.save(new BufferedWriter(settingsBuffer), this.settings.getSettings());
			inventorySilo.save(new BufferedWriter(inventoryBuffer), this.inventory, this);
			processTemplateSilo.save(new BufferedWriter(processBuffer), this.processTemplates, this);
			equipmentSilo.save(new BufferedWriter(equipmentBuffer), this.equipmentProfiles, this);
			recipeSilo.save(new BufferedWriter(recipesBuffer), this.recipes, this);
			batchSilo.save(new BufferedWriter(batchesBuffer), this.batches, this);

			fermentableSilo.save(new BufferedWriter(fermentablesBuffer), this.fermentables, this);
			hopsSilo.save(new BufferedWriter(hopsBuffer), this.hops, this);
			yeastsSilo.save(new BufferedWriter(yeastBuffer), this.yeasts, this);
			watersSilo.save(new BufferedWriter(waterBuffer), this.waters, this);
			waterParametersSilo.save(new BufferedWriter(waterParametersBuffer), this.waterParameters, this);
			miscsSilo.save(new BufferedWriter(miscBuffer), this.miscs, this);
			stylesSilo.save(new BufferedWriter(styleBuffer), this.styles, this);
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}

		try
		{
			// write to disk
			writeToDisk(dbDir+"/" + SETTINGS_JSON, settingsBuffer.toString());
			writeToDisk(dbDir+"/" + INVENTORY_JSON, inventoryBuffer.toString());
			writeToDisk(dbDir+"/" + PROCESSTEMPLATES_JSON, processBuffer.toString());
			writeToDisk(dbDir+"/" + EQUIPMENTPROFILES_JSON, equipmentBuffer.toString());
			writeToDisk(dbDir+"/" + RECIPES_JSON, recipesBuffer.toString());
			writeToDisk(dbDir+"/" + BATCHES_JSON, batchesBuffer.toString());

			writeToDisk(dbDir+"/" + FERMENTABLES_JSON, fermentablesBuffer.toString());
			writeToDisk(dbDir+"/" + HOPS_JSON, hopsBuffer.toString());
			writeToDisk(dbDir+"/" + YEASTS_JSON, yeastBuffer.toString());
			writeToDisk(dbDir+"/" + WATERS_JSON, waterBuffer.toString());
			writeToDisk(dbDir+"/" + WATER_PARAMETERS_JSON, waterParametersBuffer.toString());
			writeToDisk(dbDir+"/" + MISCS_JSON, miscBuffer.toString());
			writeToDisk(dbDir+"/" + STYLES_JSON, styleBuffer.toString());

			syncToGitBackend(Brewday.getInstance().getLog()::log);
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
		FileWriter fileWriter = new FileWriter(fileName, StandardCharsets.UTF_8);
		fileWriter.write(fileContents);
		fileWriter.flush();
		fileWriter.close();
	}

	/*-------------------------------------------------------------------------*/
	private void backupDb() throws IOException
	{
		copyFiles(dbDir, dbDir+"/backup/");
	}

	/*-------------------------------------------------------------------------*/
	public void restoreDb() throws IOException
	{
		copyFiles(dbDir+"/backup", dbDir);
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

	/**
	 * Saves only the settings data.
	 */
	public void saveSettings()
	{
		StringWriter settingsBuffer = new StringWriter();

		try
		{
			// marshall into memory. errors here will not overwrite any file contents
			settingsSilo.save(new BufferedWriter(settingsBuffer), this.settings.getSettings());
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}

		try
		{
			// write to disk
			writeToDisk(dbDir+"/" + SETTINGS_JSON, settingsBuffer.toString());

			syncToGitBackend(Brewday.getInstance().getLog()::log);
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
	public void syncToGitBackend(GitBackend.OutputCollector outputCollector)
	{
		if (Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED)))
		{
			Brewday.getInstance().getLog().log(Log.DEBUG, "git backend: sync to remote");
			gitBackend.syncToRemote(new File(this.dbDir), outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void syncFromGitBackend(GitBackend.OutputCollector outputCollector)
	{
		if (Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED)))
		{
			Brewday.getInstance().getLog().log(Log.DEBUG, "git backend: sync from remote");
			gitBackend.syncFromRemote(new File(this.dbDir), outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void enableGitBackend(GitBackend.OutputCollector outputCollector)
	{
		Brewday.getInstance().getLog().log(Log.DEBUG, "init git backend");
		this.gitBackend = new GitBackend();

		this.gitBackend.enable(getLocalStorageDirectory(), getSettings().get(Settings.GIT_REMOTE_REPO), outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	public void disableGitBackend(GitBackend.OutputCollector outputCollector)
	{
		this.gitBackend.disable(outputCollector);
		this.gitBackend = null;
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

	public ReflectiveSerialiser<Water> getWaterSerialiser()
	{
		return waterSerialiser;
	}

	public Map<String, WaterParameters> getWaterParameters()
	{
		return waterParameters;
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

		File templateDir = getTemplateDir();
		for (File f : templateDir.listFiles((dir, name) -> name.endsWith("ftl")))
		{
			result.add(f.getName());
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public File getTemplateDir()
	{
		return new File("./data/templates");
	}

	/*-------------------------------------------------------------------------*/
	public File getLocalStorageDirectory()
	{
		return new File(dbDir);
	}

	/*-------------------------------------------------------------------------*/
	public File getLocalStorageBackupDirectory()
	{
		return new File(dbDir, "backup");
	}
}
