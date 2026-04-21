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

package mclachlan.brewday.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import mclachlan.brewday.BrewdayException;

/**
 * Manages icons for the Swing UI, similar to the Icons class in the JavaFX UI.
 */
public class SwingIcons
{
	/** Icon size for standard icons */
	public static final int ICON_SIZE = 32;
	
	/** Icon size for navigation tree icons */
	public static final int NAV_ICON_SIZE = 24;
	
	// Main category icons
	public static ImageIcon brewdayIcon = createIcon("data/img/brewday.png");
	public static ImageIcon beerIcon = createIcon("data/img/icons8-beer-glass-48.png");
	public static ImageIcon inventoryIcon = createIcon("data/img/icons8-trolley-48.png");
	public static ImageIcon databaseIcon = createIcon("data/img/icons8-database-48.png");
	public static ImageIcon toolsIcon = createIcon("data/img/icons8-full-tool-storage-box-48.png");
	public static ImageIcon settingsIcon = createIcon("data/img/icons8-settings-48.png");
	public static ImageIcon helpIcon = createIcon("data/img/icons8-help-48.png");
	
	// Brewing subcategory icons
	public static ImageIcon recipeIcon = createIcon("data/img/icons8-beer-recipe-48.png");
	public static ImageIcon processTemplateIcon = createIcon("data/img/icons8-flow-48.png");
	public static ImageIcon equipmentIcon = createIcon("data/img/icons8-brewsystem-48.png");
	
	// Reference data subcategory icons
	public static ImageIcon waterIcon = createIcon("data/img/icons8-water-48.png");
	public static ImageIcon waterParametersIcon = createIcon("data/img/water_parameters.png");
	public static ImageIcon fermentableIconGeneric = createIcon("data/img/icons8-carbohydrates-48.png");
	public static ImageIcon hopsIcon = createIcon("data/img/icons8-hops-48.png");
	public static ImageIcon yeastIcon = createIcon("data/img/icons8-experiment-48.png");
	public static ImageIcon miscIconGeneric = createIcon("data/img/icons8-sugar-cubes-48.png");
	public static ImageIcon stylesIcon = createIcon("data/img/icons8-test-passed-48.png");
	
	// Tools subcategory icons
	public static ImageIcon importIcon = createIcon("data/img/icons8-import-48.png");
	public static ImageIcon waterBuilderIcon = createIcon("data/img/water_builder.png");
	
	// Other icons
	public static ImageIcon documentIcon = createIcon("data/img/icons8-document-48.png");
	public static ImageIcon infoIcon = createIcon("data/img/icons8-information-48.png");
	public static ImageIcon deleteIcon = createIcon("data/img/icons8-delete-48.png");
	public static ImageIcon gitIcon = createIcon("data/img/icons8-git-48.png");
	public static ImageIcon mashIcon = createIcon("data/img/icons8-mash-in.png");
	public static ImageIcon saveIcon = createIcon("data/img/icons8-save-48.png");
	public static ImageIcon undoIcon = createIcon("data/img/icons8-undo-48.png");
	public static ImageIcon addIcon = createIcon("data/img/icons8-add-new-48.png");
	
	/**
	 * Initialize the icons.
	 */
	public static void init()
	{
		// This method is called to ensure all static fields are initialized
	}
	
	/**
	 * Create an ImageIcon from a file path.
	 */
	private static ImageIcon createIcon(String path)
	{
		try
		{
			File file = new File(path);
			if (file.exists())
			{
				return new ImageIcon(file.getAbsolutePath());
			}
else
{
				System.err.println("Icon file not found: " + path);
				return null;
			}
		}
catch (Exception e)
{
			throw new BrewdayException("Failed to load icon: " + path, e);
		}
	}
	
	/**
	 * Resize an icon to the specified size.
	 */
	public static ImageIcon resizeIcon(ImageIcon icon, int width, int height)
	{
		if (icon == null)
		{
			return null;
		}
		
		Image img = icon.getImage();
		Image resizedImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		return new ImageIcon(resizedImg);
	}
}
