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
import java.time.Month;

/**
 * Maps brew date + hemisphere + drink-ready lead to a target seasonal lightness score.
 */
public final class SeasonalLightnessSupport
{
	private SeasonalLightnessSupport()
	{
	}

	public static double targetLightness(
		LocalDate asOf,
		RecommendationSettings.Hemisphere hemisphere,
		int seasonalLeadMonths)
	{
		LocalDate drinkDate = asOf.plusMonths(seasonalLeadMonths);
		Month month = drinkDate.getMonth();
		boolean northern = hemisphere != RecommendationSettings.Hemisphere.SOUTHERN;
		return lightnessForMonth(month, northern);
	}

	static double lightnessForMonth(Month month, boolean northern)
	{
		boolean summer = month == Month.JUNE || month == Month.JULY || month == Month.AUGUST;
		boolean winter = month == Month.DECEMBER || month == Month.JANUARY || month == Month.FEBRUARY;
		if (northern)
		{
			if (summer)
			{
				return 0.75D;
			}
			if (winter)
			{
				return 0.25D;
			}
		}
		else
		{
			if (summer)
			{
				return 0.25D;
			}
			if (winter)
			{
				return 0.75D;
			}
		}
		return 0.5D;
	}
}
