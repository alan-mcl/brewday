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

import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Precomputed signals for one recipe used across recommendation groups.
 */
public final class RecipeSignals
{
	private final Recipe recipe;
	private final String styleId;
	private final Style style;
	private final String styleDisplay;
	private final String styleFamilyKey;
	private final StyleCharacteristics characteristics;
	private final InventoryMatch inventoryMatch;
	private final BrewingHistoryIndex.RecipeHistory recipeHistory;
	private final long monthsSinceLastBrew;
	private final boolean neverBrewed;
	private final boolean brewedRecently;
	private final boolean styleNeverBrewed;
	private final double seasonalBoost;

	public RecipeSignals(
		Recipe recipe,
		String styleId,
		Style style,
		String styleDisplay,
		String styleFamilyKey,
		StyleCharacteristics characteristics,
		InventoryMatch inventoryMatch,
		BrewingHistoryIndex.RecipeHistory recipeHistory,
		long monthsSinceLastBrew,
		boolean neverBrewed,
		boolean brewedRecently,
		boolean styleNeverBrewed,
		double seasonalBoost)
	{
		this.recipe = recipe;
		this.styleId = styleId;
		this.style = style;
		this.styleDisplay = styleDisplay;
		this.styleFamilyKey = styleFamilyKey;
		this.characteristics = characteristics;
		this.inventoryMatch = inventoryMatch;
		this.recipeHistory = recipeHistory;
		this.monthsSinceLastBrew = monthsSinceLastBrew;
		this.neverBrewed = neverBrewed;
		this.brewedRecently = brewedRecently;
		this.styleNeverBrewed = styleNeverBrewed;
		this.seasonalBoost = seasonalBoost;
	}

	public Recipe getRecipe()
	{
		return recipe;
	}

	public String getRecipeName()
	{
		return recipe.getName();
	}

	public String getStyleId()
	{
		return styleId;
	}

	public Style getStyle()
	{
		return style;
	}

	public String getStyleDisplay()
	{
		return styleDisplay;
	}

	public String getStyleFamilyKey()
	{
		return styleFamilyKey;
	}

	public StyleCharacteristics getCharacteristics()
	{
		return characteristics;
	}

	public InventoryMatch getInventoryMatch()
	{
		return inventoryMatch;
	}

	public BrewingHistoryIndex.RecipeHistory getRecipeHistory()
	{
		return recipeHistory;
	}

	public long getMonthsSinceLastBrew()
	{
		return monthsSinceLastBrew;
	}

	public boolean isNeverBrewed()
	{
		return neverBrewed;
	}

	public boolean isBrewedRecently()
	{
		return brewedRecently;
	}

	public boolean isStyleNeverBrewed()
	{
		return styleNeverBrewed;
	}

	public double getSeasonalBoost()
	{
		return seasonalBoost;
	}

	public int getBrewCount()
	{
		return recipeHistory == null ? 0 : recipeHistory.getBrewCount();
	}
}
