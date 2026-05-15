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

package mclachlan.brewday.math;

import mclachlan.brewday.BrewdayException;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Keg beer-line length from Bernoulli, Darcy–Weisbach, and Swamee–Jain (Mike Soltys, 2012).
 */
public class KegLineLengthCalculator
{
	/** Water specific weight at SG = 1, N/m³. */
	private static final double WATER_SPECIFIC_WEIGHT_N_PER_M3 = 1000D * 9.80665D;

	/** Vinyl hose absolute roughness, m (0.000016 ft). */
	private static final double HOSE_ROUGHNESS_M = 0.000016D * 0.3048D;

	/** Beer dynamic viscosity, Pa·s (0.00003279 lbf·s/ft² per source article). */
	private static final double DYNAMIC_VISCOSITY_PA_S = 0.00003279D * 47.88025898D;

	private static final double GRAVITY_M_PER_S2 = 9.80665D;

	/** US pint pour volume used by the source calculator, ml. */
	private static final double PINT_VOLUME_ML = 473.176D;

	/** CO₂ gauge pressure increase per metre elevation, kPa (0.5 psi per 1000 ft). */
	private static final double ELEVATION_KPA_PER_METRE = 0.5D * 6.89475728D / 304.8D;

	private static final double MIN_REYNOLDS = 4000D;

	private KegLineLengthCalculator()
	{
	}

	public record Result(LengthUnit hoseLength, double reynoldsNumber, double frictionFactor)
	{
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param elevation metres above reference; may be {@code null} (treated as zero)
	 */
	public static Result calculate(
		DensityUnit specificGravity,
		PressureUnit co2Gauge,
		LengthUnit hoseInsideDiameter,
		LengthUnit tapHeight,
		TimeUnit pintPourTime,
		LengthUnit elevation)
	{
		if (specificGravity == null || co2Gauge == null || hoseInsideDiameter == null
			|| tapHeight == null || pintPourTime == null)
		{
			throw new BrewdayException("Keg line length calculator: missing input");
		}

		double sg = specificGravity.get(SPECIFIC_GRAVITY);
		if (sg <= 0D)
		{
			throw new BrewdayException("Keg line length calculator: specific gravity must be positive");
		}

		double elevationM = elevation == null ? 0D : elevation.get(METRE);
		double pressureKpa = co2Gauge.get(KPA) + elevationM * ELEVATION_KPA_PER_METRE;
		if (pressureKpa <= 0D)
		{
			throw new BrewdayException("Keg line length calculator: CO₂ pressure must be positive");
		}

		double diameterM = hoseInsideDiameter.get(MILLIMETRE) / 1000D;
		if (diameterM <= 0D)
		{
			throw new BrewdayException("Keg line length calculator: hose diameter must be positive");
		}

		double tapHeightM = tapHeight.get(METRE);
		if (tapHeightM < 0D)
		{
			throw new BrewdayException("Keg line length calculator: tap height cannot be negative");
		}

		double pourTimeS = pintPourTime.get();
		if (pourTimeS <= 0D)
		{
			throw new BrewdayException("Keg line length calculator: pour time must be positive");
		}

		double gamma = sg * WATER_SPECIFIC_WEIGHT_N_PER_M3;
		double deltaP = pressureKpa * 1000D;
		double pintVolumeM3 = PINT_VOLUME_ML / 1_000_000D;
		double flowM3PerS = pintVolumeM3 / pourTimeS;
		double areaM2 = Math.PI * diameterM * diameterM / 4D;
		double velocityMPerS = flowM3PerS / areaM2;
		double densityKgPerM3 = gamma / GRAVITY_M_PER_S2;
		double kinematicViscosityM2PerS = DYNAMIC_VISCOSITY_PA_S / densityKgPerM3;
		double reynolds = velocityMPerS * diameterM / kinematicViscosityM2PerS;

		if (reynolds < MIN_REYNOLDS)
		{
			throw new BrewdayException(
				"Keg line length calculator: Reynolds number too low for Swamee–Jain (" + reynolds + ")");
		}

		double frictionFactor = swameeJainFrictionFactor(HOSE_ROUGHNESS_M, diameterM, reynolds);
		double staticHeadPa = gamma * tapHeightM;
		double availablePa = deltaP - staticHeadPa;

		if (availablePa <= 0D)
		{
			throw new BrewdayException(
				"Keg line length calculator: static head exceeds available gauge pressure");
		}

		double lengthM = availablePa * diameterM * 2D * GRAVITY_M_PER_S2
			/ (frictionFactor * gamma * velocityMPerS * velocityMPerS);

		if (lengthM <= 0D || !Double.isFinite(lengthM))
		{
			throw new BrewdayException("Keg line length calculator: invalid hose length result");
		}

		return new Result(new LengthUnit(lengthM, METRE), reynolds, frictionFactor);
	}

	/*-------------------------------------------------------------------------*/

	private static double swameeJainFrictionFactor(double roughnessM, double diameterM, double reynolds)
	{
		double term = roughnessM / (3.7D * diameterM) + 5.74D / Math.pow(reynolds, 0.9D);
		double logTerm = Math.log10(term);
		return 0.25D / (logTerm * logTerm);
	}
}
