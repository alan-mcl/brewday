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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.MashPhModel;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;

/**
 * Helpers for per-model mash pH metrics on {@link Volume}.
 */
public final class PhVolumes
{
	private PhVolumes()
	{
	}

	/*-------------------------------------------------------------------------*/

	public static List<MashPhModel> reportedModels()
	{
		return Settings.parseReportedModels(Database.getInstance().getSettings());
	}

	/*-------------------------------------------------------------------------*/

	public static PhUnit get(Volume volume, MashPhModel model)
	{
		return (PhUnit)volume.getMetric(model.toMetric());
	}

	/*-------------------------------------------------------------------------*/

	public static void set(Volume volume, MashPhModel model, PhUnit ph)
	{
		volume.setPh(model, ph);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Sets the same pH value on every reported model.
	 */
	public static void setAllReported(Volume volume, PhUnit ph)
	{
		for (MashPhModel model : reportedModels())
		{
			set(volume, model, ph == null ? null : new PhUnit(ph.get(), ph.isEstimated()));
		}
	}

	/*-------------------------------------------------------------------------*/

	public static void copyAll(Volume from, Volume to)
	{
		for (MashPhModel model : MashPhModel.values())
		{
			PhUnit p = get(from, model);
			if (p != null)
			{
				set(to, model, new PhUnit(p.get(), p.isEstimated()));
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	public static MashPhModel getPrimaryModel()
	{
		List<MashPhModel> models = reportedModels();
		if (models.isEmpty())
		{
			return MashPhModel.MPH;
		}
		return models.get(0);
	}

	/*-------------------------------------------------------------------------*/

	public static PhUnit getPrimary(Volume volume, List<MashPhModel> models)
	{
		if (models == null || models.isEmpty())
		{
			return null;
		}
		return get(volume, models.get(0));
	}

	/*-------------------------------------------------------------------------*/

	public static PhUnit getPrimary(Volume volume)
	{
		return getPrimary(volume, reportedModels());
	}

	/*-------------------------------------------------------------------------*/

	public static boolean hasPerModelPh(Map<Volume.Metric, ?> metricsMap)
	{
		for (Volume.Metric m : metricsMap.keySet())
		{
			if (Settings.MashPhModel.isPhMetric(m))
			{
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/

	public static PhUnit calcMashPh(
		MashPhModel model,
		WaterAddition strikeWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> miscAdditions)
	{
		return switch (model)
		{
			case EZ_WATER -> Equations.calcMashPhEzWater(strikeWater, grainBill, miscAdditions);
			case MPH -> Equations.calcMashPhMpH(strikeWater, grainBill, miscAdditions);
			case KAISER_WATER -> Equations.calcMashPhKaiserWater(strikeWater, grainBill, miscAdditions);
		};
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Blends the per-model pH of two volumes into the output volume using
	 * hydrogen-ion (logarithmic) mixing, independently for each model present.
	 * Where a model is present on only one input, that value is carried through.
	 */
	public static void applyCombined(
		Volume v1,
		VolumeUnit vol1,
		Volume v2,
		VolumeUnit vol2,
		Volume output)
	{
		for (MashPhModel model : MashPhModel.values())
		{
			PhUnit p1 = get(v1, model);
			PhUnit p2 = get(v2, model);

			if (p1 == null && p2 == null)
			{
				continue;
			}

			PhUnit phOut;
			if (p1 == null)
			{
				phOut = new PhUnit(p2.get(), p2.isEstimated());
			}
			else if (p2 == null)
			{
				phOut = new PhUnit(p1.get(), p1.isEstimated());
			}
			else
			{
				phOut = Equations.calcCombinedPh(vol1, p1, vol2, p2);
			}

			set(output, model, phOut);
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Blends the per-model pH of a liquid volume against a single water pH (eg
	 * infusion or sparge liquor) using hydrogen-ion mixing, weighted by the liquor
	 * and water volumes. If the water has no pH, the source pH is carried through
	 * unchanged.
	 */
	public static void applyWaterBlend(
		Volume liquid,
		VolumeUnit liquorVol,
		PhUnit waterPh,
		VolumeUnit waterVol,
		Volume output)
	{
		if (waterPh == null)
		{
			copyAll(liquid, output);
			return;
		}

		for (MashPhModel model : MashPhModel.values())
		{
			PhUnit liquidPh = get(liquid, model);
			if (liquidPh == null)
			{
				continue;
			}

			set(output, model, Equations.calcCombinedPh(liquorVol, liquidPh, waterVol, waterPh));
		}
	}

	/*-------------------------------------------------------------------------*/

	public static String formatReportedLines(Volume volume, List<MashPhModel> models)
	{
		StringBuilder sb = new StringBuilder();
		for (MashPhModel model : models)
		{
			PhUnit p = get(volume, model);
			if (p == null)
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append('\n');
			}
			sb.append(model.toString());
			sb.append(": ");
			sb.append(String.format("%.2f", p.get(Quantity.Unit.PH)));
		}
		if (sb.length() == 0)
		{
			return "-";
		}
		return "pH:\n" + sb;
	}
}
