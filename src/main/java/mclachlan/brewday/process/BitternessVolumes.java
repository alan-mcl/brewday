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
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.BitternessUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.VolumeUnit;

/**
 * Helpers for per-formula bitterness metrics on {@link Volume}.
 */
public final class BitternessVolumes
{
	private BitternessVolumes()
	{
	}

	/*-------------------------------------------------------------------------*/

	public static List<HopBitternessFormula> reportedFormulas()
	{
		return Settings.parseReportedFormulas(Database.getInstance().getSettings());
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit get(Volume volume, HopBitternessFormula formula)
	{
		return (BitternessUnit)volume.getMetric(formula.toMetric());
	}

	/*-------------------------------------------------------------------------*/

	public static void set(Volume volume, HopBitternessFormula formula, BitternessUnit bitterness)
	{
		volume.setBitterness(formula, bitterness);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Sets the same bitterness value on every reported formula.
	 */
	public static void setAllReported(Volume volume, BitternessUnit bitterness)
	{
		for (HopBitternessFormula formula : reportedFormulas())
		{
			set(volume, formula, bitterness == null ? null : new BitternessUnit(bitterness));
		}
	}

	/*-------------------------------------------------------------------------*/

	public static void copyAll(Volume from, Volume to)
	{
		for (HopBitternessFormula formula : HopBitternessFormula.values())
		{
			BitternessUnit b = get(from, formula);
			if (b != null)
			{
				set(to, formula, new BitternessUnit(b));
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit getPrimary(Volume volume, List<HopBitternessFormula> formulas)
	{
		if (formulas == null || formulas.isEmpty())
		{
			return null;
		}
		return get(volume, formulas.get(0));
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit getPrimary(Volume volume)
	{
		return getPrimary(volume, reportedFormulas());
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit zero()
	{
		return new BitternessUnit(0);
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit getOrZero(Volume volume, HopBitternessFormula formula)
	{
		BitternessUnit b = get(volume, formula);
		return b == null ? zero() : b;
	}

	/*-------------------------------------------------------------------------*/

	public static void add(Volume volume, HopBitternessFormula formula, BitternessUnit delta)
	{
		BitternessUnit current = getOrZero(volume, formula);
		current.add(delta);
		set(volume, formula, current);
	}

	/*-------------------------------------------------------------------------*/

	public static void applyVolumeChange(
		Volume input,
		Volume output,
		VolumeUnit volumeOut,
		List<HopBitternessFormula> formulas)
	{
		for (HopBitternessFormula formula : formulas)
		{
			BitternessUnit bitternessOut = Equations.calcBitternessWithVolumeChange(
				input.getVolume(),
				get(input, formula),
				volumeOut);
			set(output, formula, bitternessOut);
		}
	}

	/*-------------------------------------------------------------------------*/

	public static void applyCombined(
		VolumeUnit v1,
		Volume input,
		VolumeUnit v2,
		Volume input2,
		Volume output,
		List<HopBitternessFormula> formulas)
	{
		for (HopBitternessFormula formula : formulas)
		{
			BitternessUnit bitternessOut = Equations.calcCombinedBitterness(
				v1,
				get(input, formula),
				v2,
				get(input2, formula));
			set(output, formula, bitternessOut);
		}
	}

	/*-------------------------------------------------------------------------*/

	public static String formatReportedLines(Volume volume, List<HopBitternessFormula> formulas)
	{
		StringBuilder sb = new StringBuilder();
		for (HopBitternessFormula formula : formulas)
		{
			BitternessUnit b = get(volume, formula);
			if (b == null)
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append('\n');
			}
			sb.append(formula.toString());
			sb.append(": ");
			sb.append(String.format("%.1f IBU", b.get(mclachlan.brewday.math.Quantity.Unit.IBU)));
		}
		if (sb.length() == 0)
		{
			return "-";
		}
		return "Bitterness:\n" + sb;
	}
}
