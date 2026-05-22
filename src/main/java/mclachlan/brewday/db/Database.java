/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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
import mclachlan.brewday.util.AppContentRoot;
import mclachlan.brewday.Settings;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.backends.git.GitBackend;
import mclachlan.brewday.db.backends.git.GitRemoteTestResult;
import mclachlan.brewday.db.backends.git.GitSettingsMigration;
import mclachlan.brewday.db.backends.git.GitStatusSnapshot;
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
			"ambientTemperature",
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
		equipmentSerialiser.addCustomSerialiser(TemperatureUnit.class, new QuantityValueSerialiser<>(TemperatureUnit.class));

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
			Settings.migrateLegacyHopBitternessSettings(settings.getSettings());
			migrateGitSettings();

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
				new FileInputStream(resolveAppPath(fileName)),
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

			commitToGitBackendSafely(Brewday.getInstance().getLog()::log);
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
		FileWriter fileWriter = new FileWriter(resolveAppPath(fileName), StandardCharsets.UTF_8);
		fileWriter.write(fileContents);
		fileWriter.flush();
		fileWriter.close();
	}

	/*-------------------------------------------------------------------------*/
	private void backupDb() throws IOException
	{
		copyFiles(dbDir, dbDir + "/backup/", false);
	}

	/*-------------------------------------------------------------------------*/
	public void restoreDb() throws IOException
	{
		copyFiles(dbDir + "/backup", dbDir, true);
	}

	/*-------------------------------------------------------------------------*/
	private void ensureBackupDirectory() throws IOException
	{
		File backupDir = getLocalStorageBackupDirectory();
		if (!backupDir.exists() && !backupDir.mkdirs())
		{
			throw new IOException("can't create dir " + backupDir.getAbsolutePath());
		}
	}

	/*-------------------------------------------------------------------------*/
	private void backupSettingsFile() throws IOException
	{
		File live = AppContentRoot.resolveFile(dbDir + "/" + SETTINGS_JSON);
		if (!live.isFile())
		{
			return;
		}

		ensureBackupDirectory();
		File backup = new File(getLocalStorageBackupDirectory(), SETTINGS_JSON);
		Files.copy(live.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/*-------------------------------------------------------------------------*/
	private void restoreSettingsFile() throws IOException
	{
		File backup = new File(getLocalStorageBackupDirectory(), SETTINGS_JSON);
		if (!backup.isFile())
		{
			return;
		}

		File live = AppContentRoot.resolveFile(dbDir + "/" + SETTINGS_JSON);
		Files.copy(backup.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/*-------------------------------------------------------------------------*/
	private static void deleteJsonFilesInDir(File dir) throws IOException
	{
		if (!dir.isDirectory())
		{
			return;
		}

		File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
		if (files == null)
		{
			return;
		}

		for (File f : files)
		{
			if (!f.delete())
			{
				throw new IOException("can't delete " + f.getAbsolutePath());
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void copyFiles(String src, String dest, boolean requireSourceJson) throws IOException
	{
		File srcFile = AppContentRoot.resolveFile(src);
		File destDir = AppContentRoot.resolveFile(dest);

		if (!srcFile.isDirectory())
		{
			throw new IOException("source directory not found: " + srcFile.getAbsolutePath());
		}

		if (!destDir.exists())
		{
			if (!destDir.mkdirs())
			{
				throw new IOException("can't create dir " + destDir.getAbsolutePath());
			}
		}

		deleteJsonFilesInDir(destDir);

		File[] files = srcFile.listFiles((dir, name) -> name.endsWith(".json"));
		if (files == null)
		{
			files = new File[0];
		}

		if (requireSourceJson && files.length == 0)
		{
			throw new IOException("no JSON backup files in " + srcFile.getAbsolutePath());
		}

		for (File f : files)
		{
			Files.copy(f.toPath(), new File(destDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/*-------------------------------------------------------------------------*/
	public boolean hasLocalStorageBackup()
	{
		File backupDir = getLocalStorageBackupDirectory();
		if (!backupDir.isDirectory())
		{
			return false;
		}

		File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".json"));
		return files != null && files.length > 0;
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
		saveSettings(false);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @param skipGitSync when true, do not run git commit/push (used while enabling git)
	 */
	public void saveSettings(boolean skipGitSync)
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
			backupSettingsFile();

			// write to disk
			writeToDisk(dbDir+"/" + SETTINGS_JSON, settingsBuffer.toString());

			if (!skipGitSync)
			{
				commitToGitBackendSafely(Brewday.getInstance().getLog()::log);
			}
		}
		catch (IOException e)
		{
			// Roll back settings.json only; full restoreDb() would overwrite other silos
			// from a stale full-save backup.
			try
			{
				restoreSettingsFile();
			}
			catch (IOException ex)
			{
				throw new BrewdayException(e);
			}

			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void ensureGitBackend()
	{
		if (gitBackend == null)
		{
			gitBackend = new GitBackend();
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Commits to git after save; logs failures without rolling back JSON or failing the caller.
	 */
	public void commitToGitBackendSafely(GitBackend.OutputCollector outputCollector)
	{
		try
		{
			commitToGitBackend(outputCollector);
		}
		catch (BrewdayException e)
		{
			String msg = "Git backend commit failed: " + e.getMessage();
			Brewday.getInstance().getLog().log(Log.LOUD, msg);
			if (outputCollector != null)
			{
				outputCollector.append(msg);
				outputCollector.append("\n");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void commitToGitBackend(GitBackend.OutputCollector outputCollector)
	{
		if (Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED)))
		{
			Brewday.getInstance().getLog().log(Log.DEBUG, "git backend: commit local");
			ensureGitBackend();
			gitBackend.commitLocalAfterSave(getLocalStorageDirectory(), outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Fetch, pull --ff-only, and push; logs failures without affecting saved JSON.
	 */
	public void syncGitRemoteSafely(GitBackend.OutputCollector outputCollector)
	{
		try
		{
			syncGitRemote(outputCollector);
		}
		catch (BrewdayException e)
		{
			String msg = "Git remote sync failed: " + e.getMessage();
			Brewday.getInstance().getLog().log(Log.LOUD, msg);
			if (outputCollector != null)
			{
				outputCollector.append(msg);
				outputCollector.append("\n");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void syncGitRemote(GitBackend.OutputCollector outputCollector)
	{
		if (Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED)))
		{
			Brewday.getInstance().getLog().log(Log.DEBUG, "git backend: sync remote");
			ensureGitBackend();
			gitBackend.syncWithRemote(getLocalStorageDirectory(), outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	public GitStatusSnapshot getGitStatusSnapshot()
	{
		return getGitStatusSnapshot(null);
	}

	/*-------------------------------------------------------------------------*/
	public GitStatusSnapshot getGitStatusSnapshot(GitBackend.OutputCollector outputCollector)
	{
		boolean enabled = Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED));
		ensureGitBackend();
		return gitBackend.getStatus(getLocalStorageDirectory(), enabled, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Pushes to origin; logs failures without affecting saved JSON.
	 */
	public void pushGitRemoteSafely(GitBackend.OutputCollector outputCollector)
	{
		try
		{
			pushGitRemote(outputCollector);
		}
		catch (BrewdayException e)
		{
			String msg = "Git push failed: " + e.getMessage();
			Brewday.getInstance().getLog().log(Log.LOUD, msg);
			if (outputCollector != null)
			{
				outputCollector.append(msg);
				outputCollector.append("\n");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void pushGitRemote(GitBackend.OutputCollector outputCollector)
	{
		if (Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED)))
		{
			Brewday.getInstance().getLog().log(Log.DEBUG, "git backend: push remote");
			ensureGitBackend();
			gitBackend.pushToRemoteSafely(getLocalStorageDirectory(), outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void configureGitOrigin(String remoteUrl, GitBackend.OutputCollector outputCollector)
	{
		if (Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED)))
		{
			ensureGitBackend();
			gitBackend.configureOrigin(getLocalStorageDirectory(), remoteUrl, outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void enableGitBackend(String remoteUrl, GitBackend.OutputCollector outputCollector)
	{
		Brewday.getInstance().getLog().log(Log.DEBUG, "init git backend");
		ensureGitBackend();
		this.gitBackend.setupNewGitBackup(getLocalStorageDirectory(), remoteUrl, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	/** Workflow 1: {@code remoteUrl} null for local-only. */
	public void setupNewGitBackup(String remoteUrl, GitBackend.OutputCollector outputCollector)
	{
		ensureGitBackend();
		gitBackend.setupNewGitBackup(getLocalStorageDirectory(), remoteUrl, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	public GitRemoteTestResult testGitRemoteConnectivity(
		String remoteUrl,
		GitBackend.OutputCollector outputCollector)
	{
		ensureGitBackend();
		return gitBackend.testRemoteConnectivity(getLocalStorageDirectory(), remoteUrl, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return true if the user must restart Brewday (cfg repoint)
	 */
	public boolean adoptGitRepositoryAt(File repoDir, GitBackend.OutputCollector outputCollector)
	{
		ensureGitBackend();
		return gitBackend.adoptRepositoryAtPath(repoDir, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return true if the user must restart Brewday
	 */
	public boolean cloneGitRepository(
		String remoteUrl,
		File destinationDir,
		GitBackend.OutputCollector outputCollector)
	{
		ensureGitBackend();
		return gitBackend.cloneAndPrepareRepository(remoteUrl, destinationDir, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	public void addGitRemoteBackup(String remoteUrl, GitBackend.OutputCollector outputCollector)
	{
		if (Boolean.parseBoolean(getSettings().get(Settings.GIT_BACKEND_ENABLED)))
		{
			ensureGitBackend();
			gitBackend.addRemoteBackup(getLocalStorageDirectory(), remoteUrl, outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void migrateGitSettings()
	{
		GitSettingsMigration.migrate(settings, getLocalStorageDirectory());
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
		return AppContentRoot.resolveFile("data/templates");
	}

	/*-------------------------------------------------------------------------*/
	public File getLocalStorageDirectory()
	{
		return AppContentRoot.resolveFile(dbDir);
	}

	/*-------------------------------------------------------------------------*/
	public File getLocalStorageBackupDirectory()
	{
		return AppContentRoot.resolveFile(dbDir + File.separator + "backup");
	}

	/*-------------------------------------------------------------------------*/
	private static String resolveAppPath(String fileName)
	{
		if (fileName == null || fileName.isEmpty())
		{
			return fileName;
		}
		if (new File(fileName).isAbsolute())
		{
			return fileName;
		}
		return AppContentRoot.resolveFile(fileName).getPath();
	}
}
