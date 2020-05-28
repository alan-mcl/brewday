package mclachlan.brewday.ui.jfx;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.UiUtils;

import static mclachlan.brewday.StringUtils.getUiString;

public class JfxUi extends Application
{
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

	private CardGroup cards;
	private WaterTable waterTable;
	private EquipmentProfilePane equipmentProfilePane;
	private RecipePane recipePane;

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
		grainsIcon = createImage("img/icons8-carbohydrates-48.png");
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

		primaryStage.setTitle("Brewday " + UiUtils.getVersion()); // todo, localise
		primaryStage.getIcons().add(new Image(new FileInputStream("img/brewday.png")));

		HBox root = new HBox(5);

		Database.getInstance().loadAll();

		Group cards = getCards();

		Region navMenu = getNavMenuTreeView();

		root.getChildren().add(navMenu);
		root.getChildren().add(cards);

		refreshCards();

		primaryStage.setScene(new Scene(root, 1024, 768));
		primaryStage.show();
	}

	private Group getCards()
	{
		cards = new CardGroup();

		// brewing
		// todo the rest
		cards.add(RECIPES, getRecipesCard());
		cards.add(EQUIPMENT_PROFILES, getEquipmentProfilesCard());

		// inventory
		// todo

		// ref database
		cards.add(WATER, getRefWaters());
		cards.add(HOPS, getHopsTable());
		cards.add(FERMENTABLES, getFermentablesTable("fermentables"));
		cards.add(YEAST, getYeastsTable());
		cards.add(MISC, getMiscsTable());
		cards.add(STYLES, getStylesTable());

		// settings
		// todo

		return cards;
	}

	private Node getRecipesCard()
	{
		recipePane = new RecipePane(RECIPES);
		return recipePane;
	}

	private Node getEquipmentProfilesCard()
	{
		equipmentProfilePane = new EquipmentProfilePane(EQUIPMENT_PROFILES);
		return equipmentProfilePane;
	}

	private Label getStylesTable()
	{
		return new Label(STYLES);
	}

	private Label getMiscsTable()
	{
		return new Label(MISC);
	}

	private Label getYeastsTable()
	{
		return new Label(YEAST);
	}

	private Label getFermentablesTable(String fermentables)
	{
		return new Label(fermentables);
	}

	private Label getHopsTable()
	{
		return new Label(HOPS);
	}

	private WaterTable getRefWaters()
	{
		waterTable = new WaterTable();
		return waterTable;
	}

	private void refreshCards()
	{
		detectDirty = false;

		Database db = Database.getInstance();
		recipePane.refresh(db);
		equipmentProfilePane.refresh(db);
		waterTable.refresh(db);

		detectDirty = true;
	}

	private Image createImage(String s) throws FileNotFoundException
	{
		return new Image(new FileInputStream(s));
	}

	private Control getNavMenuTreeView()
	{
		TreeItem root = new TreeItem("root");


		TreeItem<Label> brewing = new TreeItem<>(new Label(getUiString("tab.brewing"), getImageView(beerIcon)));

		batches = new TreeItem<>(new Label(getUiString("tab.batches"), getImageView(JfxUi.beerIcon)));
		recipes = new TreeItem<>(new Label(getUiString("tab.recipes"), getImageView(recipeIcon)));
		processTemplates = new TreeItem<>(new Label(getUiString("tab.process.templates"), getImageView(processTemplateIcon)));
		equipmentProfiles = new TreeItem<>(new Label(getUiString("tab.equipment.profiles"), getImageView(equipmentIcon)));

		brewing.getChildren().addAll(batches, recipes, processTemplates, equipmentProfiles);

		TreeItem<Label> inventory = new TreeItem<>(new Label(getUiString("tab.inventory"), getImageView(inventoryIcon)));
		TreeItem<Label> inv1 = new TreeItem<>(new Label(getUiString("tab.inventory"), getImageView(inventoryIcon)));
		inventory.getChildren().add(inv1);

		TreeItem<Label> refDatabase = new TreeItem<>(new Label(getUiString("tab.reference.database"), getImageView(databaseIcon)));

		water = new TreeItem<>(new Label(getUiString("tab.water"), getImageView(waterIcon)));
		fermentables = new TreeItem<>(new Label(getUiString("tab.fermentables"), getImageView(grainsIcon)));
		hops = new TreeItem<>(new Label(getUiString("tab.hops"), getImageView(hopsIcon)));
		yeast = new TreeItem<>(new Label(getUiString("tab.yeast"), getImageView(yeastIcon)));
		misc = new TreeItem<>(new Label(getUiString("tab.misc"), getImageView(miscIcon)));
		styles = new TreeItem<>(new Label(getUiString("tab.styles"), getImageView(stylesIcon)));

		refDatabase.getChildren().addAll(water, fermentables, hops, yeast, misc, styles);

		TreeItem<Label> settings = new TreeItem<>(new Label("Settings", getImageView(settingsIcon)));

		TreeItem<Label> brewing_settings = new TreeItem<>(new Label("Brewing Settings", getImageView(settingsIcon)));
		TreeItem<Label> backend_settings = new TreeItem<>(new Label("Backend Settings", getImageView(settingsIcon)));
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

		treeView.setShowRoot(false);

		treeView.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null && newValue != oldValue)
				{
					if (equipmentProfiles == newValue)
					{
						cards.setVisible(EQUIPMENT_PROFILES);
					}
					else if (recipes == newValue)
					{
						cards.setVisible(RECIPES);
					}
					else if (water == newValue)
					{
						cards.setVisible(WATER);
					}
					else if (fermentables == newValue)
					{
						cards.setVisible(FERMENTABLES);
					}
					else if (hops == newValue)
					{
						cards.setVisible(HOPS);
					}
					else if (yeast == newValue)
					{
						cards.setVisible(YEAST);
					}
					else if (misc == newValue)
					{
						cards.setVisible(MISC);
					}
					else if (styles == newValue)
					{
						cards.setVisible(STYLES);
					}
				}
			});

		return treeView;
	}

	/*-------------------------------------------------------------------------*/
	public void setDirty(String dirtyFlag)
	{
		if (detectDirty)
		{
			if (EQUIPMENT_PROFILES.equalsIgnoreCase(dirtyFlag))
			{
				equipmentProfiles.getValue().setStyle("-fx-font-weight: bold;");
			}
			else if (RECIPES.equalsIgnoreCase(dirtyFlag))
			{
				recipes.getValue().setStyle("-fx-font-weight: bold;");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private ImageView getImageView(Image i)
	{
		ImageView result = new ImageView(i);
		result.setPreserveRatio(true);
		int size = 32;
		result.setFitHeight(size);
		result.setFitWidth(size);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static Image brewdayIcon;
	public static Image grainsIcon;
	public static Image hopsIcon;
	public static Image waterIcon;
	public static Image stepIcon;
	public static Image recipeIcon;
	public static Image yeastIcon;
	public static Image miscIcon;
	public static Image removeIcon;
	public static Image increaseIcon;
	public static Image decreaseIcon;
	public static Image moreTimeIcon;
	public static Image lessTimeIcon;
	public static Image searchIcon;
	public static Image editIcon;
	public static Image newIcon;
	public static Image deleteIcon;
	public static Image duplicateIcon;
	public static Image substituteIcon;
	public static Image processTemplateIcon;
	public static Image beerIcon;
	public static Image equipmentIcon;
	public static Image settingsIcon;
	public static Image stylesIcon;
	public static Image databaseIcon;
	public static Image inventoryIcon;
	public static Image exitIcon;
	public static Image saveIcon;
	public static Image undoIcon;
	public static Image renameIcon;
	public static Image helpIcon;
	public static Image documentIcon;

}
