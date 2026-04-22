package mclachlan.brewday.ui.swing.app;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import mclachlan.brewday.Brewday;

public class SwingIcons
{
	public static final int NAV_ICON_SIZE = 24;
	public static final int TOOLBAR_ICON_SIZE = 20;
	public static final int WINDOW_ICON_16 = 16;
	public static final int WINDOW_ICON_32 = 32;
	public static final int WINDOW_ICON_64 = 64;

	public enum IconKey
	{
		BREWDAY,
		BEER,
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
		EXPORT_CSV
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

	public static ImageIcon toolbarIcon(IconKey key)
	{
		return icon(key, TOOLBAR_ICON_SIZE);
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

	public static Icon emptyIcon()
	{
		return EMPTY_ICON;
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
			case SETTINGS, BREWING_SETTINGS, BREWING_SETTINGS_GENERAL, BACKEND_SETTINGS, UI_SETTINGS -> IconKey.SETTINGS;
			case BREWING_SETTINGS_MASH -> IconKey.MASH;
			case BACKEND_SETTINGS_LOCAL_FILESYSTEM -> IconKey.DATABASE;
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

		return null;
	}

	private static Map<IconKey, String> buildPaths()
	{
		Map<IconKey, String> map = new EnumMap<>(IconKey.class);
		map.put(IconKey.BREWDAY, "data/img/brewday.png");
		map.put(IconKey.BEER, "data/img/icons8-beer-glass-48.png");
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
		map.put(IconKey.EXPORT_CSV, "data/img/icons8-export-csv-48.png");
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
}
