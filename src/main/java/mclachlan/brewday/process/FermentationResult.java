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
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.recipe.YeastCulture;

/**
 * Outcome of a single {@link Ferment} phase calculation.
 */
public class FermentationResult
{
	private final boolean hasFermentation;
	private final double blendAttenuation;
	private final double effectiveAttenuation;
	private final DensityUnit estimatedFg;
	private final double pitchRatio;
	private final double durationFactor;
	private final double pitchFactor;
	private final TemperatureUnit averageTemp;
	private final List<YeastCulture> evolvedCultures;

	/*-------------------------------------------------------------------------*/
	public FermentationResult(
		boolean hasFermentation,
		double blendAttenuation,
		double effectiveAttenuation,
		DensityUnit estimatedFg,
		double pitchRatio,
		double durationFactor,
		double pitchFactor,
		TemperatureUnit averageTemp,
		List<YeastCulture> evolvedCultures)
	{
		this.hasFermentation = hasFermentation;
		this.blendAttenuation = blendAttenuation;
		this.effectiveAttenuation = effectiveAttenuation;
		this.estimatedFg = estimatedFg;
		this.pitchRatio = pitchRatio;
		this.durationFactor = durationFactor;
		this.pitchFactor = pitchFactor;
		this.averageTemp = averageTemp;
		this.evolvedCultures = evolvedCultures == null
			? Collections.emptyList()
			: Collections.unmodifiableList(new ArrayList<>(evolvedCultures));
	}

	/*-------------------------------------------------------------------------*/
	public static FermentationResult noFermentation()
	{
		return new FermentationResult(
			false,
			0D,
			0D,
			null,
			0D,
			0D,
			0D,
			null,
			Collections.emptyList());
	}

	/*-------------------------------------------------------------------------*/
	public boolean hasFermentation()
	{
		return hasFermentation;
	}

	public double getBlendAttenuation()
	{
		return blendAttenuation;
	}

	public double getEffectiveAttenuation()
	{
		return effectiveAttenuation;
	}

	public DensityUnit getEstimatedFg()
	{
		return estimatedFg;
	}

	public double getPitchRatio()
	{
		return pitchRatio;
	}

	public double getDurationFactor()
	{
		return durationFactor;
	}

	public double getPitchFactor()
	{
		return pitchFactor;
	}

	public TemperatureUnit getAverageTemp()
	{
		return averageTemp;
	}

	public List<YeastCulture> getEvolvedCultures()
	{
		return evolvedCultures;
	}
}
