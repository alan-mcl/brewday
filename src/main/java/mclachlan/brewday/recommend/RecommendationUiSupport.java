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

import mclachlan.brewday.util.StringUtils;

/**
 * UI strings for recommendation groups, tags, and explanations.
 */
public final class RecommendationUiSupport
{
	private RecommendationUiSupport()
	{
	}

	public static String groupTitle(RecommendationGroupKind kind)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.group." + kind.name().toLowerCase());
	}

	public static String tagLabel(RecommendationTag tag)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.tag." + tag.name().toLowerCase());
	}

	public static String inventoryFullyBrewableExplanation()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.inventory.full");
	}

	public static String inventoryStrongMatchExplanation()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.inventory.strong");
	}

	public static String dueForRepeatExplanation(int brewCount)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.due.repeat", brewCount);
	}

	public static String styleRevisitExplanation(String styleDisplay)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.style.revisit", styleDisplay);
	}

	public static String neverBrewedExplanation(String styleDisplay)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.never.brewed", styleDisplay);
	}

	public static String forgottenRecipeExplanation()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.forgotten");
	}

	public static String useItUpExplanation()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.use.it.up");
	}

	public static String oneSmallPurchaseExplanation(String ingredientName)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.one.purchase", ingredientName);
	}

	public static String stretchExperimentExplanation()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.explanation.stretch");
	}

	public static String missingIngredientDetail(String ingredientName)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.detail.missing", ingredientName);
	}

	public static String useItUpUnusedIngredientDetail(String ingredientName)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.detail.use.unused", ingredientName);
	}

	public static String useItUpLargeShareDetail(String ingredientName)
	{
		return StringUtils.getUiString("tools.what.should.i.brew.detail.use.large.share", ingredientName);
	}

	public static String monthsSinceLabel(long months)
	{
		if (months <= 0)
		{
			return "";
		}
		if (months < 12)
		{
			return StringUtils.getUiString("tools.what.should.i.brew.months.since", months);
		}
		long years = months / 12;
		long rem = months % 12;
		if (rem == 0)
		{
			if (years == 1)
			{
				return StringUtils.getUiString("tools.what.should.i.brew.year.since");
			}
			return StringUtils.getUiString("tools.what.should.i.brew.years.since", years);
		}
		return StringUtils.getUiString("tools.what.should.i.brew.years.months.since", years, rem);
	}

	public static String contrastDarkMaltFromHoppy()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.contrast.dark.malt.from.hoppy");
	}

	public static String contrastHoppierFromMalt()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.contrast.hoppier.from.malt");
	}

	public static String contrastPaleFromDark()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.contrast.pale.from.dark");
	}

	public static String contrastExpressiveFromClean()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.contrast.expressive.from.clean");
	}

	public static String contrastSessionFromStrong()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.contrast.session.from.strong");
	}

	public static String contrastGeneric()
	{
		return StringUtils.getUiString("tools.what.should.i.brew.contrast.generic");
	}
}
