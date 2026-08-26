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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Generates grouped brew recommendations from recipes, inventory, and batch history.
 */
public class BrewRecommendationEngine
{
	private static final int DUE_REPEAT_MIN_BREWS = 2;
	private static final double STRETCH_MAX_CONTRAST = 3D;

	public RecommendationResult recommend(RecommendationContext context)
	{
		if (context.getRecipes().isEmpty())
		{
			return new RecommendationResult(List.of(), List.of());
		}

		BrewingHistoryIndex history = new BrewingHistoryIndex(context);
		List<RecipeSignals> signals = buildSignals(context, history);
		RecommendationSettings settings = context.getRecommendationSettings();
		int minGroupSize = settings.getMinGroupSize();
		int maxPerGroup = settings.getMaxPerGroup();

		List<RecommendationGroup> groups = new ArrayList<>();
		addGroupIfPresent(groups, bestInventoryMatches(signals, context, settings, minGroupSize, maxPerGroup));
		if (history.hasBatchHistory())
		{
			addGroupIfPresent(groups, dueForRepeat(signals, context, settings, minGroupSize, maxPerGroup));
			addGroupIfPresent(groups, stylesDueForRevisit(signals, context, history, settings, minGroupSize, maxPerGroup));
			addGroupIfPresent(groups, somethingDifferent(signals, context, history, settings, minGroupSize, maxPerGroup));
			addGroupIfPresent(groups, forgottenRecipes(signals, context, settings, minGroupSize, maxPerGroup));
		}
		addGroupIfPresent(groups, neverBrewed(signals, context, history, settings, minGroupSize, maxPerGroup));
		addGroupIfPresent(groups, useItUp(signals, context, history, settings, minGroupSize, maxPerGroup));
		addGroupIfPresent(groups, oneSmallPurchase(signals, context, settings, minGroupSize, maxPerGroup));
		if (history.hasBatchHistory())
		{
			addGroupIfPresent(groups, stretchExperiment(signals, context, history, settings, minGroupSize, maxPerGroup));
		}

		List<String> shown = collectShownRecipeNames(groups);
		return new RecommendationResult(groups, shown);
	}

	private List<RecipeSignals> buildSignals(RecommendationContext context, BrewingHistoryIndex history)
	{
		List<RecipeSignals> result = new ArrayList<>();
		double seasonalTarget = SeasonalLightnessSupport.targetLightness(
			context.getAsOf(),
			context.getRecommendationSettings().getHemisphere(),
			context.getRecommendationSettings().getSeasonalLeadMonths());

		for (Recipe recipe : context.getRecipeList())
		{
			String styleId = RecipeRecommendationUtils.getPrimaryStyleId(recipe);
			Style style = styleId == null ? null : context.getStyles().get(styleId);
			StyleCharacteristics characteristics = StyleCharacteristics.fromStyle(style);
			InventoryMatch match = InventoryMatchCalculator.calculate(recipe, context.getInventory());

			BrewingHistoryIndex.RecipeHistory rh = history.getRecipeHistory(recipe.getName());
			long monthsSince = history.monthsSinceLastBrew(recipe.getName(), context.getAsOf());
			boolean neverBrewed = rh == null || rh.getBrewCount() == 0;
			boolean styleNever = styleId != null && !history.getBrewedStyleIds().contains(styleId);
			double seasonalBoost = 1D - Math.abs(characteristics.getSeasonalLightness() - seasonalTarget);

			result.add(new RecipeSignals(
				recipe,
				styleId,
				style,
				RecipeRecommendationUtils.styleDisplay(recipe, context.getStyles()),
				RecipeRecommendationUtils.styleFamilyKey(style),
				characteristics,
				match,
				rh,
				monthsSince,
				neverBrewed,
				history.wasBrewedVeryRecently(recipe.getName()),
				styleNever,
				seasonalBoost));
		}
		return result;
	}

	private RecommendationGroup bestInventoryMatches(
		List<RecipeSignals> signals,
		RecommendationContext context,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		for (RecipeSignals s : signals)
		{
			if (s.getInventoryMatch().getMatchPercent() < settings.getBestInventoryMinMatch())
			{
				continue;
			}
			double score = s.getInventoryMatch().getMatchPercent()
				+ (s.getInventoryMatch().isFullyBrewable() ? 10D : 0D)
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
			Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(buildInventoryExplanation(s))
				.tag(RecommendationTag.USES_EXISTING_INVENTORY)
				.build());
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.BEST_INVENTORY_MATCHES, picks);
	}

	private RecommendationGroup dueForRepeat(
		List<RecipeSignals> signals,
		RecommendationContext context,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		for (RecipeSignals s : signals)
		{
			if (s.getBrewCount() < DUE_REPEAT_MIN_BREWS || s.isBrewedRecently())
			{
				continue;
			}
			if (s.getMonthsSinceLastBrew() < settings.getDueRepeatGapMonths())
			{
				continue;
			}
			double score = s.getBrewCount() * 5D
				+ s.getMonthsSinceLastBrew()
				+ s.getInventoryMatch().getMatchPercent() * 0.3D
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
		{
			String gap = RecipeRecommendationUtils.monthsSinceLabel(s.getMonthsSinceLastBrew());
			return Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(RecommendationUiSupport.dueForRepeatExplanation(s.getBrewCount()))
				.detailLine(gap)
				.build();
		});
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.DUE_FOR_REPEAT, picks);
	}

	private RecommendationGroup stylesDueForRevisit(
		List<RecipeSignals> signals,
		RecommendationContext context,
		BrewingHistoryIndex history,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		Map<String, Scored<RecipeSignals>> bestByStyle = new LinkedHashMap<>();
		for (RecipeSignals s : signals)
		{
			if (s.getStyleId() == null || s.isStyleNeverBrewed())
			{
				continue;
			}
			long styleGap = history.monthsSinceLastStyleBrew(s.getStyleId(), context.getAsOf());
			if (styleGap < settings.getStyleRevisitGapMonths())
			{
				continue;
			}
			int recentStyleCount = history.countRecentStyleBrews(s.getStyleId());
			if (recentStyleCount >= 2)
			{
				continue;
			}
			double score = styleGap
				+ s.getInventoryMatch().getMatchPercent() * 0.25D
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			Scored<RecipeSignals> existing = bestByStyle.get(s.getStyleId());
			if (existing == null || score > existing.score())
			{
				bestByStyle.put(s.getStyleId(), new Scored<>(s, score));
			}
		}

		List<Scored<RecipeSignals>> scored = new ArrayList<>(bestByStyle.values());
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
		{
			long styleGap = history.monthsSinceLastStyleBrew(s.getStyleId(), context.getAsOf());
			return Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(RecommendationUiSupport.styleRevisitExplanation(s.getStyleDisplay()))
				.detailLine(RecipeRecommendationUtils.monthsSinceLabel(styleGap))
				.build();
		});
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.STYLES_DUE_FOR_REVISIT, picks);
	}

	private RecommendationGroup somethingDifferent(
		List<RecipeSignals> signals,
		RecommendationContext context,
		BrewingHistoryIndex history,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		if (history.getRecentBatches().isEmpty())
		{
			return null;
		}
		StyleCharacteristics recent = history.getRecentStyleCentroid();

		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		for (RecipeSignals s : signals)
		{
			if (s.getStyleId() == null || s.isBrewedRecently())
			{
				continue;
			}
			double contrast = s.getCharacteristics().contrastScore(recent);
			if (contrast < settings.getSomethingDifferentMinContrast())
			{
				continue;
			}
			double score = contrast * 10D
				+ s.getInventoryMatch().getMatchPercent() * 0.2D
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
			Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(s.getCharacteristics().describeContrastFrom(recent))
				.tag(RecommendationTag.DIFFERENT_FROM_RECENT)
				.build());
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.SOMETHING_DIFFERENT, picks);
	}

	private RecommendationGroup neverBrewed(
		List<RecipeSignals> signals,
		RecommendationContext context,
		BrewingHistoryIndex history,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		StyleCharacteristics recent = history.getRecentStyleCentroid();
		for (RecipeSignals s : signals)
		{
			if (!s.isStyleNeverBrewed() || s.getStyleId() == null)
			{
				continue;
			}
			if (s.getInventoryMatch().getMatchPercent() < settings.getNeverBrewedMinMatch())
			{
				continue;
			}
			double score = s.getInventoryMatch().getMatchPercent() * 0.5D
				+ s.getCharacteristics().contrastScore(recent) * 5D
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
			Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(RecommendationUiSupport.neverBrewedExplanation(s.getStyleDisplay()))
				.tag(RecommendationTag.NEVER_BREWED)
				.build());
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.NEVER_BREWED, picks);
	}

	private RecommendationGroup forgottenRecipes(
		List<RecipeSignals> signals,
		RecommendationContext context,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		for (RecipeSignals s : signals)
		{
			if (s.isNeverBrewed() || s.getBrewCount() < 1)
			{
				continue;
			}
			if (s.getBrewCount() >= DUE_REPEAT_MIN_BREWS)
			{
				continue;
			}
			if (s.getMonthsSinceLastBrew() < settings.getForgottenGapMonths())
			{
				continue;
			}
			double score = s.getMonthsSinceLastBrew()
				+ s.getInventoryMatch().getMatchPercent() * 0.3D
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
			Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(RecommendationUiSupport.forgottenRecipeExplanation())
				.detailLine(RecipeRecommendationUtils.monthsSinceLabel(s.getMonthsSinceLastBrew()))
				.build());
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.FORGOTTEN_RECIPES, picks);
	}

	private RecommendationGroup useItUp(
		List<RecipeSignals> signals,
		RecommendationContext context,
		BrewingHistoryIndex history,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		for (RecipeSignals s : signals)
		{
			if (s.getInventoryMatch().getMatchPercent() < settings.getUseItUpMinMatch()
				|| s.getInventoryMatch().hasCriticalMiss())
			{
				continue;
			}
			double useScore = computeUseItUpScore(s, history);
			if (useScore <= 0D)
			{
				continue;
			}
			double score = useScore
				+ s.getInventoryMatch().getMatchPercent() * 0.4D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
		{
			List<String> uses = describeUseItUpIngredients(s, history);
			Recommendation.Builder b = Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(RecommendationUiSupport.useItUpExplanation())
				.tag(RecommendationTag.USE_IT_UP);
			for (String line : uses)
			{
				b.detailLine(line);
			}
			return b.build();
		});
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.USE_IT_UP, picks);
	}

	private RecommendationGroup oneSmallPurchase(
		List<RecipeSignals> signals,
		RecommendationContext context,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		for (RecipeSignals s : signals)
		{
			InventoryMatch match = s.getInventoryMatch();
			int pct = match.getMatchPercent();
			if (pct < settings.getOnePurchaseMinMatch() || pct >= 100 || match.isFullyBrewable())
			{
				continue;
			}
			List<InventoryMatchLine> missing = match.getMissingLines();
			long criticalMissing = missing.stream().filter(InventoryMatchLine::isCriticalMiss).count();
			if (criticalMissing > 0)
			{
				continue;
			}
			if (missing.size() > 2)
			{
				continue;
			}
			if (missing.size() == 2)
			{
				boolean bothMinor = missing.stream().allMatch(line ->
					line.getType() == IngredientAddition.Type.HOPS
						|| line.getType() == IngredientAddition.Type.YEAST
						|| line.getType() == IngredientAddition.Type.MISC);
				if (!bothMinor)
				{
					continue;
				}
			}
			double score = pct
				+ (3 - missing.size()) * 5D
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
		{
			InventoryMatchLine gap = s.getInventoryMatch().getMissingLines().get(0);
			return Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(RecommendationUiSupport.oneSmallPurchaseExplanation(gap.getIngredientName()))
				.tag(RecommendationTag.SMALL_PURCHASE)
				.detailLine(RecommendationUiSupport.missingIngredientDetail(gap.getIngredientName()))
				.build();
		});
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.ONE_SMALL_PURCHASE, picks);
	}

	private RecommendationGroup stretchExperiment(
		List<RecipeSignals> signals,
		RecommendationContext context,
		BrewingHistoryIndex history,
		RecommendationSettings settings,
		int minGroupSize,
		int maxPerGroup)
	{
		StyleCharacteristics recent = history.getRecentStyleCentroid();
		List<Scored<RecipeSignals>> scored = new ArrayList<>();
		for (RecipeSignals s : signals)
		{
			if (s.getStyleId() == null || s.isBrewedRecently())
			{
				continue;
			}
			double contrast = s.getCharacteristics().contrastScore(recent);
			if (contrast < settings.getStretchMinContrast() || contrast >= STRETCH_MAX_CONTRAST)
			{
				continue;
			}
			if (s.getInventoryMatch().getMatchPercent() < settings.getStretchMinMatch())
			{
				continue;
			}
			double score = contrast * 6D
				+ s.getInventoryMatch().getMatchPercent() * 0.25D
				+ (s.getBrewCount() > 0 ? 3D : 0D)
				+ s.getSeasonalBoost() * 2D
				- recentShownPenalty(s, context);
			scored.add(new Scored<>(s, score));
		}
		scored.sort(Comparator.comparingDouble(Scored<RecipeSignals>::score).reversed());

		List<Recommendation> picks = pickDiverse(scored, minGroupSize, maxPerGroup, s ->
			Recommendation.builder(s.getRecipeName())
				.styleDisplay(s.getStyleDisplay())
				.inventoryMatchPercent(s.getInventoryMatch().getMatchPercent())
				.explanation(RecommendationUiSupport.stretchExperimentExplanation())
				.tag(RecommendationTag.STRETCH)
				.build());
		return picks.isEmpty() ? null : new RecommendationGroup(RecommendationGroupKind.STRETCH_EXPERIMENT, picks);
	}

	private double computeUseItUpScore(RecipeSignals s, BrewingHistoryIndex history)
	{
		double score = 0D;
		for (InventoryMatchLine line : s.getInventoryMatch().getLines())
		{
			if (!line.isFullyCovered())
			{
				continue;
			}
			double surplus = line.getSurplusAfterBrew();
			if (surplus <= 0D)
			{
				continue;
			}
			double required = line.getRequired();
			if (required <= 0D)
			{
				continue;
			}
			double useRatio = Math.min(1D, required / (required + surplus));
			if (useRatio >= 0.5D)
			{
				score += 4D;
			}
			else if (useRatio >= 0.25D)
			{
				score += 2D;
			}
			if (history.ingredientRecentUseCount(
				mclachlan.brewday.inventory.InventoryLineItem.getUniqueId(
					line.getIngredientName(), line.getType())) == 0)
			{
				score += 2D;
			}
		}
		return score;
	}

	private List<String> describeUseItUpIngredients(RecipeSignals s, BrewingHistoryIndex history)
	{
		List<String> lines = new ArrayList<>();
		for (InventoryMatchLine line : s.getInventoryMatch().getLines())
		{
			if (!line.isFullyCovered() || line.getSurplusAfterBrew() <= 0D)
			{
				continue;
			}
			String id = mclachlan.brewday.inventory.InventoryLineItem.getUniqueId(
				line.getIngredientName(), line.getType());
			if (history.ingredientRecentUseCount(id) == 0)
			{
				lines.add(RecommendationUiSupport.useItUpUnusedIngredientDetail(line.getIngredientName()));
			}
			else if (line.getRequired() / (line.getRequired() + line.getSurplusAfterBrew()) >= 0.5D)
			{
				lines.add(RecommendationUiSupport.useItUpLargeShareDetail(line.getIngredientName()));
			}
			if (lines.size() >= 2)
			{
				break;
			}
		}
		return lines;
	}

	private String buildInventoryExplanation(RecipeSignals s)
	{
		if (s.getInventoryMatch().isFullyBrewable())
		{
			return RecommendationUiSupport.inventoryFullyBrewableExplanation();
		}
		return RecommendationUiSupport.inventoryStrongMatchExplanation();
	}

	private double recentShownPenalty(RecipeSignals s, RecommendationContext context)
	{
		return context.getRecentlyShownRecipeNames().contains(s.getRecipeName()) ? 8D : 0D;
	}

	private List<Recommendation> pickDiverse(
		List<Scored<RecipeSignals>> scored,
		int minGroupSize,
		int maxPerGroup,
		Function<RecipeSignals, Recommendation> mapper)
	{
		List<Recommendation> result = new ArrayList<>();
		Set<String> usedFamilies = new HashSet<>();
		Set<String> usedRecipes = new HashSet<>();

		for (Scored<RecipeSignals> entry : scored)
		{
			RecipeSignals s = entry.item();
			if (usedRecipes.contains(s.getRecipeName()))
			{
				continue;
			}
			if (!usedFamilies.isEmpty() && usedFamilies.contains(s.getStyleFamilyKey()))
			{
				continue;
			}
			result.add(mapper.apply(s));
			usedRecipes.add(s.getRecipeName());
			usedFamilies.add(s.getStyleFamilyKey());
			if (result.size() >= maxPerGroup)
			{
				break;
			}
		}

		if (result.size() < minGroupSize)
		{
			for (Scored<RecipeSignals> entry : scored)
			{
				RecipeSignals s = entry.item();
				if (usedRecipes.contains(s.getRecipeName()))
				{
					continue;
				}
				result.add(mapper.apply(s));
				usedRecipes.add(s.getRecipeName());
				if (result.size() >= maxPerGroup)
				{
					break;
				}
			}
		}

		if (result.size() < minGroupSize)
		{
			return List.of();
		}
		return result;
	}

	private void addGroupIfPresent(List<RecommendationGroup> groups, RecommendationGroup group)
	{
		if (group != null && !group.getRecommendations().isEmpty())
		{
			groups.add(group);
		}
	}

	private List<String> collectShownRecipeNames(List<RecommendationGroup> groups)
	{
		List<String> names = new ArrayList<>();
		for (RecommendationGroup group : groups)
		{
			for (Recommendation rec : group.getRecommendations())
			{
				if (!names.contains(rec.getRecipeName()))
				{
					names.add(rec.getRecipeName());
				}
			}
		}
		return names;
	}

	private record Scored<T>(T item, double score)
	{
	}
}
