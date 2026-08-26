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

package mclachlan.brewday.recommend;

import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Helpers for recipe/style resolution without running the process model.
 */
public final class RecipeRecommendationUtils
{
	private RecipeRecommendationUtils()
	{
	}

	public static String getPrimaryStyleId(Recipe recipe)
	{
		if (recipe == null || recipe.getSteps() == null)
		{
			return null;
		}
		for (ProcessStep step : recipe.getSteps())
		{
			if (step instanceof PackageStep packageStep)
			{
				String styleId = packageStep.getStyleId();
				if (styleId != null && !styleId.isBlank())
				{
					return styleId;
				}
			}
		}
		return null;
	}

	public static Style resolveStyle(Recipe recipe, java.util.Map<String, Style> styles)
	{
		String styleId = getPrimaryStyleId(recipe);
		if (styleId == null || styles == null)
		{
			return null;
		}
		return styles.get(styleId);
	}

	public static String styleDisplay(Recipe recipe, java.util.Map<String, Style> styles)
	{
		Style style = resolveStyle(recipe, styles);
		if (style == null)
		{
			return "";
		}
		if (style.getDisplayName() != null && !style.getDisplayName().isBlank())
		{
			return style.getDisplayName();
		}
		if (style.getStyleGuideName() != null)
		{
			return style.getStyleGuideName();
		}
		return style.getName();
	}

	public static String styleFamilyKey(Style style)
	{
		if (style == null)
		{
			return "unknown";
		}
		String category = style.getCategoryNumber();
		if (category != null && !category.isBlank())
		{
			return "cat:" + category;
		}
		if (style.getCategory() != null && !style.getCategory().isBlank())
		{
			return "catname:" + style.getCategory().toLowerCase();
		}
		return "style:" + style.getName();
	}

	public static String monthsSinceLabel(long months)
	{
		return RecommendationUiSupport.monthsSinceLabel(months);
	}
}
