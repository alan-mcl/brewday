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
 * Test class to verify the Settings node functionality in SwingUi.
 */
public class TestSwingUiSettingsNode
{
	public static void main(String[] args)
	{
		System.out.println("Testing SwingUi Settings node");
		
		// Create the UI on the EDT
		SwingUtilities.invokeLater(() -> {
			try
			{
				// Set look and feel
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				// Create and show the UI
				SwingUi ui = new SwingUi();
				ui.setVisible(true);
				
				System.out.println("SwingUi created with Settings node");
				System.out.println("Please test the following:");
				System.out.println("1. Click on the Settings node in the tree");
				System.out.println("2. Verify that a panel with buttons appears for Brewing Settings and UI Settings");
				System.out.println("3. Click on Brewing Settings and verify that it shows buttons for General, Mash, and IBU");
				System.out.println("4. Click on each child node and verify that the appropriate panel is displayed");
				System.out.println("5. If the feature toggle is enabled, verify that Backend Settings is also shown");
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
