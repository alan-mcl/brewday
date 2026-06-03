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

import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.recipe.HopAddition;
import java.util.List;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * SMPH (Shellhammer–Malowicki–Peacock–Hosom) IBU model from Alchemy Overlord.
 *
 * @see <a href="https://jphosom.github.io/alchemyoverlord/ibu_SMPH.html">SMPH calculator</a>
 */
public final class SmphEquations
{
	private SmphEquations()
	{
	}

	/** Peacock beer IBU conversion: IBU = (50/69.68) × IAA-equivalent ppm sum. */
	public static final double PEACOCK_IBU_FACTOR = 50.0D / 69.68D;

	public static final double INTEGRATION_STEP_MINUTES = 0.01D;

	public static final double IAA_LF_BOIL = 0.51D;
	public static final double FERMENTATION_FACTOR = 0.85D;

	public static final double OAA_BOIL_FACTOR = 0.11D;
	public static final double OAA_STORAGE_FACTOR = 0.33D;
	public static final double SCALE_OAA = 0.9155D;
	public static final double OBA_BOIL_FACTOR = 0.07125D;
	public static final double SCALE_OBA = 0.85D;
	public static final double SCALE_AA = 0.885D;

	public static final double AA_LIMIT_MIN_PPM = 200.0D;
	public static final double AA_LIMIT_MAX_PPM = 580.0D;

	public static final double HOP_PP_RATING = 0.04D;
	public static final double LF_HOP_PP_KETTLE = 0.20D;
	public static final double LF_HOP_PP_DRY_HOP = 0.07D;
	public static final double SCALE_HOP_PP = 0.03066D;
	public static final double FERMENT_HOP_PP = 0.70D;

	public static final double AA_DRY_HOP_SATURATION_PPM = 14.0D;

	private static final double MALOWICKI_K1_A = 7.9E11D;
	private static final double MALOWICKI_K1_B = 11858.0D;
	private static final double MALOWICKI_K2_A = 4.1E12D;
	private static final double MALOWICKI_K2_B = 12994.0D;

	private static final double MIN_ISOMERIZATION_TEMP_K = 333.15D; // ~60 °C

	/*-------------------------------------------------------------------------*/

	/**
	 * @return Boil temperature (K) from equipment elevation (feet).
	 */
	public static double calcBoilTempKelvin(double elevationFeet)
	{
		double tempF = 212.0D - 0.00178D * elevationFeet;
		return (tempF - 32.0D) * 5.0D / 9.0D + 273.15D;
	}

	/*-------------------------------------------------------------------------*/

	public static double calcMalowickiK1(double tempK)
	{
		return MALOWICKI_K1_A * Math.exp(-MALOWICKI_K1_B / tempK);
	}

	/*-------------------------------------------------------------------------*/

	public static double calcMalowickiK2(double tempK)
	{
		return MALOWICKI_K2_A * Math.exp(-MALOWICKI_K2_B / tempK);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Malowicki [IAA] from [AA]0 after steeping minutes at constant temperature.
	 */
	public static double calcMalowickiIaaPpm(double aa0Ppm, double steepMin, double tempK)
	{
		if (steepMin <= 0 || aa0Ppm <= 0)
		{
			return 0D;
		}

		double k1 = calcMalowickiK1(tempK);
		double k2 = calcMalowickiK2(tempK);

		if (Math.abs(k2 - k1) < 1e-12)
		{
			return aa0Ppm * k1 * steepMin * Math.exp(-k1 * steepMin);
		}

		return aa0Ppm * (k1 / (k2 - k1))
			* (Math.exp(-k1 * steepMin) - Math.exp(-k2 * steepMin));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Remaining alpha-acid ppm after isomerization/degradation at constant temperature.
	 */
	public static double calcRemainingAaPpm(double aa0Ppm, double steepMin, double tempK)
	{
		if (steepMin <= 0 || aa0Ppm <= 0)
		{
			return aa0Ppm;
		}

		double aaMg = aa0Ppm;
		double iaaMg = 0D;
		double t = 0D;

		while (t < steepMin)
		{
			double dt = Math.min(INTEGRATION_STEP_MINUTES, steepMin - t);
			double k1 = calcMalowickiK1(tempK);
			double k2 = calcMalowickiK2(tempK);

			double converted = aaMg * k1 * dt;
			double degraded = iaaMg * k2 * dt;

			aaMg -= converted;
			iaaMg += converted;
			iaaMg -= degraded;

			t += dt;
		}

		return Math.max(0, aaMg);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Numerical integration of Malowicki IAA production with temperature profile.
	 */
	public static double integrateMalowickiIaaPpm(
		double aa0Ppm,
		double startMin,
		double endMin,
		TemperatureCallback temperatureAtMinute)
	{
		if (endMin <= startMin || aa0Ppm <= 0)
		{
			return 0D;
		}

		double aaMg = aa0Ppm; // relative mass units
		double iaaMg = 0D;
		double t = startMin;

		while (t < endMin)
		{
			double dt = Math.min(INTEGRATION_STEP_MINUTES, endMin - t);
			double tempK = temperatureAtMinute.getKelvinAtMinute(t);
			if (tempK < MIN_ISOMERIZATION_TEMP_K)
			{
				t += dt;
				continue;
			}

			double k1 = calcMalowickiK1(tempK);
			double k2 = calcMalowickiK2(tempK);

			double converted = aaMg * k1 * dt;
			double degraded = iaaMg * k2 * dt;

			aaMg -= converted;
			iaaMg += converted;
			iaaMg -= degraded;

			t += dt;
		}

		return iaaMg;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Hopping-rate loss factor LF_hopping from Alchemy Overlord blog eq. 24–25.
	 * Returns AA_limit / [AA]0 with soft solubility between {@link #AA_LIMIT_MIN_PPM}
	 * and {@link #AA_LIMIT_MAX_PPM}.
	 */
	public static double calcHoppingRateLossFactor(double aa0Ppm)
	{
		if (aa0Ppm <= 0)
		{
			return 1.0D;
		}
		if (aa0Ppm <= AA_LIMIT_MIN_PPM)
		{
			return 1.0D;
		}

		double slope = Math.log(1.0D - AA_LIMIT_MIN_PPM / AA_LIMIT_MAX_PPM) / AA_LIMIT_MIN_PPM;
		double aaLimit = AA_LIMIT_MAX_PPM * (1.0D - Math.exp(slope * aa0Ppm));
		return aaLimit / aa0Ppm;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Cumulative alpha-acid ppm in wort when {@code target} is added: sum of hops
	 * already in the kettle (addition time &gt;= target time, minutes before flameout).
	 */
	public static double calcCumulativeAaPpmAtAddition(
		List<HopAddition> kettleHops,
		HopAddition target,
		VolumeUnit wortVolume)
	{
		double litres = wortVolume.get(LITRES);
		if (litres <= 0 || target == null || kettleHops == null)
		{
			return 0D;
		}

		double targetTimeMin = target.getTime().get(MINUTES);
		double cumulative = 0D;

		for (HopAddition hop : kettleHops)
		{
			if (hop.getForm() != null && hop.getForm().isPreIsomerized())
			{
				continue;
			}
			if (hop.getTime().get(MINUTES) >= targetTimeMin)
			{
				cumulative += calcHopAaPpm(hop, litres);
			}
		}

		return cumulative;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @deprecated use {@link #calcHoppingRateLossFactor(double)} with cumulative AA at addition
	 */
	@Deprecated
	public static double calcAaSolubilityScale(double totalAaPpm)
	{
		return calcHoppingRateLossFactor(totalAaPpm);
	}

	/*-------------------------------------------------------------------------*/

	public static double calcTotalKettleAaPpm(
		List<HopAddition> kettleHops,
		VolumeUnit wortVolume)
	{
		double litres = wortVolume.get(LITRES);
		if (litres <= 0)
		{
			return 0D;
		}

		double total = 0D;
		for (HopAddition hop : kettleHops)
		{
			if (hop.getForm() != null && hop.getForm().isPreIsomerized())
			{
				continue;
			}
			total += calcHopAaPpm(hop, litres);
		}
		return total;
	}

	/*-------------------------------------------------------------------------*/

	public static double calcHopAaPpm(HopAddition hop, double wortLitres)
	{
		double alpha = hop.getHop().getAlphaAcid().get(PERCENTAGE);
		double grams = hop.getQuantity().get(GRAMS);
		double availability = hop.getForm() != null
			? hop.getForm().getAlphaAvailability()
			: 1.0D;
		return alpha * grams * 1000.0D * availability / wortLitres;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Non-IAA pH loss factor (Alchemy Overlord blog); clamped so factor is not negative.
	 */
	public static double calcPhNonIaaFactor(double wortPh)
	{
		double factor = 0.8948D * wortPh - 4.145D;
		return Math.max(0.1D, factor);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * IAA pH loss factor (approximate; smaller effect than non-IAA).
	 */
	public static double calcPhIaaFactor(double wortPh)
	{
		double factor = 0.25D * wortPh - 0.75D;
		return Math.max(0.85D, Math.min(1.15D, factor));
	}

	/*-------------------------------------------------------------------------*/

	public static double calcMaltPolyphenolBeerPpm(
		double originalGravity,
		double wortPh,
		double postBoilPh)
	{
		double ogTerm = (originalGravity - 1.0D) * 19.0D;
		double phFactor = (2.477D * (wortPh - postBoilPh)) + 1.0D;
		return (69.68D / 51.2D) * ogTerm * phFactor;
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit calcMaltPolyphenolIbu(
		DensityUnit originalGravity,
		PhUnit mashPh,
		PhUnit kettlePh)
	{
		if (originalGravity == null)
		{
			return new BitternessUnit(0);
		}

		double wortPh = mashPh != null ? mashPh.get(PH) : 5.4D;
		double postPh = kettlePh != null ? kettlePh.get(PH) : wortPh;

		double maltPp = calcMaltPolyphenolBeerPpm(
			originalGravity.get(SPECIFIC_GRAVITY),
			wortPh,
			postPh);

		return new BitternessUnit(
			PEACOCK_IBU_FACTOR * maltPp,
			IBU,
			originalGravity.isEstimated());
	}

	/*-------------------------------------------------------------------------*/

	public static double calcHopPolyphenolBeerPpm(
		HopAddition hop,
		double wortLitres,
		boolean dryHop)
	{
		double grams = hop.getQuantity().get(GRAMS);
		double lf = dryHop ? LF_HOP_PP_DRY_HOP : LF_HOP_PP_KETTLE;
		double fermentLf = dryHop ? FERMENT_HOP_PP : 1.0D;
		return HOP_PP_RATING * grams * 1000.0D / wortLitres * lf * fermentLf;
	}

	/*-------------------------------------------------------------------------*/

	public static double calcOaaBeerPpm(
		HopAddition hop,
		double wortLitres,
		boolean kettle,
		boolean dryHop)
	{
		double alpha = hop.getHop().getAlphaAcid().get(PERCENTAGE);
		double grams = hop.getQuantity().get(GRAMS);
		double availability = hop.getForm() != null
			? hop.getForm().getAlphaAvailability()
			: 1.0D;

		double aaPpm = alpha * grams * 1000.0D * availability / wortLitres;
		double factor = kettle ? OAA_BOIL_FACTOR : OAA_STORAGE_FACTOR;

		double oaa = aaPpm * factor;
		if (dryHop)
		{
			double hopPpm = grams * 1000.0D / wortLitres;
			if (hopPpm > 2200.0D)
			{
				oaa *= 1.1181D * Math.exp(-0.0000506D * hopPpm);
			}
		}

		double pelletMult = calcOaaPelletFactor(hop.getForm());
		return oaa * pelletMult * IAA_LF_BOIL;
	}

	/*-------------------------------------------------------------------------*/

	private static double calcOaaPelletFactor(Hop.Form form)
	{
		if (form == Hop.Form.PELLET_T90 || form == Hop.Form.CRYO)
		{
			return 2.0D;
		}
		return 1.0D;
	}

	/*-------------------------------------------------------------------------*/

	public static double calcObaBeerPpm(HopAddition hop, double wortLitres, boolean kettle)
	{
		PercentageUnit beta = hop.getHop().getBetaAcid();
		if (beta == null)
		{
			return 0D;
		}

		double betaPct = beta.get(PERCENTAGE);
		double grams = hop.getQuantity().get(GRAMS);
		double betaPpm = betaPct * grams * 1000.0D / wortLitres;
		double factor = kettle ? OBA_BOIL_FACTOR : OBA_BOIL_FACTOR;
		return betaPpm * factor * IAA_LF_BOIL;
	}

	/*-------------------------------------------------------------------------*/

	public static double calcDryHopAaBeerPpm(HopAddition hop, double beerLitres)
	{
		double aaPpm = calcHopAaPpm(hop, beerLitres);
		return 14.7D * (1.0D - Math.exp(-0.00102D * aaPpm));
	}

	/*-------------------------------------------------------------------------*/

	public static double calcPeacockIbuFromBeerPpm(
		double iaaBeer,
		double aaBeer,
		double oaaBeer,
		double obaBeer,
		double hopPpBeer,
		double maltPpBeer)
	{
		double sum = iaaBeer
			+ aaBeer * SCALE_AA
			+ oaaBeer * SCALE_OAA
			+ obaBeer * SCALE_OBA
			+ hopPpBeer * SCALE_HOP_PP
			+ maltPpBeer;

		return PEACOCK_IBU_FACTOR * sum;
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit calcKettleHopIbuSmph(
		HopAddition hop,
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		PhUnit kettlePh,
		double elevationFeet,
		double equipmentUtilisation,
		double hoppingRateFactor)
	{
		double litres = wortVolume.get(LITRES);
		if (litres <= 0)
		{
			return new BitternessUnit(0);
		}

		boolean estimated = wortGravity.isEstimated() || wortVolume.isEstimated();

		double aa0Ppm = calcHopAaPpm(hop, litres);
		double steepMin = steepDuration.get(MINUTES);
		double boilTempK = calcBoilTempKelvin(elevationFeet);
		double alphaFactor = hoppingRateFactor > 0 ? hoppingRateFactor : 1.0D;

		double iaaWort = integrateMalowickiIaaPpm(
			aa0Ppm * alphaFactor,
			0,
			steepMin,
			t -> boilTempK);

		double ph = kettlePh != null ? kettlePh.get(PH) : 5.4D;
		double iaaBeer = iaaWort * IAA_LF_BOIL * calcPhIaaFactor(ph);
		double oaaBeer = calcOaaBeerPpm(hop, litres, true, false)
			* calcPhNonIaaFactor(ph)
			* alphaFactor;
		double obaBeer = calcObaBeerPpm(hop, litres, true) * calcPhNonIaaFactor(ph);
		double hopPp = calcHopPolyphenolBeerPpm(hop, litres, false) * calcPhNonIaaFactor(ph);

		double formMult = Equations.getHopFormMultiplier(Hop.Form.LEAF, hop.getForm());

		double ibu = calcPeacockIbuFromBeerPpm(iaaBeer, 0, oaaBeer, obaBeer, hopPp, 0)
			* equipmentUtilisation
			* formMult;

		return new BitternessUnit(ibu, IBU, estimated);
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit calcPostBoilHopIbuSmph(
		HopAddition hop,
		TimeUnit boilTime,
		TimeUnit coolTime,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		TemperatureUnit wortTemp,
		TemperatureUnit ambient,
		double coolingCoefficientPerHour,
		PhUnit kettlePh,
		double elevationFeet,
		double equipmentUtilisation)
	{
		if (coolTime.get(MINUTES) <= 0)
		{
			return new BitternessUnit(0);
		}

		double litres = wortVolume.get(LITRES);
		if (litres <= 0)
		{
			return new BitternessUnit(0);
		}

		boolean estimated = wortGravity.isEstimated() || wortVolume.isEstimated();

		double aa0Ppm = calcHopAaPpm(hop, litres);
		double boilMin = boilTime.get(MINUTES);
		double endMin = boilMin + coolTime.get(MINUTES);
		double boilTempK = calcBoilTempKelvin(elevationFeet);

		double aaAtFlameout = calcRemainingAaPpm(aa0Ppm, boilMin, boilTempK);

		double iaaWort = integrateMalowickiIaaPpm(
			aaAtFlameout,
			boilMin,
			endMin,
			t ->
			{
				double elapsedHours = (t - boilMin) / 60.0D;
				return Equations.calcNewtonianCoolingTemperature(
					wortTemp,
					ambient,
					coolingCoefficientPerHour,
					elapsedHours).get(KELVIN);
			});

		double ph = kettlePh != null ? kettlePh.get(PH) : 5.4D;
		double iaaBeer = iaaWort * IAA_LF_BOIL * calcPhIaaFactor(ph);

		double formMult = Equations.getHopFormMultiplier(Hop.Form.LEAF, hop.getForm());

		double ibu = calcPeacockIbuFromBeerPpm(iaaBeer, 0, 0, 0, 0, 0)
			* equipmentUtilisation
			* formMult;

		return new BitternessUnit(ibu, IBU, estimated);
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit calcDryHopIbuSmph(
		HopAddition hop,
		VolumeUnit beerVolume,
		PhUnit beerPh)
	{
		double litres = beerVolume.get(LITRES);
		if (litres <= 0)
		{
			return new BitternessUnit(0);
		}

		double ph = beerPh != null ? beerPh.get(PH) : 4.3D;
		double phNonIaa = calcPhNonIaaFactor(ph);

		double aaBeer = calcDryHopAaBeerPpm(hop, litres);
		double oaaBeer = calcOaaBeerPpm(hop, litres, false, true) * phNonIaa;
		double obaBeer = calcObaBeerPpm(hop, litres, false) * phNonIaa;
		double hopPp = calcHopPolyphenolBeerPpm(hop, litres, true) * phNonIaa;

		double ibu = calcPeacockIbuFromBeerPpm(0, aaBeer, oaaBeer, obaBeer, hopPp, 0);

		return new BitternessUnit(ibu, IBU, beerVolume.isEstimated());
	}

	/*-------------------------------------------------------------------------*/

	public static BitternessUnit applyFermentationFactor(BitternessUnit ibu)
	{
		if (ibu == null)
		{
			return new BitternessUnit(0);
		}
		return new BitternessUnit(
			ibu.get(IBU) * FERMENTATION_FACTOR,
			IBU,
			ibu.isEstimated());
	}

	/*-------------------------------------------------------------------------*/

	@FunctionalInterface
	public interface TemperatureCallback
	{
		double getKelvinAtMinute(double minute);
	}
}
