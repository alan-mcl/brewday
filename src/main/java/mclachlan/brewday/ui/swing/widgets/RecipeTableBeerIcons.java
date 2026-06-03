/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.SwingIcons;

/**
 * Resolves SRM-tinted beer-glass table icons for a recipe (packaged beer outputs).
 */
public class RecipeTableBeerIcons
{
	private RecipeTableBeerIcons()
	{
	}

	public static List<Icon> iconsForRecipe(Recipe recipe, int maxBeers)
	{
		if (recipe == null || maxBeers <= 0)
		{
			return Collections.emptyList();
		}
		try
		{
			Recipe scratch = new Recipe(recipe);
			scratch.run();
			List<Volume> beers = scratch.getBeers();
			if (beers == null || beers.isEmpty())
			{
				return Collections.emptyList();
			}
			List<Icon> icons = new ArrayList<>(Math.min(maxBeers, beers.size()));
			for (int i = 0; i < beers.size() && icons.size() < maxBeers; i++)
			{
				Volume beer = beers.get(i);
				double srm = 0;
				ColourUnit colour = beer.getColour();
				if (colour != null)
				{
					srm = colour.get(Quantity.Unit.SRM);
				}
				icons.add(SwingIcons.tintedTableBeerIcon(srm));
			}
			return icons;
		}
		catch (Exception e)
		{
			return Collections.emptyList();
		}
	}
}
