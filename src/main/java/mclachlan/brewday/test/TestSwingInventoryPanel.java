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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.SwingUi;

import javax.swing.*;

/**
 * Test class to verify the SwingInventoryPanel functionality.
 */
public class TestSwingInventoryPanel
{
	public static void main(String[] args)
	{
		System.out.println("Testing SwingInventoryPanel");
		
		// Create the UI on the EDT
		SwingUtilities.invokeLater(() -> {
			try
			{
				// Set look and feel
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				// Create and show the UI
				SwingUi ui = new SwingUi();
				ui.setVisible(true);
				
				// Select the Inventory node in the tree
				System.out.println("Please test the following:");
				System.out.println("1. Click on the Inventory node in the tree");
				System.out.println("2. Verify that the inventory panel is displayed with a table of inventory items");
				System.out.println("3. Test adding different types of inventory items using the toolbar buttons");
				System.out.println("4. Test editing an inventory item by selecting it and clicking the edit button");
				System.out.println("5. Test deleting an inventory item");
				System.out.println("6. Test saving changes using the save button");
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
