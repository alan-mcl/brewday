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

import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.HopAddition;

/**
 * Helpers for alpha-acid mass metrics on {@link Volume}.
 */
public final class HopAcidVolumes
{
	private HopAcidVolumes()
	{
	}

	/*-------------------------------------------------------------------------*/

	public static WeightUnit get(Volume volume, Volume.Metric metric)
	{
		return (WeightUnit)volume.getMetric(metric);
	}

	/*-------------------------------------------------------------------------*/

	public static void set(Volume volume, Volume.Metric metric, WeightUnit mass)
	{
		switch (metric)
		{
			case ALPHA_ACIDS_MG:
				volume.setAlphaAcidsMg(mass);
				break;
			case ISO_ALPHA_ACIDS_MG:
				volume.setIsoAlphaAcidsMg(mass);
				break;
			default:
				throw new IllegalArgumentException("invalid hop acid metric: " + metric);
		}
	}

	/*-------------------------------------------------------------------------*/

	public static WeightUnit zero()
	{
		return new WeightUnit(0, Quantity.Unit.MILLIGRAMS);
	}

	/*-------------------------------------------------------------------------*/

	public static WeightUnit getOrZero(Volume volume, Volume.Metric metric)
	{
		WeightUnit mass = get(volume, metric);
		return mass == null ? zero() : mass;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Detached copy for step working state; safe to mutate without altering {@code volume}.
	 */
	public static WeightUnit copyOrZero(Volume volume, Volume.Metric metric)
	{
		return new WeightUnit(getOrZero(volume, metric));
	}

	/*-------------------------------------------------------------------------*/

	public static void add(Volume volume, Volume.Metric metric, WeightUnit delta)
	{
		WeightUnit current = getOrZero(volume, metric);
		current.add(delta);
		set(volume, metric, current);
	}

	/*-------------------------------------------------------------------------*/

	public static void copyAll(Volume from, Volume to)
	{
		for (Volume.Metric metric : hopAcidMetrics())
		{
			WeightUnit mass = get(from, metric);
			if (mass != null)
			{
				set(to, metric, new WeightUnit(mass));
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	public static void addHopAlpha(Volume volume, HopAddition hop)
	{
		add(volume, Volume.Metric.ALPHA_ACIDS_MG, Equations.calcHopAlphaAcidsMg(hop));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Moves mass from non-isomerized alpha to iso-alpha (partition model).
	 */
	public static void isomerize(Volume volume, WeightUnit isoMg)
	{
		if (isoMg == null || isoMg.get(Quantity.Unit.MILLIGRAMS) <= 0)
		{
			return;
		}

		WeightUnit alpha = getOrZero(volume, Volume.Metric.ALPHA_ACIDS_MG);
		double isoAmount = isoMg.get(Quantity.Unit.MILLIGRAMS);
		double alphaAmount = alpha.get(Quantity.Unit.MILLIGRAMS);
		double transfer = Math.min(alphaAmount, isoAmount);

		WeightUnit transferUnit = new WeightUnit(transfer, Quantity.Unit.MILLIGRAMS, isoMg.isEstimated());
		alpha.subtract(transferUnit);
		set(volume, Volume.Metric.ALPHA_ACIDS_MG, alpha);
		add(volume, Volume.Metric.ISO_ALPHA_ACIDS_MG, transferUnit);
	}

	/*-------------------------------------------------------------------------*/

	public static void applyCombined(
		Volume input1,
		VolumeUnit v1,
		Volume input2,
		VolumeUnit v2,
		Volume output)
	{
		for (Volume.Metric metric : hopAcidMetrics())
		{
			WeightUnit combined = getOrZero(input1, metric);
			combined.add(getOrZero(input2, metric));
			set(output, metric, combined);
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Copies hop-acid masses unchanged (mass conserved through dilution or evaporation).
	 */
	public static void applyVolumeUnchanged(Volume input, Volume output)
	{
		copyAll(input, output);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Scales hop-acid masses by {@code volumeOut / volumeIn}.
	 */
	public static void applyProportionalToVolume(
		Volume input,
		VolumeUnit volumeIn,
		VolumeUnit volumeOut,
		Volume output)
	{
		double fraction = volumeFraction(volumeIn, volumeOut);
		for (Volume.Metric metric : hopAcidMetrics())
		{
			set(output, metric, scaleMass(getOrZero(input, metric), fraction));
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Partitions hop-acid masses from {@code input} into two outputs by volume ratio.
	 */
	public static void applySplit(
		Volume input,
		VolumeUnit volumeIn,
		VolumeUnit volume1Out,
		Volume output1,
		VolumeUnit volume2Out,
		Volume output2)
	{
		double totalOut = volume1Out.get() + volume2Out.get();
		if (totalOut <= 0)
		{
			for (Volume.Metric metric : hopAcidMetrics())
			{
				set(output1, metric, zero());
				set(output2, metric, zero());
			}
			return;
		}

		double f1 = volume1Out.get() / totalOut;
		double f2 = volume2Out.get() / totalOut;

		for (Volume.Metric metric : hopAcidMetrics())
		{
			WeightUnit massIn = getOrZero(input, metric);
			set(output1, metric, scaleMass(massIn, f1));
			set(output2, metric, scaleMass(massIn, f2));
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Scales hop-acid masses when liquid volume shrinks with no separate loss volume.
	 */
	public static void applyVolumeLoss(
		Volume input,
		VolumeUnit volumeBefore,
		VolumeUnit volumeAfter,
		Volume output)
	{
		applyProportionalToVolume(input, volumeBefore, volumeAfter, output);
	}

	/*-------------------------------------------------------------------------*/

	public static void applyRetention(Volume volume, double retentionFactor)
	{
		for (Volume.Metric metric : hopAcidMetrics())
		{
			set(volume, metric, scaleMass(getOrZero(volume, metric), retentionFactor));
		}
	}

	/*-------------------------------------------------------------------------*/

	public static void applyIsoRetention(Volume volume, double retentionFactor)
	{
		set(
			volume,
			Volume.Metric.ISO_ALPHA_ACIDS_MG,
			scaleMass(getOrZero(volume, Volume.Metric.ISO_ALPHA_ACIDS_MG), retentionFactor));
	}

	/*-------------------------------------------------------------------------*/

	public static void applyConcentration(Volume volume, double factor)
	{
		for (Volume.Metric metric : hopAcidMetrics())
		{
			WeightUnit mass = getOrZero(volume, metric);
			mass.set(
				mass.get(Quantity.Unit.MILLIGRAMS) * factor,
				Quantity.Unit.MILLIGRAMS);
			set(volume, metric, mass);
		}
	}

	/*-------------------------------------------------------------------------*/

	private static double volumeFraction(VolumeUnit volumeIn, VolumeUnit volumeOut)
	{
		if (volumeIn == null || volumeOut == null || volumeIn.get() <= 0)
		{
			return 0;
		}
		return volumeOut.get() / volumeIn.get();
	}

	/*-------------------------------------------------------------------------*/

	private static WeightUnit scaleMass(WeightUnit mass, double factor)
	{
		if (mass == null || factor == 0)
		{
			return zero();
		}
		return new WeightUnit(
			mass.get(Quantity.Unit.MILLIGRAMS) * factor,
			Quantity.Unit.MILLIGRAMS,
			mass.isEstimated());
	}

	/*-------------------------------------------------------------------------*/

	private static Volume.Metric[] hopAcidMetrics()
	{
		return new Volume.Metric[]
		{
			Volume.Metric.ALPHA_ACIDS_MG,
			Volume.Metric.ISO_ALPHA_ACIDS_MG
		};
	}
}
