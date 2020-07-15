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
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
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
	public static final String SETTINGS = "settings";

	public static final String BREWING_SETTINGS = "brewingSettings";
	public static final String BACKEND_SETTINGS = "backendSettings";

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

	private boolean detectDirty = true;
	private static JfxUi instance;
	private TreeItem<Label> brewing;
	private TreeItem<Label> refDatabase;

	private Map<String, TreeItem<Label>> treeItems;
	private Map<TreeItem<Label>, String> cardsMap;

	private final Set<Object> dirty = new HashSet<>();

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

		primaryStage.setTitle("Brewday " + UiUtils.getVersion()); // todo, localise
		primaryStage.getIcons().add(brewdayIcon);

		MigPane root = new MigPane("gap 3, insets 3");

		Database.getInstance().loadAll();

		Group cards = getCards();

		TreeView navMenu = getNavMenuTreeView();
//		navMenu.setPadding(new Insets(3,3,3,3));
		navMenu.setPrefWidth(180);


		root.add(navMenu, "dock west");
		root.add(cards, "dock center, aligny top");

		refreshCards();

		// initial selection
		// todo this leaves that weird blue rectangle dunno why
//		navMenu.requestFocus();
//		brewing.setExpanded(true);
//		int recipesRow = navMenu.getRow(recipes);
//		navMenu.getSelectionModel().clearSelection();
//		navMenu.getSelectionModel().clearAndSelect(recipesRow);


		Scene scene = new Scene(root, 1280, 768);

		styleScene(scene);

		primaryStage.setScene(scene);
//		primaryStage.setMaximized(true);

//		primaryStage.getScene().getStylesheets().clear();
//		primaryStage.getScene().setUserAgentStylesheet(null);
//		primaryStage.getScene().getStylesheets().add(getClass().getResource("/sample/classic.css").toExternalForm());

		primaryStage.show();
	}

	/*-------------------------------------------------------------------------*/
	public static void styleScene(Scene scene)
	{
		// JMetro
		JMetro jMetro = new JMetro(jfxtras.styles.jmetro.Style.LIGHT);
		jMetro.setScene(scene);

		// jbootfx
//		scene.getStylesheets().add(
//			JfxUi.class.getResource("bootstrap3.css").toExternalForm());
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
		// todo

		// ref database
		cards.add(WATER, getRefWaters());
		cards.add(HOPS, getHopsTable());
		cards.add(FERMENTABLES, getFermentablesTable());
		cards.add(YEAST, getYeastsTable());
		cards.add(MISC, getMiscsTable());
		cards.add(STYLES, getStylesTable());

		// settings
		// todo


		return cards;
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
		batchesPane.refresh(db);
		recipePane.refresh(db);
		equipmentProfilePane.refresh(db);
		processTemplatePane.refresh(db);

		refWaterPane.refresh(db);
		refFermentablePane.refresh(db);
		refHopPane.refresh(db);
		refYeastPane.refresh(db);
		refMiscPane.refresh(db);
		refStylePane.refresh(db);

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

		brewing.getChildren().addAll(batches, recipes, processTemplates, equipmentProfiles);

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

		TreeItem<Label> settings = new TreeItem<>(new Label("Settings", getImageView(settingsIcon, size)));

		TreeItem<Label> brewing_settings = new TreeItem<>(new Label("Brewing Settings", getImageView(settingsIcon, size)));
		TreeItem<Label> backend_settings = new TreeItem<>(new Label("Backend Settings", getImageView(settingsIcon, size)));
		settings.getChildren().addAll(
			brewing_settings,
			backend_settings);

		root.getChildren().addAll(
			brewing,
			inventory,
			refDatabase,
			settings
		);

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
		cardsMap.put(settings, SETTINGS);

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
		treeItems.put(SETTINGS, settings);

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
				this.dirty.add(obj);

				if (obj instanceof String)
				{
					String dirtyFlag = (String)obj;

					TreeItem<Label> treeItem = treeItems.get(dirtyFlag);
					treeItem.getValue().setStyle("-fx-font-weight: bold;");
					// treeItem.getParent().getValue().setStyle("-fx-font-weight: bold;"); todo
				}
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
		addFermentable, addHops, addWater, addYeast, addMisc;
}
