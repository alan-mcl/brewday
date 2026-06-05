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

package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.HopStand;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.process.Steep;
import mclachlan.brewday.process.YeastRehydrate;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

/**
 * Migrates legacy {@link Stand} steps with ingredient additions to {@link Steep},
 * {@link HopStand}, or {@link YeastRehydrate} during Brewday database import.
 */
public final class StandStepMigration
{
	private StandStepMigration()
	{
	}

	/*-------------------------------------------------------------------------*/
	public record Result(int migrated, int manualReview, List<String> warnings)
	{
		public Result
		{
			warnings = warnings == null ? List.of() : List.copyOf(warnings);
		}

		public Result merge(Result other)
		{
			List<String> merged = new ArrayList<>(warnings);
			merged.addAll(other.warnings);
			return new Result(migrated + other.migrated, manualReview + other.manualReview, merged);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static Result migrateRecipe(Recipe recipe)
	{
		if (recipe == null || recipe.getSteps() == null)
		{
			return new Result(0, 0, List.of());
		}

		int migrated = 0;
		int manualReview = 0;
		List<String> warnings = new ArrayList<>();

		List<ProcessStep> steps = recipe.getSteps();
		for (int i = 0; i < steps.size(); i++)
		{
			ProcessStep step = steps.get(i);
			if (!(step instanceof Stand stand))
			{
				continue;
			}

			Decision decision = classify(stand);
			if (decision.action == Action.MANUAL_REVIEW)
			{
				manualReview++;
				warnings.add(String.format(
					"%s / step '%s': %s",
					recipe.getName(),
					stand.getName(),
					decision.reason));
				continue;
			}

			if (decision.action == Action.KEEP_STAND)
			{
				Stand trimmed = new Stand(
					stand.getName(),
					stand.getDescription(),
					stand.getInputVolume(),
					stand.getOutputVolume(),
					new TimeUnit(stand.getDuration().get()),
					List.of());
				trimmed.setCoolingCoefficient(stand.getCoolingCoefficient());
				trimmed.setRecipe(recipe);
				steps.set(i, trimmed);
				continue;
			}

			ProcessStep replacement = buildReplacement(stand, decision.targetType);
			replacement.setRecipe(recipe);
			steps.set(i, replacement);
			migrated++;
			warnings.add(String.format(
				"%s / step '%s': migrated Stand -> %s",
				recipe.getName(),
				stand.getName(),
				decision.targetType));
		}

		return new Result(migrated, manualReview, warnings);
	}

	/*-------------------------------------------------------------------------*/
	public static Result migrateRecipes(Collection<Recipe> recipes)
	{
		Result total = new Result(0, 0, List.of());
		if (recipes == null)
		{
			return total;
		}
		for (Recipe recipe : recipes)
		{
			total = total.merge(migrateRecipe(recipe));
		}
		return total;
	}

	/*-------------------------------------------------------------------------*/
	private enum Action
	{
		MANUAL_REVIEW,
		KEEP_STAND,
		MIGRATE
	}

	/*-------------------------------------------------------------------------*/
	private record Decision(Action action, ProcessStep.Type targetType, String reason)
	{
	}

	/*-------------------------------------------------------------------------*/
	private static Decision classify(Stand stand)
	{
		List<IngredientAddition> additions = stand.getIngredientAdditions();
		if (additions == null || additions.isEmpty())
		{
			if (stand.isLegacyRemoveTrubAndChillerLoss())
			{
				return new Decision(Action.MIGRATE, ProcessStep.Type.HOP_STAND, null);
			}
			return new Decision(Action.KEEP_STAND, null, null);
		}

		boolean hasHops = false;
		boolean hasYeast = false;
		boolean hasFermentables = false;
		boolean hasWaterOrMiscOnly = true;
		boolean hasYeastCulture = false;

		for (IngredientAddition ia : additions)
		{
			switch (ia.getType())
			{
				case HOPS -> hasHops = true;
				case YEAST -> hasYeast = true;
				case FERMENTABLES -> hasFermentables = true;
				case YEAST_CULTURE -> hasYeastCulture = true;
				case WATER, MISC -> { }
				default -> hasWaterOrMiscOnly = false;
			}
			if (ia.getType() != IngredientAddition.Type.WATER
				&& ia.getType() != IngredientAddition.Type.MISC)
			{
				hasWaterOrMiscOnly = false;
			}
		}

		if (hasYeastCulture)
		{
			return new Decision(Action.MANUAL_REVIEW, null,
				"Stand has yeast culture additions; convert manually");
		}

		int primaryKinds = (hasHops ? 1 : 0) + (hasYeast ? 1 : 0) + (hasFermentables ? 1 : 0);
		if (primaryKinds > 1)
		{
			return new Decision(Action.MANUAL_REVIEW, null,
				"Stand has mixed ingredient types; convert manually");
		}

		if (hasWaterOrMiscOnly && !hasHops && !hasYeast && !hasFermentables)
		{
			return new Decision(Action.MANUAL_REVIEW, null,
				"Stand has water or misc additions only; convert manually");
		}

		if (hasHops)
		{
			return new Decision(Action.MIGRATE, ProcessStep.Type.HOP_STAND, null);
		}
		if (hasYeast)
		{
			return new Decision(Action.MIGRATE, ProcessStep.Type.YEAST_REHYDRATE, null);
		}
		if (hasFermentables)
		{
			return new Decision(Action.MIGRATE, ProcessStep.Type.STEEP, null);
		}
		if (stand.isLegacyRemoveTrubAndChillerLoss())
		{
			return new Decision(Action.MIGRATE, ProcessStep.Type.HOP_STAND, null);
		}

		return new Decision(Action.KEEP_STAND, null, null);
	}

	/*-------------------------------------------------------------------------*/
	private static ProcessStep buildReplacement(Stand stand, ProcessStep.Type targetType)
	{
		List<IngredientAddition> ingredients = cloneAdditions(stand.getIngredientAdditions());
		TimeUnit duration = new TimeUnit(stand.getDuration().get());

		return switch (targetType)
		{
			case HOP_STAND ->
			{
				HopStand hopStand = new HopStand(
					stand.getName(),
					stand.getDescription(),
					stand.getInputVolume(),
					stand.getOutputVolume(),
					duration,
					ingredients,
					stand.isLegacyRemoveTrubAndChillerLoss());
				hopStand.setCoolingCoefficient(stand.getCoolingCoefficient());
				yield hopStand;
			}
			case YEAST_REHYDRATE ->
			{
				YeastRehydrate yeastRehydrate = new YeastRehydrate(
					stand.getName(),
					stand.getDescription(),
					stand.getInputVolume(),
					stand.getOutputVolume(),
					duration,
					ingredients);
				yeastRehydrate.setCoolingCoefficient(stand.getCoolingCoefficient());
				yield yeastRehydrate;
			}
			case STEEP ->
			{
				Steep steep = new Steep(
					stand.getName(),
					stand.getDescription(),
					stand.getInputVolume(),
					stand.getOutputVolume(),
					duration,
					ingredients);
				steep.setCoolingCoefficient(stand.getCoolingCoefficient());
				yield steep;
			}
			default -> throw new IllegalStateException("unexpected migration target: " + targetType);
		};
	}

	/*-------------------------------------------------------------------------*/
	private static List<IngredientAddition> cloneAdditions(List<IngredientAddition> src)
	{
		if (src == null || src.isEmpty())
		{
			return new ArrayList<>();
		}
		List<IngredientAddition> result = new ArrayList<>();
		for (IngredientAddition ia : src)
		{
			result.add(ia.clone());
		}
		return result;
	}
}
