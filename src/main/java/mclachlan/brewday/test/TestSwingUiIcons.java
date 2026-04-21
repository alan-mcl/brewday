/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.test;

import mclachlan.brewday.Brewday;
import mclachlan.brewday.ui.swing.SwingIcons;
import mclachlan.brewday.ui.swing.SwingUi;

import javax.swing.*;

/**
 * Test class to verify the tree icons in SwingUi.
 */
public class TestSwingUiIcons
{
	public static void main(String[] args)
	{
		System.out.println("Testing SwingUi tree icons");
		
		// Create the UI on the EDT
		SwingUtilities.invokeLater(() -> {
			try
			{
				// Set look and feel
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				// Initialize icons
				SwingIcons.init();
				
				// Verify icons are loaded
				verifyIcons();
				
				// Create and show the UI
				SwingUi ui = new SwingUi();
				ui.setVisible(true);
				
				System.out.println("SwingUi created with tree icons");
				System.out.println("Please manually verify that icons are displayed in the navigation tree");
				System.out.println("Close the window when done testing");
				
			}
catch (Exception e)
{
				e.printStackTrace();
				Brewday.getInstance().getLog().log(e);
			}
		});
	}
	
	/**
	 * Verify that icons are loaded correctly.
	 */
	private static void verifyIcons()
	{
		System.out.println("Verifying icons are loaded:");
		
		verifyIcon("brewdayIcon", SwingIcons.brewdayIcon);
		verifyIcon("beerIcon", SwingIcons.beerIcon);
		verifyIcon("recipeIcon", SwingIcons.recipeIcon);
		verifyIcon("inventoryIcon", SwingIcons.inventoryIcon);
		verifyIcon("databaseIcon", SwingIcons.databaseIcon);
		verifyIcon("waterIcon", SwingIcons.waterIcon);
		verifyIcon("hopsIcon", SwingIcons.hopsIcon);
		verifyIcon("yeastIcon", SwingIcons.yeastIcon);
		verifyIcon("miscIconGeneric", SwingIcons.miscIconGeneric);
		verifyIcon("stylesIcon", SwingIcons.stylesIcon);
		verifyIcon("toolsIcon", SwingIcons.toolsIcon);
		verifyIcon("settingsIcon", SwingIcons.settingsIcon);
		verifyIcon("helpIcon", SwingIcons.helpIcon);
		
		System.out.println("Icon verification complete");
	}
	
	/**
	 * Verify that an icon is loaded.
	 */
	private static void verifyIcon(String name, ImageIcon icon)
	{
		if (icon == null)
		{
			System.out.println("  " + name + ": NOT LOADED");
		}
else
{
			System.out.println("  " + name + ": LOADED (" + icon.getIconWidth() + "x" + icon.getIconHeight() + ")");
		}
	}
}
