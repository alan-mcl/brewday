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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.style.Style;
import mclachlan.brewday.ui.UiUtils;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;

public class JfxUi extends Application implements TrackDirty
{
	public static final int ICON_SIZE = 32;

	public static final String BATCHES = "batches";
	public static final String RECIPES = "recipes";
	public static final String PROCESS_TEMPLATES = "processTemplates";
	public static final String EQUIPMENT_PROFILES = "equipmentProfiles";

	public static final String INVENTORY = "inventory";

	public static final String WATER = "water";
	public static final String HOPS = "hops";
	public static final String FERMENTABLES = "fermentables";
	public static final String YEAST = "yeast";
	public static final String MISC = "misc";
	public static final String STYLES = "styles";

	public static final String BREWING_SETTINGS = "brewingSettings";
	public static final String BACKEND_SETTINGS = "backendSettings";
	public static final String UI_SETTINGS = "uiSettings";

	public static final String IMPORT = "import";

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

	private TreeItem<Label> water;
	private TreeItem<Label> fermentables;
	private TreeItem<Label> hops;
	private TreeItem<Label> yeast;
	private TreeItem<Label> misc;
	private TreeItem<Label> styles;
	private TreeItem<Label> batches;
	private TreeItem<Label> recipes;
	private TreeItem<Label> processTemplates;
	private TreeItem<Label> equipmentProfiles;
	private TreeItem<Label> importTools;
	private TreeItem<Label> brewingSettings;
	private TreeItem<Label> uiSettings;
	private TreeItem<Label> backendSettings;

	private boolean detectDirty = true;
	private static JfxUi instance;
	private TreeItem<Label> brewing;
	private TreeItem<Label> refDatabase;

	private Map<String, TreeItem<Label>> treeItems;
	private Map<TreeItem<Label>, String> cardsMap;

	private final Set<Object> dirty = new HashSet<>();
	private Scene mainScene;

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args)
	{
		try
		{
			launch(args);
		}
		catch (Exception x)
		{
			x.printStackTrace();
			throw x;
		}
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

		brewdayIcon = createImage("img/brewday.png");
		recipeIcon = createImage("img/icons8-beer-recipe-48.png");
		stepIcon = createImage("img/icons8-file-48.png");
		hopsIcon = createImage("img/icons8-hops-48.png");
		fermentableIcon = createImage("img/icons8-carbohydrates-48.png");
		waterIcon = createImage("img/icons8-water-48.png");
		yeastIcon = createImage("img/icons8-experiment-48.png");
		miscIcon = createImage("img/icons8-sugar-cubes-48.png");
		removeIcon = createImage("img/icons8-delete-48.png");
		increaseIcon = createImage("img/icons8-plus-48.png");
		decreaseIcon = createImage("img/icons8-minus-48.png");
		moreTimeIcon = createImage("img/icons8-future-48.png");
		lessTimeIcon = createImage("img/icons8-time-machine-48.png");
		searchIcon = createImage("img/icons8-search-48.png");
		editIcon = createImage("img/icons8-edit-property-48.png");
		newIcon = createImage("img/icons8-add-new-48.png");
		deleteIcon = createImage("img/icons8-delete-48.png");
		duplicateIcon = createImage("img/icons8-transfer-48.png");
		substituteIcon = createImage("img/icons8-replace-48.png");
		processTemplateIcon = createImage("img/icons8-flow-48.png");
		beerIcon = createImage("img/icons8-beer-glass-48.png");
		equipmentIcon = createImage("img/icons8-brewsystem-48.png");
		stylesIcon = createImage("img/icons8-test-passed-48.png");
		settingsIcon = createImage("img/icons8-settings-48.png");
		databaseIcon = createImage("img/icons8-database-48.png");
		inventoryIcon = createImage("img/icons8-trolley-48.png");
		exitIcon = createImage("img/icons8-close-window-48.png");
		saveIcon = createImage("img/icons8-save-48.png");
		undoIcon = createImage("img/icons8-undo-48.png");
		renameIcon = createImage("img/icons8-rename-48.png");
		helpIcon = createImage("img/icons8-help-48.png");
		documentIcon = createImage("img/icons8-document-48.png");
		addRecipe = createImage("img/add_recipe.png");
		addStep = createImage("img/add_step.png");
		addFermentable = createImage("img/add_fermentable.png");
		addHops = createImage("img/add_hop.png");
		addWater = createImage("img/add_water.png");
		addYeast = createImage("img/add_yeast.png");
		addMisc = createImage("img/add_misc.png");
		toolsIcon = createImage("img/icons8-full-tool-storage-box-48.png");
		importIcon = createImage("img/icons8-import-48.png");
		boilIcon = createImage("img/icons8-boiling-48.png");
		mashIcon = createImage("img/icons8-mash-in.png");
		mashInfusionIcon = createImage("img/icons8-mash-infusion.png");
		lauterIcon = createImage("img/icons8-lauter.png");
		batchSpargeIcon = createImage("img/icons8-batch-sparge.png");
		heatIcon = createImage("img/icons8-heating-48.png");
		coolIcon = createImage("img/icons8-cooling-48.png");
		splitIcon = createImage("img/icons8-split-48.png");
		combineIcon = createImage("img/icons8-merge-48.png");
		packageIcon = createImage("img/icons8-package-48.png");
		standIcon = createImage("img/icons8-sleep-mode-48.png");
		fermentIcon = createImage("img/icons8-glass-jar-48.png");
		diluteIcon = addWater;

		Database.getInstance().loadAll();

		primaryStage.setTitle(StringUtils.getUiString("ui.about.msg", UiUtils.getVersion()));
		primaryStage.getIcons().add(brewdayIcon);

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

		primaryStage.setScene(mainScene);
//		primaryStage.setMaximized(true);

		primaryStage.show();
	}

	/*-------------------------------------------------------------------------*/
	public static void styleScene(Scene scene)
	{
		String theme = Database.getInstance().getSettings().get(Settings.UI_THEME);

		scene.getStylesheets().clear();

		switch (theme)
		{
			case Settings.JMETRO_DARK:
				new JMetro(jfxtras.styles.jmetro.Style.DARK).setScene(scene);
				break;
			case Settings.JMETRO_LIGHT:
				new JMetro(jfxtras.styles.jmetro.Style.LIGHT).setScene(scene);
				break;
//			case Settings.MODENA:
//				setUserAgentStylesheet(STYLESHEET_MODENA);
//				break;
//			case Settings.CASPIAN:
//				setUserAgentStylesheet(STYLESHEET_CASPIAN);
//				break;
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
		if (isFeatureOn(Settings.FEATURE_TOGGLE_BATCHES))
		{
			cards.add(BATCHES, getBatchesPane());
		}
		cards.add(RECIPES, getRecipesCard());
		cards.add(EQUIPMENT_PROFILES, getEquipmentProfilesCard());
		cards.add(PROCESS_TEMPLATES, getProcessTemplatesCard());

		// inventory
		if (isFeatureOn(Settings.FEATURE_TOGGLE_INVENTORY))
		{
			cards.add(INVENTORY, new Label("coming soon"));
		}

		// ref database
		cards.add(WATER, getRefWaters());
		cards.add(HOPS, getHopsTable());
		cards.add(FERMENTABLES, getFermentablesTable());
		cards.add(YEAST, getYeastsTable());
		cards.add(MISC, getMiscsTable());
		cards.add(STYLES, getStylesTable());

		// tools
		cards.add(IMPORT, getImportPane());

		// help
		cards.add(ABOUT, getAboutPane());

		// settings
		cards.add(BREWING_SETTINGS, getBrewingSettingsCard());
		if (isFeatureOn(Settings.FEATURE_TOGGLE_REMOTE_BACKENDS))
		{
			cards.add(BACKEND_SETTINGS, new Label("coming soonish"));
		}
		if (isFeatureOn(Settings.FEATURE_TOGGLE_UI_SETTINGS))
		{
			cards.add(UI_SETTINGS, getUiSettingsCard());
		}

		return cards;
	}

	private Node getAboutPane()
	{
		MigPane result = new MigPane();

		result.add(new Label("", getImageView(brewdayIcon, ICON_SIZE)), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.msg", UiUtils.getVersion())), "wrap");
		result.add(new Label(StringUtils.getUiString("ui.about.url")), "wrap");
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

	private Node getBrewingSettingsCard()
	{
		return new MigPane();
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
		recipePane = new RecipePane(RECIPES, this);
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
		if (isFeatureOn(Settings.FEATURE_TOGGLE_BATCHES))
		{
			batchesPane.refresh(db);
		}
		recipePane.refresh(db);
		equipmentProfilePane.refresh(db);
		processTemplatePane.refresh(db);

		refWaterPane.refresh(db);
		refFermentablePane.refresh(db);
		refHopPane.refresh(db);
		refYeastPane.refresh(db);
		refMiscPane.refresh(db);
		refStylePane.refresh(db);

		if (isFeatureOn(Settings.FEATURE_TOGGLE_UI_SETTINGS))
		{
			uiSettingsPane.refresh(db);
		}

		detectDirty = true;
	}

	private Image createImage(String s) throws FileNotFoundException
	{
		return new Image(new FileInputStream(s));
	}

	private TreeView getNavMenuTreeView()
	{
		TreeItem root = new TreeItem("root");

		int size = 24;

		brewing = new TreeItem<>(new Label(getUiString("tab.brewing"), getImageView(beerIcon, size)));

		batches = new TreeItem<>(new Label(getUiString("tab.batches"), getImageView(JfxUi.beerIcon, size)));
		recipes = new TreeItem<>(new Label(getUiString("tab.recipes"), getImageView(recipeIcon, size)));
		processTemplates = new TreeItem<>(new Label(getUiString("tab.process.templates"), getImageView(processTemplateIcon, size)));
		equipmentProfiles = new TreeItem<>(new Label(getUiString("tab.equipment.profiles"), getImageView(equipmentIcon, size)));

		if (isFeatureOn(Settings.FEATURE_TOGGLE_BATCHES))
		{
			brewing.getChildren().add(batches);
		}
		brewing.getChildren().add(recipes);
		brewing.getChildren().add(processTemplates);
		brewing.getChildren().add(equipmentProfiles);


		TreeItem<Label> inventory = new TreeItem<>(new Label(getUiString("tab.inventory"), getImageView(inventoryIcon, size)));
		TreeItem<Label> inv1 = new TreeItem<>(new Label(getUiString("tab.inventory"), getImageView(inventoryIcon, size)));
		inventory.getChildren().add(inv1);

		refDatabase = new TreeItem<>(new Label(getUiString("tab.reference.database"), getImageView(databaseIcon, size)));

		water = new TreeItem<>(new Label(getUiString("tab.water"), getImageView(waterIcon, size)));
		fermentables = new TreeItem<>(new Label(getUiString("tab.fermentables"), getImageView(fermentableIcon, size)));
		hops = new TreeItem<>(new Label(getUiString("tab.hops"), getImageView(hopsIcon, size)));
		yeast = new TreeItem<>(new Label(getUiString("tab.yeast"), getImageView(yeastIcon, size)));
		misc = new TreeItem<>(new Label(getUiString("tab.misc"), getImageView(miscIcon, size)));
		styles = new TreeItem<>(new Label(getUiString("tab.styles"), getImageView(stylesIcon, size)));

		refDatabase.getChildren().addAll(water, fermentables, hops, yeast, misc, styles);

		TreeItem<Label> tools = new TreeItem<>(new Label(StringUtils.getUiString("tab.tools"), getImageView(toolsIcon, size)));

		importTools = new TreeItem<>(new Label(getUiString("tools.import"), getImageView(importIcon, size)));

		tools.getChildren().addAll(importTools);

		TreeItem<Label> settings = new TreeItem<>(new Label(StringUtils.getUiString("tab.settings"), getImageView(settingsIcon, size)));

		TreeItem<Label> brewingSettings = new TreeItem<>(new Label(StringUtils.getUiString("settings.brewing"), getImageView(settingsIcon, size)));
		TreeItem<Label> backendSettings = new TreeItem<>(new Label(StringUtils.getUiString("settings.backend"), getImageView(settingsIcon, size)));
		uiSettings = new TreeItem<>(new Label(StringUtils.getUiString("settings.ui"), getImageView(settingsIcon, size)));

		settings.getChildren().add(brewingSettings);
		if (isFeatureOn(Settings.FEATURE_TOGGLE_REMOTE_BACKENDS))
		{
			settings.getChildren().add(backendSettings);
		}
		if (isFeatureOn(Settings.FEATURE_TOGGLE_UI_SETTINGS))
		{
			settings.getChildren().add(uiSettings);
		}

		TreeItem<Label> help = new TreeItem<>(new Label(StringUtils.getUiString("ui.help"), getImageView(helpIcon, size)));
		TreeItem<Label> about = new TreeItem<>(new Label(StringUtils.getUiString("ui.about"), getImageView(brewdayIcon, size)));

		help.getChildren().addAll(about);

		root.getChildren().add(brewing);
		if (isFeatureOn(Settings.FEATURE_TOGGLE_INVENTORY))
		{
			root.getChildren().add(inventory);
		}
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
					if (selected != null)
					{
						cards.setVisible(selected);
					}
				}
			});

		cardsMap = new HashMap<>();
		cardsMap.put(batches, BATCHES);
		cardsMap.put(recipes, RECIPES);
		cardsMap.put(processTemplates, PROCESS_TEMPLATES);
		cardsMap.put(equipmentProfiles, EQUIPMENT_PROFILES);
		cardsMap.put(inventory, INVENTORY);
		cardsMap.put(water, WATER);
		cardsMap.put(fermentables, FERMENTABLES);
		cardsMap.put(hops, HOPS);
		cardsMap.put(yeast, YEAST);
		cardsMap.put(misc, MISC);
		cardsMap.put(styles, STYLES);
		cardsMap.put(brewingSettings, BREWING_SETTINGS);
		cardsMap.put(backendSettings, BACKEND_SETTINGS);
		cardsMap.put(uiSettings, UI_SETTINGS);
		cardsMap.put(importTools, IMPORT);
		cardsMap.put(about, ABOUT);

		treeItems = new HashMap<>();
		treeItems.put(BATCHES, batches);
		treeItems.put(RECIPES, recipes);
		treeItems.put(PROCESS_TEMPLATES, processTemplates);
		treeItems.put(EQUIPMENT_PROFILES, equipmentProfiles);
		treeItems.put(INVENTORY, inventory);
		treeItems.put(WATER, water);
		treeItems.put(FERMENTABLES, fermentables);
		treeItems.put(HOPS, hops);
		treeItems.put(YEAST, yeast);
		treeItems.put(MISC, misc);
		treeItems.put(STYLES, styles);
		treeItems.put(BREWING_SETTINGS, brewingSettings);
		treeItems.put(BACKEND_SETTINGS, backendSettings);
		treeItems.put(UI_SETTINGS, uiSettings);
		treeItems.put(IMPORT, importTools);
		treeItems.put(ABOUT, about);

		return treeView;
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
			for (Object obj : objs)
			{
				if (!this.isDirty(obj))
				{
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
							case HOPS:
							case FERMENTABLES:
							case YEAST:
							case MISC:
							case STYLES:
								refDatabase.getValue().setStyle(dirtyCss);
								break;

							case INVENTORY:
								// todo
								break;

							case IMPORT:
								// todo
								break;

							case BREWING_SETTINGS:
							case BACKEND_SETTINGS:
							case UI_SETTINGS:
								// todo
								break;

							case ABOUT:
								break;
						}
					}
				}

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
		fermentables.getValue().setStyle(null);
		hops.getValue().setStyle(null);
		yeast.getValue().setStyle(null);
		misc.getValue().setStyle(null);
		styles.getValue().setStyle(null);
		batches.getValue().setStyle(null);
		recipes.getValue().setStyle(null);
		processTemplates.getValue().setStyle(null);
		equipmentProfiles.getValue().setStyle(null);
//		uiSettings.getValue().setStyle(null);

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
	public static Image brewdayIcon, fermentableIcon, hopsIcon, waterIcon, stepIcon,
		recipeIcon, yeastIcon, miscIcon, removeIcon, increaseIcon, decreaseIcon,
		moreTimeIcon, lessTimeIcon, searchIcon, editIcon, newIcon, deleteIcon,
		duplicateIcon, substituteIcon, processTemplateIcon, beerIcon, equipmentIcon,
		settingsIcon, stylesIcon, databaseIcon, inventoryIcon, exitIcon, saveIcon,
		undoIcon, renameIcon, helpIcon, documentIcon, addRecipe, addStep,
		addFermentable, addHops, addWater, addYeast, addMisc, toolsIcon, importIcon,
		boilIcon, mashIcon, mashInfusionIcon, heatIcon, coolIcon, splitIcon, combineIcon,
		packageIcon, standIcon, diluteIcon, fermentIcon, batchSpargeIcon, lauterIcon;
}
