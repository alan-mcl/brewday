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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Aggregated brew history for recommendation scoring.
 */
public final class BrewingHistoryIndex
{
	private static final int RECENT_BATCH_COUNT = 6;
	private static final int RECENT_MONTHS = 6;

	private final Map<String, RecipeHistory> recipeHistory;
	private final Map<String, StyleHistory> styleHistory;
	private final List<RecentBatchEntry> recentBatches;
	private final Set<String> brewedStyleIds;
	private final Set<String> recentRecipeNames;
	private final StyleCharacteristics recentStyleCentroid;
	private final Map<String, Integer> ingredientUseInRecentBatches;

	public BrewingHistoryIndex(RecommendationContext context)
	{
		this.recipeHistory = new HashMap<>();
		this.styleHistory = new HashMap<>();
		this.brewedStyleIds = new HashSet<>();
		this.ingredientUseInRecentBatches = new HashMap<>();

		List<BatchEntry> all = new ArrayList<>();
		for (Batch batch : context.getBatches().values())
		{
			if (batch.getRecipe() == null || batch.getDate() == null)
			{
				continue;
			}
			all.add(new BatchEntry(batch.getRecipe(), batch.getDate(), batch.getName()));
		}
		all.sort(Comparator.comparing(BatchEntry::date).reversed());

		LocalDate cutoff = context.getAsOf().minusMonths(RECENT_MONTHS);
		List<RecentBatchEntry> recent = new ArrayList<>();
		Set<String> recentRecipes = new HashSet<>();
		for (int i = 0; i < all.size(); i++)
		{
			BatchEntry entry = all.get(i);
			boolean inWindow = i < RECENT_BATCH_COUNT || !entry.date().isBefore(cutoff);
			if (inWindow)
			{
				Recipe recipe = context.getRecipes().get(entry.recipeName());
				String styleId = RecipeRecommendationUtils.getPrimaryStyleId(recipe);
				recent.add(new RecentBatchEntry(entry.recipeName(), styleId, entry.date()));
				recentRecipes.add(entry.recipeName());
				if (recipe != null)
				{
					for (IngredientAddition ia : recipe.getIngredientsBillOfMaterials())
					{
						String key = ia.getInventoryId();
						ingredientUseInRecentBatches.merge(key, 1, Integer::sum);
					}
				}
			}

			RecipeHistory rh = recipeHistory.computeIfAbsent(
				entry.recipeName(), k -> new RecipeHistory());
			rh.brewCount++;
			if (rh.lastBrewDate == null || entry.date().isAfter(rh.lastBrewDate))
			{
				rh.lastBrewDate = entry.date();
			}

			Recipe recipe = context.getRecipes().get(entry.recipeName());
			String styleId = RecipeRecommendationUtils.getPrimaryStyleId(recipe);
			if (styleId != null)
			{
				brewedStyleIds.add(styleId);
				StyleHistory sh = styleHistory.computeIfAbsent(styleId, k -> new StyleHistory());
				sh.brewCount++;
				if (sh.lastBrewDate == null || entry.date().isAfter(sh.lastBrewDate))
				{
					sh.lastBrewDate = entry.date();
				}
			}
		}

		this.recentBatches = List.copyOf(recent);
		this.recentRecipeNames = Collections.unmodifiableSet(recentRecipes);
		this.recentStyleCentroid = computeRecentCentroid(context, recent);
	}

	public boolean wasBrewedVeryRecently(String recipeName)
	{
		int limit = Math.min(2, recentBatches.size());
		for (int i = 0; i < limit; i++)
		{
			if (recipeName.equals(recentBatches.get(i).recipeName()))
			{
				return true;
			}
		}
		return false;
	}

	public RecipeHistory getRecipeHistory(String recipeName)
	{
		return recipeHistory.get(recipeName);
	}

	public StyleHistory getStyleHistory(String styleId)
	{
		return styleHistory.get(styleId);
	}

	public List<RecentBatchEntry> getRecentBatches()
	{
		return recentBatches;
	}

	public Set<String> getBrewedStyleIds()
	{
		return brewedStyleIds;
	}

	public Set<String> getRecentRecipeNames()
	{
		return recentRecipeNames;
	}

	public StyleCharacteristics getRecentStyleCentroid()
	{
		return recentStyleCentroid;
	}

	public long monthsSinceLastBrew(String recipeName, LocalDate asOf)
	{
		RecipeHistory rh = recipeHistory.get(recipeName);
		if (rh == null || rh.lastBrewDate == null)
		{
			return -1L;
		}
		return ChronoUnit.MONTHS.between(rh.lastBrewDate, asOf);
	}

	public long monthsSinceLastStyleBrew(String styleId, LocalDate asOf)
	{
		StyleHistory sh = styleHistory.get(styleId);
		if (sh == null || sh.lastBrewDate == null)
		{
			return -1L;
		}
		return ChronoUnit.MONTHS.between(sh.lastBrewDate, asOf);
	}

	public int countRecentStyleBrews(String styleId)
	{
		int count = 0;
		for (RecentBatchEntry entry : recentBatches)
		{
			if (styleId != null && styleId.equals(entry.styleId()))
			{
				count++;
			}
		}
		return count;
	}

	public boolean wasBrewedRecently(String recipeName)
	{
		return recentRecipeNames.contains(recipeName);
	}

	public int ingredientRecentUseCount(String inventoryId)
	{
		return ingredientUseInRecentBatches.getOrDefault(inventoryId, 0);
	}

	public boolean hasBatchHistory()
	{
		return !recipeHistory.isEmpty();
	}

	private StyleCharacteristics computeRecentCentroid(
		RecommendationContext context,
		List<RecentBatchEntry> recent)
	{
		if (recent.isEmpty())
		{
			return StyleCharacteristics.neutral();
		}

		int hopHoppy = 0;
		int hopMalt = 0;
		int colourPale = 0;
		int colourDark = 0;
		int strengthSession = 0;
		int strengthStrong = 0;
		int fermentationExpressive = 0;

		for (RecentBatchEntry entry : recent)
		{
			if (entry.styleId() == null)
			{
				continue;
			}
			Style style = context.getStyles().get(entry.styleId());
			StyleCharacteristics chars = StyleCharacteristics.fromStyle(style);
			switch (chars.getHopProfile())
			{
				case HOPPY -> hopHoppy++;
				case MALT_FORWARD -> hopMalt++;
				default -> { }
			}
			switch (chars.getColourProfile())
			{
				case PALE -> colourPale++;
				case DARK -> colourDark++;
				default -> { }
			}
			switch (chars.getStrengthProfile())
			{
				case SESSION -> strengthSession++;
				case STRONG -> strengthStrong++;
				default -> { }
			}
			if (chars.getFermentationProfile() == StyleCharacteristics.FermentationProfile.EXPRESSIVE)
			{
				fermentationExpressive++;
			}
		}

		StyleCharacteristics.HopProfile hop = hopHoppy >= hopMalt
			? (hopHoppy > 0 ? StyleCharacteristics.HopProfile.HOPPY : StyleCharacteristics.HopProfile.BALANCED)
			: StyleCharacteristics.HopProfile.MALT_FORWARD;
		StyleCharacteristics.ColourProfile colour = colourDark >= colourPale
			? (colourDark > 0 ? StyleCharacteristics.ColourProfile.DARK : StyleCharacteristics.ColourProfile.AMBER)
			: StyleCharacteristics.ColourProfile.PALE;
		StyleCharacteristics.StrengthProfile strength = strengthStrong >= strengthSession
			? (strengthStrong > 0 ? StyleCharacteristics.StrengthProfile.STRONG : StyleCharacteristics.StrengthProfile.STANDARD)
			: StyleCharacteristics.StrengthProfile.SESSION;
		StyleCharacteristics.FermentationProfile fermentation = fermentationExpressive >= recent.size() / 2
			? StyleCharacteristics.FermentationProfile.EXPRESSIVE
			: StyleCharacteristics.FermentationProfile.CLEAN;

		return StyleCharacteristics.ofProfiles(
			hop,
			colour,
			strength,
			fermentation,
			false,
			0.5D,
			null);
	}

	public static final class RecipeHistory
	{
		private LocalDate lastBrewDate;
		private int brewCount;

		public LocalDate getLastBrewDate()
		{
			return lastBrewDate;
		}

		public int getBrewCount()
		{
			return brewCount;
		}
	}

	public static final class StyleHistory
	{
		private LocalDate lastBrewDate;
		private int brewCount;

		public LocalDate getLastBrewDate()
		{
			return lastBrewDate;
		}

		public int getBrewCount()
		{
			return brewCount;
		}
	}

	public record RecentBatchEntry(String recipeName, String styleId, LocalDate date)
	{
	}

	private record BatchEntry(String recipeName, LocalDate date, String batchId)
	{
	}
}
