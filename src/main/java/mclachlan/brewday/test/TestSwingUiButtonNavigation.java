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
import mclachlan.brewday.ui.swing.SwingUi;

import javax.swing.*;

/**
 * Test class to verify the button navigation functionality in SwingUi.
 */
public class TestSwingUiButtonNavigation
{
	public static void main(String[] args)
	{
		System.out.println("Testing SwingUi button navigation");
		
		// Create the UI on the EDT
		SwingUtilities.invokeLater(() -> {
			try
			{
				// Set look and feel
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				// Create and show the UI
				SwingUi ui = new SwingUi();
				ui.setVisible(true);
				
				System.out.println("SwingUi created with button navigation");
				System.out.println("Please test the following:");
				System.out.println("1. Click on parent nodes in the tree (Brewing, Reference Database, etc.)");
				System.out.println("2. Verify that a panel with large buttons appears for each child node");
				System.out.println("3. Click on these buttons and verify that navigation works correctly");
				System.out.println("4. Verify that icons are displayed properly on the buttons");
				System.out.println("Close the window when done testing");
				
			}
catch (Exception e)
{
				e.printStackTrace();
				Brewday.getInstance().getLog().log(e);
			}
		});
	}
}
