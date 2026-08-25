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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;

/**
 * Shared inventory match calculation for all recommendation groups.
 */
public final class InventoryMatchCalculator
{
	private static final double TYPE_FERMENTABLE = 1.0D;
	private static final double TYPE_HOP = 0.55D;
	private static final double TYPE_YEAST = 0.35D;
	private static final double TYPE_MISC = 0.15D;

	private InventoryMatchCalculator()
	{
	}

	public static InventoryMatch calculate(Recipe recipe, Map<String, InventoryLineItem> inventory)
	{
		List<IngredientAddition> bom = recipe.getIngredientsBillOfMaterials();
		Map<String, AggregatedLine> aggregated = aggregateLines(bom);

		double fermentableTotal = sumRequiredByType(aggregated, IngredientAddition.Type.FERMENTABLES);
		double hopTotal = sumRequiredByType(aggregated, IngredientAddition.Type.HOPS);

		List<InventoryMatchLine> lines = new ArrayList<>();
		List<InventoryMatchLine> missing = new ArrayList<>();
		double weightedCoverage = 0D;
		double totalWeight = 0D;
		boolean allFullyCovered = true;

		for (AggregatedLine agg : aggregated.values())
		{
			if (agg.type == IngredientAddition.Type.WATER)
			{
				continue;
			}

			InventoryLineItem stock = inventory.get(agg.inventoryId);
			Quantity.Unit unit = stock != null ? stock.getUnit() : agg.unit;
			double required = agg.required;
			double inStock = stock == null ? 0D : stock.getQuantity().get(unit);
			double coverage = required <= 0D ? 1D : Math.min(1D, inStock / required);

			double shareWeight = shareWithinType(agg, fermentableTotal, hopTotal);
			double typeMultiplier = typeMultiplier(agg.type);
			double lineWeight = shareWeight * typeMultiplier;

			boolean criticalMiss = isCriticalMiss(agg, coverage, fermentableTotal);

			InventoryMatchLine line = new InventoryMatchLine(
				agg.ingredientName,
				agg.type,
				required,
				inStock,
				coverage,
				lineWeight,
				unit,
				criticalMiss);
			lines.add(line);

			if (lineWeight > 0D)
			{
				weightedCoverage += coverage * lineWeight;
				totalWeight += lineWeight;
			}

			if (!line.isFullyCovered())
			{
				allFullyCovered = false;
				missing.add(line);
			}
		}

		int percent = totalWeight <= 0D
			? 100
			: (int)Math.round(100D * weightedCoverage / totalWeight);

		if (!allFullyCovered)
		{
			percent = Math.min(percent, 99);
		}

		for (InventoryMatchLine line : lines)
		{
			if (line.isCriticalMiss())
			{
				percent = Math.min(percent, 75);
				break;
			}
		}

		percent = Math.max(0, Math.min(100, percent));

		return new InventoryMatch(percent, allFullyCovered, lines, missing);
	}

	private static Map<String, AggregatedLine> aggregateLines(List<IngredientAddition> bom)
	{
		Map<String, AggregatedLine> result = new HashMap<>();
		for (IngredientAddition ia : bom)
		{
			if (ia.getType() == IngredientAddition.Type.WATER)
			{
				continue;
			}
			String id = ia.getInventoryId();
			AggregatedLine agg = result.get(id);
			if (agg == null)
			{
				agg = new AggregatedLine(
					ia.getName(),
					ia.getType(),
					id,
					ia.getUnit(),
					ia instanceof FermentableAddition);
				result.put(id, agg);
			}
			Quantity.Unit unit = ia.getUnit();
			agg.required += ia.getQuantity().get(unit);
			if (agg.unit == null)
			{
				agg.unit = unit;
			}
		}
		return result;
	}

	private static double sumRequiredByType(
		Map<String, AggregatedLine> aggregated,
		IngredientAddition.Type type)
	{
		double total = 0D;
		for (AggregatedLine agg : aggregated.values())
		{
			if (agg.type == type)
			{
				total += toGramsIfWeight(agg);
			}
		}
		return total;
	}

	private static double toGramsIfWeight(AggregatedLine agg)
	{
		if (agg.unit == null)
		{
			return agg.required;
		}
		try
		{
			return Quantity.parseQuantity("" + agg.required, agg.unit).get(GRAMS);
		}
		catch (RuntimeException ex)
		{
			return agg.required;
		}
	}

	private static double shareWithinType(
		AggregatedLine agg,
		double fermentableTotal,
		double hopTotal)
	{
		return switch (agg.type)
		{
			case FERMENTABLES ->
			{
				double grams = toGramsIfWeight(agg);
				yield fermentableTotal <= 0D ? 0D : grams / fermentableTotal;
			}
			case HOPS ->
			{
				double grams = toGramsIfWeight(agg);
				yield hopTotal <= 0D ? 0D : grams / hopTotal;
			}
			case YEAST -> 1D;
			case MISC -> 0.5D;
			default -> 0D;
		};
	}

	private static double typeMultiplier(IngredientAddition.Type type)
	{
		return switch (type)
		{
			case FERMENTABLES -> TYPE_FERMENTABLE;
			case HOPS -> TYPE_HOP;
			case YEAST -> TYPE_YEAST;
			case MISC -> TYPE_MISC;
			default -> 0D;
		};
	}

	private static boolean isCriticalMiss(
		AggregatedLine agg,
		double coverage,
		double fermentableTotal)
	{
		if (coverage >= 0.999D)
		{
			return false;
		}
		if (agg.type == IngredientAddition.Type.YEAST)
		{
			return true;
		}
		if (agg.type == IngredientAddition.Type.FERMENTABLES && fermentableTotal > 0D)
		{
			double share = toGramsIfWeight(agg) / fermentableTotal;
			return share >= 0.25D;
		}
		return false;
	}

	private static final class AggregatedLine
	{
		private final String ingredientName;
		private final IngredientAddition.Type type;
		private final String inventoryId;
		private Quantity.Unit unit;
		private double required;
		private final boolean fermentable;

		private AggregatedLine(
			String ingredientName,
			IngredientAddition.Type type,
			String inventoryId,
			Quantity.Unit unit,
			boolean fermentable)
		{
			this.ingredientName = ingredientName;
			this.type = type;
			this.inventoryId = inventoryId;
			this.unit = unit;
			this.fermentable = fermentable;
		}
	}
}
