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

package mclachlan.brewday.ui.jfx;

import java.awt.Desktop;
import java.io.*;
import java.util.*;
import java.util.function.*;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.document.DocumentCreator;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.util.Log;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class JfxUi extends Application implements TrackDirty
{
	public static final String BATCHES = "batches";
	public static final String RECIPES = "recipes";
	public static final String RECIPE_TAG = "RECIPE~TAG~:";
	public static final String PROCESS_TEMPLATES = "processTemplates";
	public static final String EQUIPMENT_PROFILES = "equipmentProfiles";

	public static final String INVENTORY = "inventory";

	public static final String WATER = "water";
	public static final String HOPS = "hops";
	public static final String FERMENTABLES = "fermentables";
	public static final String YEAST = "yeast";
	public static final String MISC = "misc";
	public static final String WATER_PARAMETERS = "waterParameters";
	public static final String STYLES = "styles";

	public static final String BREWING_SETTINGS_GENERAL = "brewingSettingsGeneral";
	public static final String BREWING_SETTINGS_MASH = "brewingSettingsMash";
	public static final String BREWING_SETTINGS_IBU = "brewingSettingsIbu";
	public static final String BACKEND_SETTINGS = "backendSettings";
	public static final String BACKEND_SETTINGS_LOCAL_FILESYSTEM = "backendSettingsLocalFileSystem";
	public static final String BACKEND_SETTINGS_GIT = "backendSettingsGit";
	public static final String UI_SETTINGS = "uiSettings";

	public static final String IMPORT = "import";
	public static final String WATER_BUILDER = "waterBuilder";

	public static final String ABOUT = "about";

	private CardGroup cards;
	private V2DataObjectPane<Water> refWaterPane;
	private V2DataObjectPane<Fermentable> refFermentablePane;
	private V2DataObjectPane<Hop> refHopPane;
	private V2DataObjectPane<Yeast> refYeastPane;
	private V2DataObjectPane<Misc> refMiscPane;
	private V2DataObjectPane<Style> refStylePane;
	private EquipmentProfilePane equipmentProfilePane;
	private RecipePane recipePane;
	private ProcessTemplatePane processTemplatePane;
	private V2DataObjectPane<Batch> batchesPane;
	private UiSettingsPane uiSettingsPane;
	private InventoryPane inventoryPane;
	private WaterParametersPane waterParametersPane;
	private WaterBuilderPane waterBuilderPane;
	private GitBackendPane gitBackendPane;

	private TreeItem<Label> water;
	private TreeItem<Label> waterParameters;
	private TreeItem<Label> fermentables;
	private TreeItem<Label> hops;
	private TreeItem<Label> yeast;
	private TreeItem<Label> misc;
	private TreeItem<Label> styles;
	private TreeItem<Label> batches;
	private TreeItem<Label> recipes;
	private TreeItem<Label> inventory;
	private TreeItem<Label> processTemplates;
	private TreeItem<Label> equipmentProfiles;
	private TreeItem<Label> importTools;
	private TreeItem<Label> waterBuilder;
	private TreeItem<Label> brewingSettings;
	private TreeItem<Label> uiSettings;
	private TreeItem<Label> backendSettings;
	private TreeItem<Label> inventoryRoot;

	private boolean detectDirty = true;
	private static JfxUi instance;
	private TreeItem<Label> brewing;
	private TreeItem<Label> refDatabase;
	private TreeItem<Label> settings;

	private Map<String, TreeItem<Label>> treeItems;
	private Map<TreeItem<Label>, String> cardsMap;

	private final Set<Object> dirty = new HashSet<>();
	private Scene mainScene;
	private static String theme;

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		launch(args);
	}

	/*-------------------------------------------------------------------------*/
	public static JfxUi getInstance()
	{
		return instance;
	}

	public Scene getMainScene()
	{
		return mainScene;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void start(Stage primaryStage) throws FileNotFoundException
	{
		instance = this;

		Brewday.getInstance().getLog().log(Log.DEBUG, "init icons");
		Icons.init();

		Brewday.getInstance().getLog().log(Log.DEBUG, "load db");
		Database.getInstance().loadAll();
		theme = Database.getInstance().getSettings().get(Settings.UI_THEME);

		Brewday.getInstance().getLog().log(Log.DEBUG, "init primary stage");
		primaryStage.setTitle(StringUtils.getUiString("ui.about.msg", UiUtils.getVersion()));
		primaryStage.getIcons().add(Icons.brewdayIcon);

		MigPane root = new MigPane("gap 3, insets 3");

		Group cards = getCards();

		TreeView navMenu = getNavMenuTreeView();
		navMenu.setPrefWidth(180);

		root.add(navMenu, "dock west");
		root.add(cards, "dock center, aligny top");

		refreshCards();

		// initial selection
		navMenu.requestFocus();
		brewing.setExpanded(true);
		int recipesRow = navMenu.getRow(recipes);
		navMenu.getSelectionModel().clearSelection();
		navMenu.getSelectionModel().clearAndSelect(recipesRow);

		mainScene = new Scene(root, 1280, 768);

		styleScene(mainScene);

		primaryStage.setMaximized(true);
		primaryStage.setScene(mainScene);

		Thread.currentThread().setUncaughtExceptionHandler((thread, exception) ->
		{
			Brewday.getInstance().getLog().log(Log.LOUD, exception);
			String message = exception.getMessage();

			if (message == null || message.length() == 0)
			{
				message = "Fatal: " + exception.getClass();
			}

			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));

			String stackTrace = sw.toString();

			ErrorDialog ed = new ErrorDialog(message, stackTrace);
			ed.showAndWait();
		});

		Brewday.getInstance().getLog().log(Log.DEBUG, "show primary stage");
		primaryStage.show();
	}

	/*-------------------------------------------------------------------------*/
	public static void styleScene(Scene scene)
	{
		scene.getStylesheets().clear();

		switch (theme)
		{
			case Settings.JMETRO_DARK:
				new JMetro(jfxtras.styles.jmetro.Style.DARK).setScene(scene);
				break;
			case Settings.JMETRO_LIGHT:
				new JMetro(jfxtras.styles.jmetro.Style.LIGHT).setScene(scene);
				break;
			case Settings.MODENA:
				setUserAgentStylesheet(STYLESHEET_MODENA);
				break;
			case Settings.CASPIAN:
				setUserAgentStylesheet(STYLESHEET_CASPIAN);
				break;
			default:
				throw new BrewdayException("Invalid UI theme " + theme);
		}
	}

	/*-------------------------------------------------------------------------*/
	public boolean isFeatureOn(String toggle)
	{
		return Database.getInstance().getSettings().isFeatureOn(toggle);
	}

	/*-------------------------------------------------------------------------*/
	private Group getCards()
	{
		cards = new CardGroup();

		// brewing
		cards.add(BATCHES, getBatchesPane());
		cards.add(RECIPES, getRecipesCard());

		cards.add(EQUIPMENT_PROFILES, getEquipmentProfilesCard());
		cards.add(PROCESS_TEMPLATES, getProcessTemplatesCard());

		// inventory
		cards.add(INVENTORY, getInventoryPane());

		// ref database
		cards.add(WATER, getRefWaters());
		cards.add(WATER_PARAMETERS, getWaterParametersTable());
		cards.add(HOPS, getHopsTable());
		cards.add(FERMENTABLES, getFermentablesTable());
		cards.add(YEAST, getYeastsTable());
		cards.add(MISC, getMiscsTable());
		cards.add(STYLES, getStylesTable());

		// tools
		cards.add(IMPORT, getImportPane());
		cards.add(WATER_BUILDER, getWaterBuilderPane());

		// help
		cards.add(ABOUT, getAboutPane());

		// settings
		cards.add(BREWING_SETTINGS_GENERAL, getBrewingSettingsGeneralCard());
		cards.add(BREWING_SETTINGS_MASH, getBrewingSettingsMashCard());
		cards.add(BREWING_SETTINGS_IBU, getBrewingSettingsIbuCard());
		if (isFeatureOn(Settings.FEATURE_TOGGLE_REMOTE_BACKENDS))
		{
			cards.add(BACKEND_SETTINGS, new Label("coming soonish"));
			cards.add(BACKEND_SETTINGS_LOCAL_FILESYSTEM, new Label("coming soonish"));
			cards.add(BACKEND_SETTINGS_GIT, getGitBackendSettingsCard());
		}
		cards.add(UI_SETTINGS, getUiSettingsCard());

		return cards;
	}

	/*-------------------------------------------------------------------------*/
	private Node getGitBackendSettingsCard()
	{
		gitBackendPane = new GitBackendPane();
		return gitBackendPane;
	}

	/*-------------------------------------------------------------------------*/
	private Node getWaterParametersTable()
	{
		waterParametersPane = new WaterParametersPane(WATER_PARAMETERS, this);
		return waterParametersPane;
	}

	/*-------------------------------------------------------------------------*/
	private Node getInventoryPane()
	{
		inventoryPane = new InventoryPane(this);
		return inventoryPane;
	}

	/*-------------------------------------------------------------------------*/
	private Node getWaterBuilderPane()
	{
		waterBuilderPane = new WaterBuilderPane(null);
		return waterBuilderPane;
	}

	/*-------------------------------------------------------------------------*/
	private Node getAboutPane()
	{
		MigPane result = new MigPane();

		result.add(new Label("", getImageView(Icons.brewdayIcon, Icons.ICON_SIZE)), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.msg", UiUtils.getVersion())), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.url")), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.db",
			Database.getInstance().getLocalStorageDirectory().getAbsolutePath())), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.log",
			Brewday.getInstance().getLog().getLogPath())), "wrap");

		result.add(new Label(), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.icons8")), "wrap");
		result.add(new Label(), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.gpl3")), "wrap");

		return result;
	}

	private Node getUiSettingsCard()
	{
		uiSettingsPane = new UiSettingsPane();
		return uiSettingsPane;
	}

	private Node getImportPane()
	{
		return new ImportPane(this);
	}

	private Node getBrewingSettingsGeneralCard()
	{
		return new BrewingSettingsGeneralPane();
	}
	private Node getBrewingSettingsMashCard()
	{
		return new BrewingSettingsMashPane();
	}
	private Node getBrewingSettingsIbuCard()
	{
		return new BrewingSettingsIbuPane();
	}

	private Node getBatchesPane()
	{
		batchesPane = new BatchesPane(BATCHES, this);
		return batchesPane;
	}

	private Node getProcessTemplatesCard()
	{
		processTemplatePane = new ProcessTemplatePane(PROCESS_TEMPLATES, this);
		return processTemplatePane;
	}

	private Node getRecipesCard()
	{
		recipePane = new RecipePane(RECIPES, null, this);
		return recipePane;
	}

	private Node getEquipmentProfilesCard()
	{
		equipmentProfilePane = new EquipmentProfilePane(EQUIPMENT_PROFILES, this);
		return equipmentProfilePane;
	}

	private Node getStylesTable()
	{
		refStylePane = new RefStylePane(STYLES, this);
		return refStylePane;
	}

	private Node getMiscsTable()
	{
		refMiscPane = new RefMiscPane(MISC, this);
		return refMiscPane;
	}

	private Node getYeastsTable()
	{
		refYeastPane = new RefYeastPane(YEAST, this);
		return refYeastPane;
	}

	private Node getFermentablesTable()
	{
		refFermentablePane = new RefFermentablePane(FERMENTABLES, this);
		return refFermentablePane;
	}

	private Node getHopsTable()
	{
		refHopPane = new RefHopPane(HOPS, this);
		return refHopPane;
	}

	private Node getRefWaters()
	{
		refWaterPane = new RefWaterPane(WATER, this);
		return refWaterPane;
	}

	private void refreshCards()
	{
		detectDirty = false;

		Database db = Database.getInstance();
		batchesPane.refresh(db);
		recipePane.refresh(db);
		refreshRecipeTags();
		equipmentProfilePane.refresh(db);
		processTemplatePane.refresh(db);

		inventoryPane.refresh(db);

		refWaterPane.refresh(db);
		waterParametersPane.refresh(db);
		refFermentablePane.refresh(db);
		refHopPane.refresh(db);
		refYeastPane.refresh(db);
		refMiscPane.refresh(db);
		refStylePane.refresh(db);

		uiSettingsPane.refresh(db);

		waterBuilderPane.refresh(db);

		detectDirty = true;
	}

	private TreeView<?> getNavMenuTreeView()
	{
		cardsMap = new HashMap<>();
		treeItems = new HashMap<>();

		TreeItem<Label> root = new TreeItem<>(new Label("root"));

		brewing = new TreeItem<>(new Label(getUiString("tab.brewing"), getImageView(Icons.beerIcon, Icons.NAV_ICON_SIZE)));

		batches = new TreeItem<>(new Label(getUiString("tab.batches"), getImageView(Icons.beerIcon, Icons.NAV_ICON_SIZE)));
		recipes = new TreeItem<>(new Label(getUiString("tab.recipes"), getImageView(Icons.recipeIcon, Icons.NAV_ICON_SIZE)));
		processTemplates = new TreeItem<>(new Label(getUiString("tab.process.templates"), getImageView(Icons.processTemplateIcon, Icons.NAV_ICON_SIZE)));
		equipmentProfiles = new TreeItem<>(new Label(getUiString("tab.equipment.profiles"), getImageView(Icons.equipmentIcon, Icons.NAV_ICON_SIZE)));

		brewing.getChildren().add(batches);
		brewing.getChildren().add(recipes);
		brewing.getChildren().add(processTemplates);
		brewing.getChildren().add(equipmentProfiles);

		refreshRecipeTags();

		inventoryRoot = new TreeItem<>(new Label(getUiString("tab.inventory"), getImageView(Icons.inventoryIcon, Icons.NAV_ICON_SIZE)));
		TreeItem<Label> inv1 = new TreeItem<>(new Label(getUiString("tab.inventory"), getImageView(Icons.inventoryIcon, Icons.NAV_ICON_SIZE)));
		inventoryRoot.getChildren().add(inv1);
		this.inventory = inv1;

		refDatabase = new TreeItem<>(new Label(getUiString("tab.reference.database"), getImageView(Icons.databaseIcon, Icons.NAV_ICON_SIZE)));

		water = new TreeItem<>(new Label(getUiString("tab.water"), getImageView(Icons.waterIcon, Icons.NAV_ICON_SIZE)));
		waterParameters = new TreeItem<>(new Label(getUiString("tab.water.parameters"), getImageView(Icons.waterParametersIcon, Icons.NAV_ICON_SIZE)));
		fermentables = new TreeItem<>(new Label(getUiString("tab.fermentables"), getImageView(Icons.fermentableIconGeneric, Icons.NAV_ICON_SIZE)));
		hops = new TreeItem<>(new Label(getUiString("tab.hops"), getImageView(Icons.hopsIcon, Icons.NAV_ICON_SIZE)));
		yeast = new TreeItem<>(new Label(getUiString("tab.yeast"), getImageView(Icons.yeastIcon, Icons.NAV_ICON_SIZE)));
		misc = new TreeItem<>(new Label(getUiString("tab.misc"), getImageView(Icons.miscIconGeneric, Icons.NAV_ICON_SIZE)));
		styles = new TreeItem<>(new Label(getUiString("tab.styles"), getImageView(Icons.stylesIcon, Icons.NAV_ICON_SIZE)));

		refDatabase.getChildren().addAll(water, waterParameters, fermentables, hops, yeast, misc, styles);

		TreeItem<Label> tools = new TreeItem<>(new Label(StringUtils.getUiString("tab.tools"), getImageView(Icons.toolsIcon, Icons.NAV_ICON_SIZE)));

		importTools = new TreeItem<>(new Label(getUiString("tools.import"), getImageView(Icons.importIcon, Icons.NAV_ICON_SIZE)));
		waterBuilder = new TreeItem<>(new Label(getUiString("tools.water.builder"), getImageView(Icons.waterBuilderIcon, Icons.NAV_ICON_SIZE)));

		tools.getChildren().addAll(waterBuilder, importTools);

		settings = new TreeItem<>(new Label(StringUtils.getUiString("tab.settings"), getImageView(Icons.settingsIcon, Icons.NAV_ICON_SIZE)));

		brewingSettings = new TreeItem<>(new Label(StringUtils.getUiString("settings.brewing"), getImageView(Icons.settingsIcon, Icons.NAV_ICON_SIZE)));
		backendSettings = new TreeItem<>(new Label(StringUtils.getUiString("settings.backend"), getImageView(Icons.settingsIcon, Icons.NAV_ICON_SIZE)));
		uiSettings = new TreeItem<>(new Label(StringUtils.getUiString("settings.ui"), getImageView(Icons.settingsIcon, Icons.NAV_ICON_SIZE)));

		TreeItem<Label> brewingSettingsGeneral = new TreeItem<>(new Label(StringUtils.getUiString("settings.brewing.general"), getImageView(Icons.settingsIcon, Icons.NAV_ICON_SIZE)));
		TreeItem<Label> brewingSettingsIbu = new TreeItem<>(new Label(StringUtils.getUiString("settings.brewing.ibu"), getImageView(Icons.hopsIcon, Icons.NAV_ICON_SIZE)));
		TreeItem<Label> brewingSettingsMash = new TreeItem<>(new Label(StringUtils.getUiString("settings.brewing.mash"), getImageView(Icons.mashIcon, Icons.NAV_ICON_SIZE)));

		brewingSettings.getChildren().add(brewingSettingsGeneral);
		brewingSettings.getChildren().add(brewingSettingsMash);
		brewingSettings.getChildren().add(brewingSettingsIbu);

		TreeItem<Label> backendLocalFileSystem = new TreeItem<>(new Label(StringUtils.getUiString("settings.backend.local.filesystem"), getImageView(Icons.databaseIcon, Icons.NAV_ICON_SIZE)));
		TreeItem<Label> backendGit = new TreeItem<>(new Label(StringUtils.getUiString("settings.backend.git"), getImageView(Icons.gitIcon, Icons.NAV_ICON_SIZE)));

		backendSettings.getChildren().add(backendLocalFileSystem);
		backendSettings.getChildren().add(backendGit);

		settings.getChildren().add(brewingSettings);
		if (isFeatureOn(Settings.FEATURE_TOGGLE_REMOTE_BACKENDS))
		{
			settings.getChildren().add(backendSettings);
		}
		settings.getChildren().add(uiSettings);

		TreeItem<Label> help = new TreeItem<>(new Label(StringUtils.getUiString("ui.help"), getImageView(Icons.helpIcon, Icons.NAV_ICON_SIZE)));
		TreeItem<Label> about = new TreeItem<>(new Label(StringUtils.getUiString("ui.about"), getImageView(Icons.brewdayIcon, Icons.NAV_ICON_SIZE)));

		help.getChildren().addAll(about);

		root.getChildren().add(brewing);
		root.getChildren().add(inventoryRoot);
		root.getChildren().add(refDatabase);
		root.getChildren().add(tools);
		root.getChildren().add(settings);
		root.getChildren().add(help);

		TreeView treeView = new TreeView();
		treeView.setRoot(root);
		treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		treeView.setShowRoot(false);

		treeView.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null && newValue != oldValue)
				{
					String selected = cardsMap.get(newValue);

					if (selected != null && selected.startsWith(RECIPE_TAG))
					{
						String[] split = selected.split(":");
						selected = RECIPES;
						String tag = split[1];
						recipePane.setTag(tag);
					}
					else if (RECIPES.equals(selected))
					{
						recipePane.setTag(null);
					}

					if (selected != null)
					{
						cards.setVisible(selected);
					}
				}
			});

		cardsMap.put(batches, BATCHES);
		cardsMap.put(recipes, RECIPES);
		cardsMap.put(processTemplates, PROCESS_TEMPLATES);
		cardsMap.put(equipmentProfiles, EQUIPMENT_PROFILES);
		cardsMap.put(inventory, INVENTORY);
		cardsMap.put(water, WATER);
		cardsMap.put(waterParameters, WATER_PARAMETERS);
		cardsMap.put(fermentables, FERMENTABLES);
		cardsMap.put(hops, HOPS);
		cardsMap.put(yeast, YEAST);
		cardsMap.put(misc, MISC);
		cardsMap.put(styles, STYLES);
		cardsMap.put(brewingSettingsGeneral, BREWING_SETTINGS_GENERAL);
		cardsMap.put(brewingSettingsMash, BREWING_SETTINGS_MASH);
		cardsMap.put(brewingSettingsIbu, BREWING_SETTINGS_IBU);
		cardsMap.put(backendSettings, BACKEND_SETTINGS);
		cardsMap.put(backendLocalFileSystem, BACKEND_SETTINGS_LOCAL_FILESYSTEM);
		cardsMap.put(backendGit, BACKEND_SETTINGS_GIT);
		cardsMap.put(uiSettings, UI_SETTINGS);
		cardsMap.put(importTools, IMPORT);
		cardsMap.put(waterBuilder, WATER_BUILDER);
		cardsMap.put(about, ABOUT);

		treeItems.put(BATCHES, batches);
		treeItems.put(RECIPES, recipes);
		treeItems.put(PROCESS_TEMPLATES, processTemplates);
		treeItems.put(EQUIPMENT_PROFILES, equipmentProfiles);
		treeItems.put(INVENTORY, inventory);
		treeItems.put(WATER, water);
		treeItems.put(WATER_PARAMETERS, waterParameters);
		treeItems.put(FERMENTABLES, fermentables);
		treeItems.put(HOPS, hops);
		treeItems.put(YEAST, yeast);
		treeItems.put(MISC, misc);
		treeItems.put(STYLES, styles);
		treeItems.put(BREWING_SETTINGS_GENERAL, brewingSettingsGeneral);
		treeItems.put(BREWING_SETTINGS_MASH, brewingSettingsMash);
		treeItems.put(BREWING_SETTINGS_IBU, brewingSettingsIbu);
		treeItems.put(BACKEND_SETTINGS, backendSettings);
		treeItems.put(BACKEND_SETTINGS_LOCAL_FILESYSTEM, backendLocalFileSystem);
		treeItems.put(BACKEND_SETTINGS_GIT, backendGit);
		treeItems.put(UI_SETTINGS, uiSettings);
		treeItems.put(IMPORT, importTools);
		treeItems.put(WATER_BUILDER, waterBuilder);
		treeItems.put(ABOUT, about);

		return treeView;
	}

	/*-------------------------------------------------------------------------*/
	private void refreshRecipeTags()
	{
		// recipe tags

		recipes.getChildren().clear();

		List<String> recipeTags = Brewday.getInstance().getRecipeTags();
		for (String tag : recipeTags)
		{
			TreeItem<Label> tagItem = new TreeItem<>(new Label(tag, getImageView(Icons.recipeIcon, Icons.NAV_ICON_SIZE)));
			recipes.getChildren().add(tagItem);

			cardsMap.put(tagItem, RECIPE_TAG+tag);
			treeItems.put(RECIPE_TAG+tag, tagItem);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void setDetectDirty(boolean detectDirty)
	{
		this.detectDirty = detectDirty;
	}

	/*-------------------------------------------------------------------------*/
	public void setDirty(Object... objs)
	{
		if (detectDirty)
		{
			boolean anyChanged = false;

			for (Object obj : objs)
			{
				if (!this.isDirty(obj))
				{
					anyChanged = true;
					this.dirty.add(obj);

					String dirtyCss = "-fx-font-weight: bold;";

					if (obj instanceof String)
					{
						String dirtyFlag = (String)obj;

						TreeItem<Label> treeItem = treeItems.get(dirtyFlag);
						treeItem.getValue().setStyle(dirtyCss);

						switch ((String)obj)
						{
							case BATCHES:
							case RECIPES:
							case PROCESS_TEMPLATES:
							case EQUIPMENT_PROFILES:
								brewing.getValue().setStyle(dirtyCss);
								break;

							case WATER:
							case WATER_PARAMETERS:
							case HOPS:
							case FERMENTABLES:
							case YEAST:
							case MISC:
							case STYLES:
								refDatabase.getValue().setStyle(dirtyCss);
								break;

							case INVENTORY:
								inventoryRoot.getValue().setStyle(dirtyCss);
								inventory.getValue().setStyle(dirtyCss);
								break;

							case IMPORT:
							case WATER_BUILDER:
								// not applicable
								break;

							case BREWING_SETTINGS_GENERAL:
							case BREWING_SETTINGS_MASH:
							case BREWING_SETTINGS_IBU:
								brewingSettings.getValue().setStyle(dirtyCss);
								settings.getValue().setStyle(dirtyCss);
								break;
							case BACKEND_SETTINGS:
							case BACKEND_SETTINGS_LOCAL_FILESYSTEM:
							case BACKEND_SETTINGS_GIT:
							case UI_SETTINGS:
								// not applicable
								break;

							case ABOUT:
								break;
						}
					}
				}
			}

			if (anyChanged)
			{
				refreshCards();
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public boolean isDirty(Object obj)
	{
		return this.dirty.contains(obj);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void clearDirty()
	{
		water.getValue().setStyle(null);
		waterParameters.getValue().setStyle(null);
		fermentables.getValue().setStyle(null);
		hops.getValue().setStyle(null);
		yeast.getValue().setStyle(null);
		misc.getValue().setStyle(null);
		styles.getValue().setStyle(null);
		batches.getValue().setStyle(null);
		recipes.getValue().setStyle(null);
		processTemplates.getValue().setStyle(null);
		equipmentProfiles.getValue().setStyle(null);
		uiSettings.getValue().setStyle(null);
		settings.getValue().setStyle(null);
		inventoryRoot.getValue().setStyle(null);
		inventory.getValue().setStyle(null);

		brewing.getValue().setStyle(null);
		refDatabase.getValue().setStyle(null);

		this.dirty.clear();
	}

	/*-------------------------------------------------------------------------*/
	public static ImageView getImageView(Image i, int size)
	{
		ImageView result = new ImageView(i);
		result.setPreserveRatio(true);
		result.setFitHeight(size);
		result.setFitWidth(size);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static Button getDocumentGenerationButton(Supplier<Recipe> recipeSupplier)
	{
		Button result = new Button(
			StringUtils.getUiString("doc.gen.generate.document"),
			JfxUi.getImageView(Icons.documentIcon, Icons.ICON_SIZE));

		result.setOnAction(event ->
		{
			List<String> documentTemplates = Database.getInstance().getDocumentTemplates();

			if (documentTemplates == null || documentTemplates.isEmpty())
			{
				return;
			}

			ChoiceDialog<String> dialog = new ChoiceDialog<>(
				documentTemplates.get(0),
				documentTemplates.toArray(new String[0]));
			dialog.setTitle(StringUtils.getUiString("doc.gen.generate.document"));
			dialog.setContentText(StringUtils.getUiString("doc.gen.choose.template"));
			dialog.setGraphic(JfxUi.getImageView(Icons.documentIcon, Icons.ICON_SIZE));

			JfxUi.styleScene(dialog.getDialogPane().getScene());

			dialog.showAndWait();
			String template = dialog.getResult();

			if (template != null)
			{
				String defaultSuffix = template.substring(0, template.indexOf("."));

				String extension = template.substring(
					template.indexOf(".")+1,
					template.lastIndexOf("."));

				FileChooser chooser = new FileChooser();

				FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
					"."+extension, extension);
				chooser.setSelectedExtensionFilter(filter);

				chooser.setInitialFileName(
					recipeSupplier.get().getName().replaceAll("\\W", "_")+ "_"+defaultSuffix+"."+ extension);

				Settings settings = Database.getInstance().getSettings();
				String dirS = settings.get(Settings.LAST_EXPORT_DIRECTORY);

				if (dirS != null)
				{
					File dir = new File(dirS);
					if (dir.exists() && dir.isDirectory())
					{
						chooser.setInitialDirectory(dir);
					}
				}

				File file = chooser.showSaveDialog(JfxUi.getInstance().getMainScene().getWindow());
				if (file != null)
				{
					String parent = file.getParent();
					if (parent != null)
					{
						settings.set(Settings.LAST_EXPORT_DIRECTORY, parent);
						Database.getInstance().saveSettings();
					}

					DocumentCreator dc = DocumentCreator.getInstance();

					try
					{
						dc.createDocument(recipeSupplier.get(), template, file);
						Desktop.getDesktop().open(file);
					}
					catch (IOException ex)
					{
						throw new BrewdayException(ex);
					}
				}
			}
		});

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static class OkCancelDialog extends Dialog<Boolean>
	{
		private boolean output = false;

		public OkCancelDialog(String title, String message)
		{
			Scene scene = this.getDialogPane().getScene();
			JfxUi.styleScene(scene);
			Stage stage = (Stage)scene.getWindow();
			stage.getIcons().add(Icons.infoIcon);

			ButtonType okButtonType = new ButtonType(
				getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
			ButtonType cancelButtonType = new ButtonType(
				getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

			this.getDialogPane().getButtonTypes().add(okButtonType);
			this.getDialogPane().getButtonTypes().add(cancelButtonType);

			this.setTitle(title);

			MigPane content = new MigPane();

			content.setPrefWidth(500);

			Label headerLabel = new Label(message);
			headerLabel.setWrapText(true);
			headerLabel.setPrefWidth(450);
			content.add(headerLabel, "wrap");

			this.getDialogPane().setContent(content);

			// -----

			final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
			btOk.addEventFilter(ActionEvent.ACTION, event -> output = true);
		}

		public boolean getOutput()
		{
			return output;
		}
	}

	/*-------------------------------------------------------------------------*/
	public static class ErrorDialog extends Dialog<Boolean>
	{
		private boolean output = false;

		public ErrorDialog(String message, String stackTrace)
		{
			Scene scene = this.getDialogPane().getScene();
			JfxUi.styleScene(scene);
			Stage stage = (Stage)scene.getWindow();
			stage.getIcons().add(Icons.deleteIcon);

			ButtonType okButtonType = new ButtonType(
				getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
			this.getDialogPane().getButtonTypes().add(okButtonType);

			this.setTitle(StringUtils.getUiString("ui.error"));

			MigPane content = new MigPane();

			content.setPrefWidth(500);

			Label headerLabel = new Label(message);
			headerLabel.setWrapText(true);
			headerLabel.setPrefWidth(450);
			content.add(headerLabel, "wrap");

			TextArea textArea = new TextArea();
			textArea.setPrefWidth(450);
			textArea.setEditable(false);
			content.add(textArea, "span, wrap");

			textArea.setText(stackTrace);

			this.getDialogPane().setContent(content);

			// -----

			final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
			btOk.addEventFilter(ActionEvent.ACTION, event -> output = true);
		}
	}
}
