
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

package mclachlan.brewday.util;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Fixes the styles data element, needed when we changed the name field to
 * include the style guide name
 */
public class FixStyles
{
	public static void main(String[] args)
	{
		Database db = Database.getInstance();
		db.loadAll();

		for (Style s : db.getStyles().values())
		{
			String oldName = s.getName();

			String newName = s.getStyleNumber()+"/"+s.getStyleGuideName()+"/"+s.getStyleGuide();

			s.setDisplayName(oldName);
			s.setName(newName);

			System.out.println(oldName+" -> "+newName);

			// update recipes
			for (Recipe r : db.getRecipes().values())
			{
				for (ProcessStep ps : r.getSteps())
				{
					if (ps instanceof PackageStep &&
						((PackageStep)ps).getStyleId() != null &&
						((PackageStep)ps).getStyleId().equals(oldName))
					{
						((PackageStep)ps).setStyleId(newName);

						System.out.println(" - "+r.getName());
					}
				}
			}

			// update process templates
			for (Recipe r : db.getProcessTemplates().values())
			{
				for (ProcessStep ps : r.getSteps())
				{
					if (ps instanceof PackageStep &&
						((PackageStep)ps).getStyleId() != null &&
						((PackageStep)ps).getStyleId().equals(oldName))
					{
						((PackageStep)ps).setStyleId(newName);

						System.out.println(" - "+r.getName());
					}
				}
			}
		}

		db.saveAll();
	}
}
