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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mclachlan.brewday.Settings;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Immutable snapshot of data used for recommendations.
 */
public final class RecommendationContext
{
	public static final String RECENT_SHOWN_SETTING = Settings.WHAT_SHOULD_I_BREW_RECENT;

	private final Map<String, Recipe> recipes;
	private final Map<String, Batch> batches;
	private final Map<String, InventoryLineItem> inventory;
	private final Map<String, Style> styles;
	private final LocalDate asOf;
	private final Set<String> recentlyShownRecipeNames;
	private final RecommendationSettings recommendationSettings;

	private RecommendationContext(
		Map<String, Recipe> recipes,
		Map<String, Batch> batches,
		Map<String, InventoryLineItem> inventory,
		Map<String, Style> styles,
		LocalDate asOf,
		Set<String> recentlyShownRecipeNames,
		RecommendationSettings recommendationSettings)
	{
		this.recipes = recipes;
		this.batches = batches;
		this.inventory = inventory;
		this.styles = styles;
		this.asOf = asOf;
		this.recentlyShownRecipeNames = recentlyShownRecipeNames;
		this.recommendationSettings = recommendationSettings == null
			? RecommendationSettings.defaults()
			: recommendationSettings;
	}

	public static RecommendationContext fromDatabase(LocalDate asOf)
	{
		Database db = Database.getInstance();
		Set<String> recent = parseRecentShown(
			db.getSettings().get(RECENT_SHOWN_SETTING));
		return new RecommendationContext(
			db.getRecipes(),
			db.getBatches(),
			db.getInventory(),
			db.getStyles(),
			asOf,
			recent,
			RecommendationSettings.from(db.getSettings()));
	}

	public static RecommendationContext forTest(
		Map<String, Recipe> recipes,
		Map<String, Batch> batches,
		Map<String, InventoryLineItem> inventory,
		Map<String, Style> styles,
		LocalDate asOf,
		Set<String> recentlyShownRecipeNames)
	{
		return forTest(
			recipes,
			batches,
			inventory,
			styles,
			asOf,
			recentlyShownRecipeNames,
			RecommendationSettings.defaults());
	}

	public static RecommendationContext forTest(
		Map<String, Recipe> recipes,
		Map<String, Batch> batches,
		Map<String, InventoryLineItem> inventory,
		Map<String, Style> styles,
		LocalDate asOf,
		Set<String> recentlyShownRecipeNames,
		RecommendationSettings recommendationSettings)
	{
		return new RecommendationContext(
			recipes,
			batches,
			inventory,
			styles,
			asOf,
			recentlyShownRecipeNames == null ? Set.of() : recentlyShownRecipeNames,
			recommendationSettings);
	}

	public static String formatRecentShown(List<String> recipeNames)
	{
		if (recipeNames == null || recipeNames.isEmpty())
		{
			return "";
		}
		return String.join(",", recipeNames);
	}

	public static Set<String> parseRecentShown(String raw)
	{
		if (raw == null || raw.isBlank())
		{
			return Set.of();
		}
		Set<String> result = new LinkedHashSet<>();
		for (String part : raw.split(","))
		{
			String trimmed = part.trim();
			if (!trimmed.isEmpty())
			{
				result.add(trimmed);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	public Map<String, Recipe> getRecipes()
	{
		return recipes;
	}

	public Map<String, Batch> getBatches()
	{
		return batches;
	}

	public Map<String, InventoryLineItem> getInventory()
	{
		return inventory;
	}

	public Map<String, Style> getStyles()
	{
		return styles;
	}

	public LocalDate getAsOf()
	{
		return asOf;
	}

	public Set<String> getRecentlyShownRecipeNames()
	{
		return recentlyShownRecipeNames;
	}

	public RecommendationSettings getRecommendationSettings()
	{
		return recommendationSettings;
	}

	public List<Recipe> getRecipeList()
	{
		return new ArrayList<>(recipes.values());
	}

	public void persistRecentShown(List<String> recipeNames)
	{
		Database.getInstance().getSettings().set(
			RECENT_SHOWN_SETTING,
			formatRecentShown(recipeNames));
		Database.getInstance().saveSettings();
	}
}
