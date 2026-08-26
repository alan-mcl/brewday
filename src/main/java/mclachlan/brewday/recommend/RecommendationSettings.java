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

import mclachlan.brewday.Settings;

/**
 * Thresholds for {@link BrewRecommendationEngine} group filters, persisted in settings.
 */
public final class RecommendationSettings
{
	private final int minGroupSize;
	private final int bestInventoryMinMatch;
	private final long dueRepeatGapMonths;
	private final long styleRevisitGapMonths;
	private final double somethingDifferentMinContrast;
	private final int neverBrewedMinMatch;
	private final long forgottenGapMonths;
	private final int useItUpMinMatch;
	private final int onePurchaseMinMatch;
	private final double stretchMinContrast;
	private final int stretchMinMatch;

	public RecommendationSettings(
		int minGroupSize,
		int bestInventoryMinMatch,
		long dueRepeatGapMonths,
		long styleRevisitGapMonths,
		double somethingDifferentMinContrast,
		int neverBrewedMinMatch,
		long forgottenGapMonths,
		int useItUpMinMatch,
		int onePurchaseMinMatch,
		double stretchMinContrast,
		int stretchMinMatch)
	{
		this.minGroupSize = minGroupSize;
		this.bestInventoryMinMatch = bestInventoryMinMatch;
		this.dueRepeatGapMonths = dueRepeatGapMonths;
		this.styleRevisitGapMonths = styleRevisitGapMonths;
		this.somethingDifferentMinContrast = somethingDifferentMinContrast;
		this.neverBrewedMinMatch = neverBrewedMinMatch;
		this.forgottenGapMonths = forgottenGapMonths;
		this.useItUpMinMatch = useItUpMinMatch;
		this.onePurchaseMinMatch = onePurchaseMinMatch;
		this.stretchMinContrast = stretchMinContrast;
		this.stretchMinMatch = stretchMinMatch;
	}

	public static RecommendationSettings defaults()
	{
		return new RecommendationSettings(
			2,
			50,
			6L,
			12L,
			1.0D,
			40,
			12L,
			60,
			80,
			0.5D,
			55);
	}

	public static RecommendationSettings from(Settings settings)
	{
		RecommendationSettings d = defaults();
		return new RecommendationSettings(
			parseInt(settings.get(Settings.RECOMMEND_MIN_GROUP_SIZE), d.minGroupSize, 1, 3),
			parseInt(settings.get(Settings.RECOMMEND_BEST_INVENTORY_MIN_MATCH), d.bestInventoryMinMatch, 0, 100),
			parseLong(settings.get(Settings.RECOMMEND_DUE_REPEAT_GAP_MONTHS), d.dueRepeatGapMonths, 1L, Long.MAX_VALUE),
			parseLong(settings.get(Settings.RECOMMEND_STYLE_REVISIT_GAP_MONTHS), d.styleRevisitGapMonths, 1L, Long.MAX_VALUE),
			parseDouble(settings.get(Settings.RECOMMEND_SOMETHING_DIFFERENT_MIN_CONTRAST), d.somethingDifferentMinContrast, 0D, Double.MAX_VALUE),
			parseInt(settings.get(Settings.RECOMMEND_NEVER_BREWED_MIN_MATCH), d.neverBrewedMinMatch, 0, 100),
			parseLong(settings.get(Settings.RECOMMEND_FORGOTTEN_GAP_MONTHS), d.forgottenGapMonths, 1L, Long.MAX_VALUE),
			parseInt(settings.get(Settings.RECOMMEND_USE_IT_UP_MIN_MATCH), d.useItUpMinMatch, 0, 100),
			parseInt(settings.get(Settings.RECOMMEND_ONE_PURCHASE_MIN_MATCH), d.onePurchaseMinMatch, 0, 99),
			parseDouble(settings.get(Settings.RECOMMEND_STRETCH_MIN_CONTRAST), d.stretchMinContrast, 0D, Double.MAX_VALUE),
			parseInt(settings.get(Settings.RECOMMEND_STRETCH_MIN_MATCH), d.stretchMinMatch, 0, 100));
	}

	public void persist(Settings settings)
	{
		settings.set(Settings.RECOMMEND_MIN_GROUP_SIZE, "" + minGroupSize);
		settings.set(Settings.RECOMMEND_BEST_INVENTORY_MIN_MATCH, "" + bestInventoryMinMatch);
		settings.set(Settings.RECOMMEND_DUE_REPEAT_GAP_MONTHS, "" + dueRepeatGapMonths);
		settings.set(Settings.RECOMMEND_STYLE_REVISIT_GAP_MONTHS, "" + styleRevisitGapMonths);
		settings.set(Settings.RECOMMEND_SOMETHING_DIFFERENT_MIN_CONTRAST, "" + somethingDifferentMinContrast);
		settings.set(Settings.RECOMMEND_NEVER_BREWED_MIN_MATCH, "" + neverBrewedMinMatch);
		settings.set(Settings.RECOMMEND_FORGOTTEN_GAP_MONTHS, "" + forgottenGapMonths);
		settings.set(Settings.RECOMMEND_USE_IT_UP_MIN_MATCH, "" + useItUpMinMatch);
		settings.set(Settings.RECOMMEND_ONE_PURCHASE_MIN_MATCH, "" + onePurchaseMinMatch);
		settings.set(Settings.RECOMMEND_STRETCH_MIN_CONTRAST, "" + stretchMinContrast);
		settings.set(Settings.RECOMMEND_STRETCH_MIN_MATCH, "" + stretchMinMatch);
	}

	public static void clearPersisted(Settings settings)
	{
		settings.set(Settings.RECOMMEND_MIN_GROUP_SIZE, null);
		settings.set(Settings.RECOMMEND_BEST_INVENTORY_MIN_MATCH, null);
		settings.set(Settings.RECOMMEND_DUE_REPEAT_GAP_MONTHS, null);
		settings.set(Settings.RECOMMEND_STYLE_REVISIT_GAP_MONTHS, null);
		settings.set(Settings.RECOMMEND_SOMETHING_DIFFERENT_MIN_CONTRAST, null);
		settings.set(Settings.RECOMMEND_NEVER_BREWED_MIN_MATCH, null);
		settings.set(Settings.RECOMMEND_FORGOTTEN_GAP_MONTHS, null);
		settings.set(Settings.RECOMMEND_USE_IT_UP_MIN_MATCH, null);
		settings.set(Settings.RECOMMEND_ONE_PURCHASE_MIN_MATCH, null);
		settings.set(Settings.RECOMMEND_STRETCH_MIN_CONTRAST, null);
		settings.set(Settings.RECOMMEND_STRETCH_MIN_MATCH, null);
	}

	public int getMinGroupSize()
	{
		return minGroupSize;
	}

	public int getBestInventoryMinMatch()
	{
		return bestInventoryMinMatch;
	}

	public long getDueRepeatGapMonths()
	{
		return dueRepeatGapMonths;
	}

	public long getStyleRevisitGapMonths()
	{
		return styleRevisitGapMonths;
	}

	public double getSomethingDifferentMinContrast()
	{
		return somethingDifferentMinContrast;
	}

	public int getNeverBrewedMinMatch()
	{
		return neverBrewedMinMatch;
	}

	public long getForgottenGapMonths()
	{
		return forgottenGapMonths;
	}

	public int getUseItUpMinMatch()
	{
		return useItUpMinMatch;
	}

	public int getOnePurchaseMinMatch()
	{
		return onePurchaseMinMatch;
	}

	public double getStretchMinContrast()
	{
		return stretchMinContrast;
	}

	public int getStretchMinMatch()
	{
		return stretchMinMatch;
	}

	private static int parseInt(String raw, int fallback, int min, int max)
	{
		if (raw == null || raw.isBlank())
		{
			return fallback;
		}
		try
		{
			int value = Integer.parseInt(raw.trim());
			if (value < min)
			{
				return min;
			}
			if (value > max)
			{
				return max;
			}
			return value;
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	private static long parseLong(String raw, long fallback, long min, long max)
	{
		if (raw == null || raw.isBlank())
		{
			return fallback;
		}
		try
		{
			long value = Long.parseLong(raw.trim());
			if (value < min)
			{
				return min;
			}
			if (value > max)
			{
				return max;
			}
			return value;
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	private static double parseDouble(String raw, double fallback, double min, double max)
	{
		if (raw == null || raw.isBlank())
		{
			return fallback;
		}
		try
		{
			double value = Double.parseDouble(raw.trim());
			if (value < min)
			{
				return min;
			}
			if (value > max)
			{
				return max;
			}
			return value;
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}
