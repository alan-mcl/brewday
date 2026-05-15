package mclachlan.brewday.ui.swing.dialogs;

import java.util.ArrayList;
import java.util.List;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.DiastaticPowerUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.FermentableAddition;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.LINTNER;

/**
 * Grain-bill percentage math for {@link SwingGrainProportionAdjusterDialog}.
 */
final class GrainBillProportionAdjuster
{
	private GrainBillProportionAdjuster()
	{
	}

	static List<FermentableAddition> filterAdjustable(List<FermentableAddition> all)
	{
		List<FermentableAddition> result = new ArrayList<>();
		if (all == null)
		{
			return result;
		}
		for (FermentableAddition fa : all)
		{
			if (fa.getFermentable().getType().getQuantityType() == Quantity.Type.WEIGHT)
			{
				result.add(fa);
			}
		}
		return result;
	}

	static double grams(FermentableAddition fa)
	{
		return fa.getQuantity().get(GRAMS);
	}

	static double[] initPercents(List<FermentableAddition> additions, double totalGrams)
	{
		int n = additions.size();
		double[] percents = new double[n];
		if (totalGrams <= 0D)
		{
			return percents;
		}
		for (int i = 0; i < n; i++)
		{
			percents[i] = 100D * grams(additions.get(i)) / totalGrams;
		}
		return percents;
	}

	static int chooseCounterIndex(int editedIndex, List<FermentableAddition> additions, double[] gramWeights)
	{
		int n = additions.size();
		if (n < 2 || editedIndex < 0 || editedIndex >= n)
		{
			return -1;
		}

		int best = -1;
		double bestDp = -1D;
		double bestWeight = -1D;

		for (int i = 0; i < n; i++)
		{
			if (i == editedIndex)
			{
				continue;
			}
			Fermentable f = additions.get(i).getFermentable();
			if (f.getType() == Fermentable.Type.GRAIN && f.isRecommendMash())
			{
				double dp = diastaticPowerLintner(f);
				double w = gramWeights[i];
				if (dp > bestDp || (dp == bestDp && w > bestWeight))
				{
					bestDp = dp;
					bestWeight = w;
					best = i;
				}
			}
		}
		if (best >= 0)
		{
			return best;
		}
		return indexOfLargestWeightExcluding(editedIndex, gramWeights);
	}

	static void applyPercentChange(
		int editedIndex,
		int counterIndex,
		double newPercent,
		double[] percents,
		double[] gramWeights,
		double totalGrams)
	{
		int n = percents.length;
		if (editedIndex < 0 || editedIndex >= n || counterIndex < 0 || counterIndex >= n
			|| editedIndex == counterIndex)
		{
			return;
		}

		double sumOthers = 0D;
		for (int k = 0; k < n; k++)
		{
			if (k != editedIndex && k != counterIndex)
			{
				sumOthers += percents[k];
			}
		}

		percents[editedIndex] = newPercent;
		percents[counterIndex] = 100D - newPercent - sumOthers;

		if (percents[counterIndex] < 0D)
		{
			percents[counterIndex] = 0D;
			percents[editedIndex] = 100D - sumOthers;
		}
		if (percents[editedIndex] < 0D)
		{
			percents[editedIndex] = 0D;
			percents[counterIndex] = 100D - sumOthers;
		}

		recomputeWeightsFromPercents(percents, gramWeights, totalGrams, counterIndex);
	}

	static void recomputeWeightsFromPercents(
		double[] percents,
		double[] gramWeights,
		double totalGrams,
		int residualIndex)
	{
		int n = percents.length;
		double sum = 0D;
		for (int k = 0; k < n; k++)
		{
			gramWeights[k] = totalGrams * percents[k] / 100D;
			sum += gramWeights[k];
		}
		if (residualIndex >= 0 && residualIndex < n)
		{
			gramWeights[residualIndex] += totalGrams - sum;
		}
	}

	private static double diastaticPowerLintner(Fermentable f)
	{
		DiastaticPowerUnit dp = f.getDiastaticPower();
		return dp == null ? 0D : dp.get(LINTNER);
	}

	private static int indexOfLargestWeightExcluding(int excludedIndex, double[] gramWeights)
	{
		int best = -1;
		double bestWeight = -1D;
		for (int i = 0; i < gramWeights.length; i++)
		{
			if (i == excludedIndex)
			{
				continue;
			}
			if (gramWeights[i] > bestWeight)
			{
				bestWeight = gramWeights[i];
				best = i;
			}
		}
		return best;
	}
}
