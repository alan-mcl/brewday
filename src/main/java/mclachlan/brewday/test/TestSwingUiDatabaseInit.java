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
 * Test class to verify that the database is properly initialized in SwingUi.
 */
public class TestSwingUiDatabaseInit
{
	public static void main(String[] args)
	{
		System.out.println("Testing SwingUi database initialization");
		
		// Create the UI on the EDT
		SwingUtilities.invokeLater(() -> {
			try
			{
				// Set look and feel
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				// Create the UI
				SwingUi ui = new SwingUi();
				
				// Verify database initialization
				Database db = ui.getDatabase();
				
				// Check if database is loaded by checking if settings are available
				if (db.getSettings() != null)
				{
					System.out.println("Database settings loaded successfully");
				}
else
{
					System.out.println("ERROR: Database settings not loaded");
				}
				
				// Check if strings are loaded
				if (db.getStrings("ui") != null && !db.getStrings("ui").isEmpty())
				{
					System.out.println("UI strings loaded successfully");
				}
else
{
					System.out.println("ERROR: UI strings not loaded");
				}
				
				// Check if reference data is loaded
				if (db.getHops() != null && db.getFermentables() != null && db.getYeasts() != null)
				{
					System.out.println("Reference data loaded successfully");
				}
else
{
					System.out.println("ERROR: Reference data not loaded");
				}
				
				System.out.println("Database initialization test complete");
				
				// Exit after test
				System.exit(0);
			}
catch (Exception e)
{
				e.printStackTrace();
				Brewday.getInstance().getLog().log(e);
				System.exit(1);
			}
		});
	}
}
