package mclachlan.brewday.ui.swing.app;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.util.AppContentRoot;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;

public class SwingIcons
{
	public static final int NAV_ICON_SIZE = 32;
	public static final int TREE_ICON_SIZE = 32;
	/** Icon size for parent-nav landing tile buttons (navigation hub screens). */
	public static final int LANDING_NAV_ICON_SIZE = 48;
	public static final int TOOLBAR_ICON_SIZE = 32;
	/** Icon size for ingredient tables (reference DB and inventory). */
	public static final int TABLE_ICON_SIZE = 24;
	public static final int WINDOW_ICON_16 = 16;
	public static final int WINDOW_ICON_32 = 32;
	public static final int WINDOW_ICON_64 = 64;

	/** Sizes for {@link javax.swing.JFrame#setIconImages}: title bar and OS taskbar/dock (HiDPI-friendly). */
	public static final int[] WINDOW_ICON_SIZES = { 16, 24, 32, 48, 64, 128 };

	/** Default JTree row height for nav and recipe trees (icon + padding). */
	public static final int TREE_ROW_HEIGHT = 36;

	/** Default JTable row height when showing icon + name in column 0. */
	public static final int TABLE_ROW_HEIGHT = 28;

	public enum IconKey
	{
		BREWDAY,
		BEER,
		BEER_NARROW,
		RECIPE,
		PROCESS_TEMPLATE,
		EQUIPMENT,
		INVENTORY,
		DATABASE,
		WATER,
		WATER_PARAMETERS,
		FERMENTABLE,
		HOPS,
		YEAST,
		MISC,
		STYLES,
		TOOLS,
		IMPORT,
		GRAPH,
		WATER_BUILDER,
		SETTINGS,
		MASH,
		GIT,
		HELP,
		ADD_WATER,
		ADD_FERMENTABLE,
		ADD_HOPS,
		ADD_YEAST,
		ADD_MISC,
		EDIT,
		DELETE,
		SAVE,
		UNDO,
		CANCEL,
		FILTER,
		OK,
		DUPLICATE,
		SUBSTITUTE,
		EXPORT_CSV,
		ADD_STEP,
		RENAME,
		STEP,
		MASH_INFUSION,
		LAUTER,
		BATCH_SPARGE,
		FLY_SPARGE,
		BOIL,
		HEAT,
		COOL,
		SPLIT,
		COMBINE,
		DILUTE,
		STAND,
		HOP_STAND,
		STEEP,
		YEAST_REHYDRATE,
		FERMENT,
		PACKAGE,
		FREEZE_CONCENTRATE,
		PROCESS_TEMPLATE_APPLY,
		VOLUME_MASH,
		VOLUME_WORT,
		VOLUME_BEER,
		KEG_LINE_LENGTH,
		YEAST_CALCULATOR,
		RECIPE_TAG_MANAGER,
		WHAT_SHOULD_I_BREW,
		LOCAL_FILESYSTEM,
		FERMENTABLE_GRAIN,
		FERMENTABLE_SUGAR,
		FERMENTABLE_DRY_EXTRACT,
		FERMENTABLE_ADJUNCT,
		FERMENTABLE_JUICE,
		FERMENTABLE_HONEY,
		FERMENTABLE_LIQUID_EXTRACT,
		MISC_SPICE,
		MISC_WATER_AGENT,
		MISC_FINING,
		MISC_HERB,
		MISC_FLAVOUR,
		MISC_OTHER,
		YEAST_CULTURE
	}

	private static final ImageIcon EMPTY_ICON = new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
	private static final Map<IconKey, String> PATHS = buildPaths();
	private static final Map<IconKey, ImageIcon> BASE_CACHE = new EnumMap<>(IconKey.class);
	private static final Map<String, ImageIcon> SCALED_CACHE = new HashMap<>();
	private static final Map<IconKey, Boolean> MISSING_LOGGED = new EnumMap<>(IconKey.class);

	private SwingIcons()
	{
	}

	public static ImageIcon navIcon(IconKey key)
	{
		return icon(key, NAV_ICON_SIZE);
	}

	public static ImageIcon treeIcon(IconKey key)
	{
		return icon(key, TREE_ICON_SIZE);
	}

	public static ImageIcon toolbarIcon(IconKey key)
	{
		return icon(key, TOOLBAR_ICON_SIZE);
	}

	public static ImageIcon tableIcon(IconKey key)
	{
		return icon(key, TABLE_ICON_SIZE);
	}

	public static Icon tableNavIcon(ScreenKey screenKey)
	{
		return tableIcon(navKey(screenKey));
	}

	/**
	 * Approximate beer colour for UI tinting from SRM (visual chart interpolation, not lab colour).
	 */
	public static Color colorForSrm(double srm)
	{
		if (srm < 0)
		{
			srm = 0;
		}
		if (srm > 50)
		{
			srm = 50;
		}
		final double[][] stops = {
			{0, 245, 245, 170},
			{2, 245, 238, 130},
			{4, 245, 220, 90},
			{6, 245, 200, 70},
			{8, 245, 180, 50},
			{10, 245, 155, 35},
			{13, 240, 130, 25},
			{17, 230, 100, 20},
			{20, 220, 75, 15},
			{24, 200, 55, 10},
			{29, 180, 40, 8},
			{35, 140, 25, 5},
			{40, 80, 15, 3},
			{50, 30, 10, 2}
		};
		for (int i = 0; i < stops.length - 1; i++)
		{
			if (srm <= stops[i + 1][0])
			{
				double t = (srm - stops[i][0]) / (stops[i + 1][0] - stops[i][0]);
				int r = (int)Math.round(stops[i][1] + t * (stops[i + 1][1] - stops[i][1]));
				int g = (int)Math.round(stops[i][2] + t * (stops[i + 1][2] - stops[i][2]));
				int b = (int)Math.round(stops[i][3] + t * (stops[i + 1][3] - stops[i][3]));
				return new Color(clampRgb(r), clampRgb(g), clampRgb(b));
			}
		}
		double[] last = stops[stops.length - 1];
		return new Color(clampRgb((int)last[1]), clampRgb((int)last[2]), clampRgb((int)last[3]));
	}

	public static Icon tintedTableBeerIcon(double srm)
	{
		double bucket = Math.round(srm * 2) / 2.0;
		String cacheKey = "BEER_TINT:" + TABLE_ICON_SIZE + ":" + bucket;
		ImageIcon cached = SCALED_CACHE.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}
		ImageIcon base = tableIcon(IconKey.BEER_NARROW);
		if (base == EMPTY_ICON)
		{
			return EMPTY_ICON;
		}
		ImageIcon tinted = tintIcon(base, colorForSrm(srm));
		SCALED_CACHE.put(cacheKey, tinted);
		return tinted;
	}

	public static ImageIcon icon(IconKey key, int size)
	{
		String cacheKey = key.name() + ":" + size;
		ImageIcon cached = SCALED_CACHE.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}

		ImageIcon base = baseIcon(key);
		if (base == EMPTY_ICON)
		{
			return EMPTY_ICON;
		}

		Image image = base.getImage();
		Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
		ImageIcon result = new ImageIcon(scaled);
		SCALED_CACHE.put(cacheKey, result);
		return result;
	}

	public static Image windowIcon(int size)
	{
		return icon(IconKey.BREWDAY, size).getImage();
	}

	/**
	 * Multi-resolution images from {@code data/img/brewday.png} for the main shell frame (PNG;
	 * {@code brewday.ico} is for native wrappers only — core Java does not load ICO reliably here).
	 */
	public static List<Image> brewdayWindowImages()
	{
		ArrayList<Image> images = new ArrayList<>(WINDOW_ICON_SIZES.length);
		for (int size : WINDOW_ICON_SIZES)
		{
			images.add(windowIcon(size));
		}
		return Collections.unmodifiableList(images);
	}

	public static Icon emptyIcon()
	{
		return EMPTY_ICON;
	}

	public static IconKey stepTypeIcon(ProcessStep.Type type)
	{
		return switch (type)
		{
			case MASH -> IconKey.MASH;
			case MASH_INFUSION -> IconKey.MASH_INFUSION;
			case LAUTER -> IconKey.LAUTER;
			case BATCH_SPARGE -> IconKey.BATCH_SPARGE;
			case FLY_SPARGE -> IconKey.FLY_SPARGE;
			case BOIL -> IconKey.BOIL;
			case HEAT -> IconKey.HEAT;
			case COOL -> IconKey.COOL;
			case SPLIT -> IconKey.SPLIT;
			case COMBINE -> IconKey.COMBINE;
			case DILUTE -> IconKey.DILUTE;
			case STAND -> IconKey.STAND;
			case HOP_STAND -> IconKey.HOP_STAND;
			case STEEP -> IconKey.STEEP;
			case YEAST_REHYDRATE -> IconKey.YEAST_REHYDRATE;
			case FERMENT -> IconKey.FERMENT;
			case PACKAGE -> IconKey.PACKAGE;
			case FREEZE_CONCENTRATE -> IconKey.FREEZE_CONCENTRATE;
			default -> IconKey.STEP;
		};
	}

	public static IconKey volumeTypeIcon(Volume.Type type)
	{
		if (type == null)
		{
			return IconKey.STEP;
		}
		return switch (type)
		{
			case MASH -> IconKey.VOLUME_MASH;
			case WORT -> IconKey.VOLUME_WORT;
			case BEER -> IconKey.VOLUME_BEER;
		};
	}

	public static IconKey iconKeyFor(IngredientAddition.Type type)
	{
		if (type == null)
		{
			return IconKey.STEP;
		}
		return switch (type)
		{
			case FERMENTABLES -> IconKey.FERMENTABLE;
			case HOPS -> IconKey.HOPS;
			case WATER -> IconKey.WATER;
			case YEAST -> IconKey.YEAST;
			case MISC -> IconKey.MISC;
			case YEAST_CULTURE -> IconKey.YEAST_CULTURE;
		};
	}

	public static IconKey iconKeyFor(Fermentable.Type type)
	{
		if (type == null)
		{
			return IconKey.FERMENTABLE;
		}
		return switch (type)
		{
			case GRAIN -> IconKey.FERMENTABLE_GRAIN;
			case SUGAR -> IconKey.FERMENTABLE_SUGAR;
			case DRY_EXTRACT -> IconKey.FERMENTABLE_DRY_EXTRACT;
			case ADJUNCT -> IconKey.FERMENTABLE_ADJUNCT;
			case JUICE -> IconKey.FERMENTABLE_JUICE;
			case HONEY -> IconKey.FERMENTABLE_HONEY;
			case LIQUID_EXTRACT -> IconKey.FERMENTABLE_LIQUID_EXTRACT;
			default -> IconKey.FERMENTABLE;
		};
	}

	public static IconKey iconKeyFor(Misc misc)
	{
		if (misc == null)
		{
			return IconKey.MISC;
		}
		return iconKeyFor(misc.getType(), misc.getMeasurementType());
	}

	public static IconKey iconKeyFor(Misc.Type type, Quantity.Type measurementType)
	{
		if (type == null)
		{
			return IconKey.MISC;
		}
		if (type == Misc.Type.WATER_AGENT)
		{
			Quantity.Type mt = measurementType == null ? Quantity.Type.WEIGHT : measurementType;
			return mt == Quantity.Type.VOLUME ? IconKey.MISC_WATER_AGENT : IconKey.MISC;
		}
		return iconKeyForMiscSubtype(type);
	}

	public static IconKey iconKeyFor(Misc.Type type)
	{
		return iconKeyFor(type, null);
	}

	private static IconKey iconKeyForMiscSubtype(Misc.Type type)
	{
		return switch (type)
		{
			case SPICE -> IconKey.MISC_SPICE;
			case FINING -> IconKey.MISC_FINING;
			case HERB -> IconKey.MISC_HERB;
			case FLAVOUR -> IconKey.MISC_FLAVOUR;
			case OTHER -> IconKey.MISC_OTHER;
			default -> IconKey.MISC;
		};
	}

	public static IconKey iconKeyFor(V2DataObject row)
	{
		if (row instanceof Fermentable fermentable)
		{
			return iconKeyFor(fermentable.getType());
		}
		if (row instanceof Misc misc)
		{
			return iconKeyFor(misc);
		}
		if (row instanceof Hop)
		{
			return IconKey.HOPS;
		}
		if (row instanceof Yeast)
		{
			return IconKey.YEAST;
		}
		if (row instanceof Water)
		{
			return IconKey.WATER;
		}
		return IconKey.STEP;
	}

	public static IconKey iconKeyFor(IngredientAddition addition)
	{
		if (addition == null)
		{
			return IconKey.STEP;
		}
		if (addition instanceof FermentableAddition fa)
		{
			Fermentable fermentable = fa.getFermentable();
			if (fermentable != null)
			{
				return iconKeyFor(fermentable.getType());
			}
		}
		if (addition instanceof MiscAddition ma)
		{
			Misc misc = ma.getMisc();
			if (misc != null)
			{
				return iconKeyFor(misc);
			}
		}
		if (addition instanceof HopAddition)
		{
			return IconKey.HOPS;
		}
		if (addition instanceof YeastAddition)
		{
			return IconKey.YEAST;
		}
		if (addition instanceof WaterAddition)
		{
			return IconKey.WATER;
		}
		return iconKeyFor(addition.getType());
	}

	public static IconKey iconKeyForReferenceName(IngredientAddition.Type type, String name)
	{
		if (type == null || name == null || name.isEmpty())
		{
			return iconKeyFor(type);
		}
		Database db = Database.getInstance();
		return switch (type)
		{
			case FERMENTABLES ->
			{
				Fermentable fermentable = db.getFermentables().get(name);
				yield fermentable != null ? iconKeyFor(fermentable.getType()) : iconKeyFor(IngredientAddition.Type.FERMENTABLES);
			}
			case MISC ->
			{
				Misc misc = db.getMiscs().get(name);
				yield misc != null ? iconKeyFor(misc) : iconKeyFor(IngredientAddition.Type.MISC);
			}
			case HOPS -> IconKey.HOPS;
			case YEAST -> IconKey.YEAST;
			case YEAST_CULTURE -> IconKey.YEAST_CULTURE;
			case WATER -> IconKey.WATER;
		};
	}

	public static IconKey iconKeyFor(InventoryLineItem item)
	{
		if (item == null)
		{
			return IconKey.STEP;
		}
		return iconKeyForReferenceName(item.getType(), item.getIngredient());
	}

	public static Icon iconFor(IngredientAddition.Type type)
	{
		return tableIcon(iconKeyFor(type));
	}

	public static Icon iconFor(Fermentable.Type type)
	{
		return tableIcon(iconKeyFor(type));
	}

	public static Icon iconFor(Misc.Type type)
	{
		return tableIcon(iconKeyFor(type));
	}

	public static Icon iconFor(Misc misc)
	{
		return tableIcon(iconKeyFor(misc));
	}

	public static Icon iconForReference(V2DataObject row)
	{
		return tableIcon(iconKeyFor(row));
	}

	public static Icon iconForAddition(IngredientAddition addition)
	{
		return icon(iconKeyFor(addition), TREE_ICON_SIZE);
	}

	public static Icon iconForInventoryLine(InventoryLineItem item)
	{
		return tableIcon(iconKeyFor(item));
	}

	public static Icon iconForReferenceName(IngredientAddition.Type type, String name)
	{
		return tableIcon(iconKeyForReferenceName(type, name));
	}

	public static IconKey navKey(ScreenKey screenKey)
	{
		return switch (screenKey)
		{
			case BREWING, BATCHES -> IconKey.BEER;
			case RECIPES -> IconKey.RECIPE;
			case PROCESS_TEMPLATES -> IconKey.PROCESS_TEMPLATE;
			case EQUIPMENT_PROFILES -> IconKey.EQUIPMENT;
			case INVENTORY_GROUP, INVENTORY -> IconKey.INVENTORY;
			case REFERENCE_DATABASE -> IconKey.DATABASE;
			case WATER -> IconKey.WATER;
			case WATER_PARAMETERS -> IconKey.WATER_PARAMETERS;
			case FERMENTABLES -> IconKey.FERMENTABLE;
			case HOPS, BREWING_SETTINGS_IBU -> IconKey.HOPS;
			case YEAST -> IconKey.YEAST;
			case MISC -> IconKey.MISC;
			case STYLES -> IconKey.STYLES;
			case TOOLS -> IconKey.TOOLS;
			case IMPORT -> IconKey.IMPORT;
			case WATER_BUILDER -> IconKey.WATER_BUILDER;
			case KEG_LINE_LENGTH -> IconKey.KEG_LINE_LENGTH;
			case YEAST_CALCULATOR -> IconKey.YEAST_CALCULATOR;
			case RECIPE_TAG_MANAGER -> IconKey.RECIPE_TAG_MANAGER;
			case WHAT_SHOULD_I_BREW, UI_SETTINGS_RECOMMENDATIONS -> IconKey.WHAT_SHOULD_I_BREW;
			case SETTINGS, BREWING_SETTINGS, BREWING_SETTINGS_GENERAL, BACKEND_SETTINGS, UI_SETTINGS, UI_SETTINGS_APPEARANCE, UI_SETTINGS_UNITS -> IconKey.SETTINGS;
			case BREWING_SETTINGS_MASH -> IconKey.MASH;
			case BACKEND_SETTINGS_LOCAL_FILESYSTEM -> IconKey.LOCAL_FILESYSTEM;
			case BACKEND_SETTINGS_GIT -> IconKey.GIT;
			case HELP -> IconKey.HELP;
			case ABOUT -> IconKey.BREWDAY;
		};
	}

	private static ImageIcon baseIcon(IconKey key)
	{
		ImageIcon cached = BASE_CACHE.get(key);
		if (cached != null)
		{
			return cached;
		}

		String path = PATHS.get(key);
		if (path == null)
		{
			logMissing(key, "No icon path configured");
			BASE_CACHE.put(key, EMPTY_ICON);
			return EMPTY_ICON;
		}

		ImageIcon result = loadIcon(path);
		if (result == null)
		{
			logMissing(key, "Missing icon path: " + path);
			result = EMPTY_ICON;
		}

		BASE_CACHE.put(key, result);
		return result;
	}

	private static ImageIcon loadIcon(String path)
	{
		String normalized = path.startsWith("/") ? path.substring(1) : path;
		ClassLoader classLoader = SwingIcons.class.getClassLoader();
		URL url = classLoader.getResource(normalized);
		if (url != null)
		{
			return new ImageIcon(url);
		}

		File file = new File(path);
		if (file.exists())
		{
			return new ImageIcon(path);
		}

		File rooted = AppContentRoot.resolveFile(path);
		if (rooted.isFile())
		{
			return new ImageIcon(rooted.getAbsolutePath());
		}

		return null;
	}

	private static Map<IconKey, String> buildPaths()
	{
		Map<IconKey, String> map = new EnumMap<>(IconKey.class);
		map.put(IconKey.BREWDAY, "data/img/brewday.png");
		map.put(IconKey.BEER, "data/img/icons8-beer-glass-48.png");
		map.put(IconKey.BEER_NARROW, "data/img/icons8-beer-glass-48.png");
		map.put(IconKey.RECIPE, "data/img/icons8-beer-recipe-48.png");
		map.put(IconKey.PROCESS_TEMPLATE, "data/img/icons8-flow-48.png");
		map.put(IconKey.EQUIPMENT, "data/img/icons8-brewsystem-48.png");
		map.put(IconKey.INVENTORY, "data/img/icons8-trolley-48.png");
		map.put(IconKey.DATABASE, "data/img/icons8-database-48.png");
		map.put(IconKey.WATER, "data/img/icons8-water-48.png");
		map.put(IconKey.WATER_PARAMETERS, "data/img/water_parameters.png");
		map.put(IconKey.FERMENTABLE, "data/img/icons8-carbohydrates-48.png");
		map.put(IconKey.HOPS, "data/img/icons8-hops-48.png");
		map.put(IconKey.YEAST, "data/img/icons8-experiment-48.png");
		map.put(IconKey.MISC, "data/img/icons8-sugar-cubes-48.png");
		map.put(IconKey.STYLES, "data/img/icons8-test-passed-48.png");
		map.put(IconKey.TOOLS, "data/img/icons8-full-tool-storage-box-48.png");
		map.put(IconKey.IMPORT, "data/img/icons8-import-48.png");
		map.put(IconKey.GRAPH, "data/img/icons8-graph-48.png");
		map.put(IconKey.WATER_BUILDER, "data/img/water_builder.png");
		map.put(IconKey.SETTINGS, "data/img/icons8-settings-48.png");
		map.put(IconKey.MASH, "data/img/icons8-mash-in.png");
		map.put(IconKey.GIT, "data/img/icons8-git-48.png");
		map.put(IconKey.HELP, "data/img/icons8-help-48.png");
		map.put(IconKey.ADD_WATER, "data/img/add_water.png");
		map.put(IconKey.ADD_FERMENTABLE, "data/img/add_fermentable.png");
		map.put(IconKey.ADD_HOPS, "data/img/add_hop.png");
		map.put(IconKey.ADD_YEAST, "data/img/add_yeast.png");
		map.put(IconKey.ADD_MISC, "data/img/add_misc.png");
		map.put(IconKey.EDIT, "data/img/icons8-edit-property-48.png");
		map.put(IconKey.DELETE, "data/img/icons8-delete-48.png");
		map.put(IconKey.SAVE, "data/img/icons8-save-48.png");
		map.put(IconKey.UNDO, "data/img/icons8-undo-48.png");
		map.put(IconKey.CANCEL, "data/img/icons8-cancel-48.png");
		map.put(IconKey.FILTER, "data/img/icons8-filter-48.png");
		map.put(IconKey.OK, "data/img/icons8-ok-48.png");
		map.put(IconKey.DUPLICATE, "data/img/icons8-transfer-48.png");
		map.put(IconKey.SUBSTITUTE, "data/img/icons8-replace-48.png");
		map.put(IconKey.EXPORT_CSV, "data/img/icons8-export-csv-48.png");
		map.put(IconKey.ADD_STEP, "data/img/add_step.png");
		map.put(IconKey.RENAME, "data/img/icons8-rename-48.png");
		map.put(IconKey.STEP, "data/img/icons8-file-48.png");
		map.put(IconKey.MASH_INFUSION, "data/img/icons8-mash-infusion.png");
		map.put(IconKey.LAUTER, "data/img/icons8-lauter.png");
		map.put(IconKey.BATCH_SPARGE, "data/img/batch-sparge.png");
		map.put(IconKey.FLY_SPARGE, "data/img/fly_sparge.png");
		map.put(IconKey.BOIL, "data/img/icons8-boiling-48.png");
		map.put(IconKey.HEAT, "data/img/icons8-heating-48.png");
		map.put(IconKey.COOL, "data/img/icons8-cooling-48.png");
		map.put(IconKey.SPLIT, "data/img/icons8-split-48.png");
		map.put(IconKey.COMBINE, "data/img/icons8-merge-48.png");
		map.put(IconKey.STAND, "data/img/icons8-sleep-mode-48.png");
		map.put(IconKey.HOP_STAND, "data/img/hop_stand.png");
		map.put(IconKey.STEEP, "data/img/steep.png");
		map.put(IconKey.YEAST_REHYDRATE, "data/img/yeast_rehydrate.png");
		map.put(IconKey.FERMENT, "data/img/icons8-glass-jar-48.png");
		map.put(IconKey.PACKAGE, "data/img/icons8-package-48.png");
		map.put(IconKey.DILUTE, "data/img/add_water.png");
		map.put(IconKey.FREEZE_CONCENTRATE, "data/img/icons8-freeze-48.png");
		map.put(IconKey.PROCESS_TEMPLATE_APPLY, "data/img/icons8-flow-48.png");
		map.put(IconKey.VOLUME_MASH, "data/img/icons8-mash-in.png");
		map.put(IconKey.VOLUME_WORT, "data/img/icons8-boiling-48.png");
		map.put(IconKey.VOLUME_BEER, "data/img/icons8-beer-bottle-48.png");
		map.put(IconKey.KEG_LINE_LENGTH, "data/img/keg_calculator.png");
		map.put(IconKey.YEAST_CALCULATOR, "data/img/yeast_calculator.png");
		map.put(IconKey.RECIPE_TAG_MANAGER, "data/img/icons8-beer-recipe-48.png");
		map.put(IconKey.WHAT_SHOULD_I_BREW, "data/img/icons8-idea-48.png");
		map.put(IconKey.LOCAL_FILESYSTEM, "data/img/icons8-folder-48.png");
		map.put(IconKey.FERMENTABLE_GRAIN, "data/img/icons8-barley-48.png");
		map.put(IconKey.FERMENTABLE_SUGAR, "data/img/icons8-spoon-of-sugar-48.png");
		map.put(IconKey.FERMENTABLE_DRY_EXTRACT, "data/img/icons8-flour-48.png");
		map.put(IconKey.FERMENTABLE_ADJUNCT, "data/img/icons8-grains-of-rice-48.png");
		map.put(IconKey.FERMENTABLE_JUICE, "data/img/icons8-orange-juice-48.png");
		map.put(IconKey.FERMENTABLE_HONEY, "data/img/icons8-honey-48.png");
		map.put(IconKey.FERMENTABLE_LIQUID_EXTRACT, "data/img/icons8-tin-can-48.png");
		map.put(IconKey.MISC_SPICE, "data/img/icons8-spice-48.png");
		map.put(IconKey.MISC_WATER_AGENT, "data/img/icons8-acid-flask-48.png");
		map.put(IconKey.MISC_FINING, "data/img/icons8-mana-48.png");
		map.put(IconKey.MISC_HERB, "data/img/icons8-natural-food-48.png");
		map.put(IconKey.MISC_FLAVOUR, "data/img/icons8-test-tube-48.png");
		map.put(IconKey.MISC_OTHER, "data/img/icons8-sugar-cubes-48.png");
		map.put(IconKey.YEAST_CULTURE, "data/img/icons8-experiment-48.png");
		return map;
	}

	private static void logMissing(IconKey key, String detail)
	{
		if (Boolean.TRUE.equals(MISSING_LOGGED.get(key)))
		{
			return;
		}
		MISSING_LOGGED.put(key, true);
		Brewday.getInstance().getLog().log("SwingIcons: " + detail);
	}

	private static int clampRgb(int value)
	{
		return Math.max(0, Math.min(255, value));
	}

	private static ImageIcon tintIcon(ImageIcon base, Color tint)
	{
		int w = base.getIconWidth();
		int h = base.getIconHeight();
		if (w <= 0 || h <= 0)
		{
			return base;
		}
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.drawImage(base.getImage(), 0, 0, null);
		g.setComposite(AlphaComposite.SrcAtop);
		g.setColor(tint);
		g.fillRect(0, 0, w, h);
		g.dispose();
		return new ImageIcon(img);
	}
}
