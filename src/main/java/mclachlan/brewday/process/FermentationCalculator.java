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
import java.util.Locale;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Heuristic fermentation phase calculations for {@link Ferment}.
 * <p>
 * References: White &amp; Zainasheff — <em>Yeast</em>; Briggs et al. —
 * <em>Brewing Science and Practice</em>; Kunze — <em>Technology Brewing and
 * Malting</em>.
 */
public final class FermentationCalculator
{
	// EMPIRICAL: power-law bias toward dominant viable populations
	private static final double CULTURE_DOMINANCE_EXPONENT = 1.2D;

	// EMPIRICAL:
//
// Fermentation phases should approach completion relatively quickly
// under healthy brewing conditions.
//
// Typical ale fermentation:
//   ~70-85% complete within 3-5 days
//   ~90-97% complete within 7-10 days
//
// We intentionally use an asymptotic curve so that:
// - short phases can be intentionally incomplete
// - chained fermentation phases remain meaningful
// - very long phases provide diminishing returns
//
// References:
// - White & Zainasheff — Yeast
// - commercial ale fermentation practice
//
// 7d  ≈ 97%
// 14d ≈ ~100%
//
// y = 1 - e^(-k*x)
	private static final double DURATION_RATE_PER_DAY = 0.50D;

	// HEURISTIC: White & Zainasheff — dry yeast ≈ 20 billion cells per gram
	private static final long DRY_YEAST_CELLS_PER_GRAM = 20_000_000_000L;

	// EMPIRICAL: ~100 billion cells per commercial liquid unit (White & Zainasheff)
	private static final long LIQUID_PACKAGE_CELLS = 100_000_000_000L;

	// HEURISTIC: Wyeast/Omega-style pouch; FUTURE: manufacturer-specific package size
	private static final double LIQUID_REFERENCE_PACKAGE_ML = 125D;

	// EMPIRICAL: cap multi-pack/starter estimates
	private static final int LIQUID_MAX_PACKAGE_EQUIVALENTS = 4;

	// EMPIRICAL: residual extract buffer on FG floor (~1 % of OG points)
	private static final double RESIDUAL_EXTRACT_BUFFER_FRAC = 0.01D;

	// EMPIRICAL: base viability decay rate per day (tuned)
	private static final double VIABILITY_DECAY_BASE = 0.03D;

	private static final double DEFAULT_STRAIN_ATTENUATION = 0.75D;
	private static final double DEFAULT_WORT_FERMENTABILITY = 0.9D;

	private static final double[] TEMP_DISTANCE_C = {0D, 2D, 5D, 10D};
	private static final double[] TEMP_FACTOR = {1D, 0.9D, 0.6D, 0.2D};

	private static final double[] PITCH_RATIO_ANCHORS = {0.25D, 0.5D, 1D, 1.5D};
	private static final double[] PITCH_FACTOR_ANCHORS = {0.5D, 0.75D, 1D, 1.05D};

	private FermentationCalculator()
	{
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Culture state for one fermentation phase, including whether it was pitched
	 * this step.
	 */
	static final class CulturePhaseContext
	{
		private final YeastCulture culture;
		private final boolean newPitchThisStep;

		CulturePhaseContext(YeastCulture culture, boolean newPitchThisStep)
		{
			this.culture = culture;
			this.newPitchThisStep = newPitchThisStep;
		}

		YeastCulture getCulture()
		{
			return culture;
		}

		boolean isNewPitchThisStep()
		{
			return newPitchThisStep;
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Runs culture collection, attenuation estimation, gravity projection, and
	 * evolution for one fermentation phase.
	 * <p>
	 * IMPORTANT:
	 * <p>
	 * Separate:
	 * <p>
	 * 1. attenuation capability -> determines theoretical terminal attenuation /
	 * FG
	 * <p>
	 * 2. fermentation phase progress -> determines how much progress this phase
	 * makes toward terminal FG
	 * <p>
	 * Previous implementations incorrectly conflated these concepts, causing
	 * healthy high-attenuation strains (e.g. US-05) to substantially
	 * underattenuate.
	 */
	public static FermentationResult fermentPhase(
		Volume input,
		List<YeastAddition> stepPitches,
		TemperatureUnit startTemp,
		TemperatureUnit endTemp,
		TimeUnit duration,
		ProcessLog log)
	{
		List<CulturePhaseContext> contexts =
			collectPhaseCultures(input, stepPitches);

		if (contexts.isEmpty())
		{
			return FermentationResult.noFermentation();
		}

		contexts = mergePhaseContexts(contexts);

		List<YeastCulture> cultures =
			culturesFromContexts(contexts);

		for (YeastCulture culture : cultures)
		{
			estimateCellCountIfMissing(culture, log);
			defaultViabilityIfMissing(culture, log);
		}

		logCultures(cultures, log);

		double fermentationDays =
			duration == null ? 0D : duration.get(DAYS);

		TemperatureUnit avgTempC =
			calcAverageTempC(startTemp, endTemp);

		double totalEffectiveCells = 0D;

		for (YeastCulture culture : cultures)
		{
			totalEffectiveCells +=
				calcEffectiveCells(culture);
		}

		if (totalEffectiveCells <= 0D)
		{
			if (input.getType() == Volume.Type.WORT)
			{
				log.addError(StringUtils.getProcessString("ferment.no.viable.yeast"));
			}
			else
			{
				log.addWarning(StringUtils.getProcessString("ferment.no.viable.yeast"));
			}

			return FermentationResult.noFermentation();
		}

		double volumeMl =
			input.getVolume() == null
				? 0D
				: input.getVolume().get(MILLILITRES);

		double plato =
			input.getGravity() == null
				? 0D
				: input.getGravity().get(PLATO);

		double weightedPitchRate =
			calcWeightedPitchRate(cultures, avgTempC);

		double requiredCells =
			calcRequiredCells(
				volumeMl,
				plato,
				cultures,
				avgTempC);

		double pitchRatio =
			requiredCells > 0D
				? totalEffectiveCells / requiredCells
				: 0D;

		logWeightedPitchRate(
			log,
			weightedPitchRate,
			requiredCells,
			pitchRatio);

		//
		// Terminal attenuation capability.
		//
		double blendAttenuation =
			calcBlendAttenuation(
				input,
				cultures,
				avgTempC);

		PercentageUnit wortLimit =
			input.getFermentability();

		if (wortLimit != null)
		{
			blendAttenuation =
				Math.min(
					blendAttenuation,
					wortLimit.get(PERCENTAGE));
		}

		blendAttenuation =
			clamp(blendAttenuation, 0D, 1D);

		//
		// Fermentation kinetics / phase progress.
		//
		double pitchFactor =
			calcPitchFactor(pitchRatio);

		double durationFactor =
			calcDurationFactor(fermentationDays);

		//
		// IMPORTANT:
		//
		// Phase progress determines HOW MUCH of remaining
		// attenuation opportunity is consumed this phase.
		//
		// It is NOT attenuation capability.
		//
		double phaseProgress =
			durationFactor * lerp(0.9, 1.0, pitchFactor);

		phaseProgress =
			clamp(phaseProgress, 0D, 1D);

		DensityUnit og =
			input.getOriginalGravity() != null
				? input.getOriginalGravity()
				: input.getGravity();

		DensityUnit current =
			input.getGravity();

		DensityUnit estimatedFg =
			calcStepFg(
				og,
				current,
				blendAttenuation,
				phaseProgress,
				wortLimit);

		logAttenuation(
			log,
			blendAttenuation,
			phaseProgress,
			pitchRatio,
			pitchFactor,
			durationFactor,
			estimatedFg);

		double stepProgress =
			calcStepProgress(
				current,
				og,
				blendAttenuation,
				phaseProgress);

		double abvFraction =
			input.getAbv() == null
				? 0D
				: input.getAbv().get(PERCENTAGE);

		List<YeastCulture> evolved =
			evolveCultures(
				contexts,
				fermentationDays,
				avgTempC,
				stepProgress,
				abvFraction,
				log);

		TemperatureUnit averageTemp =
			calcAverageTempC(startTemp, endTemp);

		return new FermentationResult(
			true,
			blendAttenuation,
			phaseProgress,
			estimatedFg,
			pitchRatio,
			durationFactor,
			pitchFactor,
			averageTemp,
			evolved);
	}

	/*-------------------------------------------------------------------------*/
	static List<CulturePhaseContext> collectPhaseCultures(
		Volume input,
		List<YeastAddition> stepPitches)
	{
		List<CulturePhaseContext> result = new ArrayList<>();

		for (YeastCulture culture : input.getYeastCultures())
		{
			result.add(new CulturePhaseContext((YeastCulture)culture.clone(), false));
		}

		if (stepPitches != null)
		{
			for (YeastAddition pitch : stepPitches)
			{
				result.add(new CulturePhaseContext(YeastCulture.fromPitch(pitch), true));
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	static List<YeastCulture> collectActiveCultures(
		Volume input,
		List<YeastAddition> stepPitches)
	{
		return culturesFromContexts(collectPhaseCultures(input, stepPitches));
	}

	/*-------------------------------------------------------------------------*/
	static List<CulturePhaseContext> mergePhaseContexts(
		List<CulturePhaseContext> contexts)
	{
		Map<String, CulturePhaseContext> merged = new LinkedHashMap<>();

		for (CulturePhaseContext ctx : contexts)
		{
			String key = cultureKey(ctx.getCulture());
			CulturePhaseContext existing = merged.get(key);
			if (existing == null)
			{
				merged.put(key, ctx);
			}
			else
			{
				mergePair(existing.getCulture(), ctx.getCulture());
				boolean newPitch = existing.isNewPitchThisStep() || ctx.isNewPitchThisStep();
				merged.put(key, new CulturePhaseContext(existing.getCulture(), newPitch));
			}
		}

		return new ArrayList<>(merged.values());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * EMPIRICAL: scales estimated cell count after a propagation starter phase (estimate only).
	 */
	static List<YeastCulture> applyStarterCellGrowthHeuristic(
		List<YeastCulture> cultures,
		double phaseProgress,
		ProcessLog log)
	{
		if (cultures == null || cultures.isEmpty())
		{
			return cultures == null ? Collections.emptyList() : cultures;
		}

		double progress = clamp(phaseProgress, 0D, 1D);
		double growthFactor = 1D + 3D * progress;

		List<YeastCulture> result = new ArrayList<>();
		for (YeastCulture culture : cultures)
		{
			YeastCulture next = (YeastCulture)culture.clone();
			long cells = next.getCellCount();
			if (cells > 0L)
			{
				long scaled = (long)(cells * growthFactor);
				next.setCellCount(Math.max(cells, scaled));
				log.addMessage(StringUtils.getProcessString(
					"ferment.log.starter.cell.growth",
					next.getYeast() == null ? "?" : next.getYeast().getName(),
					formatCells(cells),
					formatCells(next.getCellCount())));
			}
			result.add(next);
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	static List<YeastCulture> mergeCultures(List<YeastCulture> cultures)
	{
		List<CulturePhaseContext> contexts = new ArrayList<>();
		for (YeastCulture culture : cultures)
		{
			contexts.add(new CulturePhaseContext(culture, false));
		}
		return culturesFromContexts(mergePhaseContexts(contexts));
	}

	/*-------------------------------------------------------------------------*/
	static List<YeastCulture> culturesFromContexts(
		List<CulturePhaseContext> contexts)
	{
		List<YeastCulture> result = new ArrayList<>();
		for (CulturePhaseContext ctx : contexts)
		{
			result.add(ctx.getCulture());
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	static void mergePair(YeastCulture target, YeastCulture other)
	{
		long cellsA = target.getCellCount();
		long cellsB = other.getCellCount();
		long totalCells = cellsA + cellsB;

		target.setCellCount(totalCells);
		target.setGeneration(Math.max(target.getGeneration(), other.getGeneration()));

		double viabA = viabilityFraction(target.getViability());
		double viabB = viabilityFraction(other.getViability());
		if (totalCells > 0L)
		{
			double weighted = (cellsA * viabA + cellsB * viabB) / totalCells;
			target.setViability(new PercentageUnit(weighted, true));
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Result of liquid cell estimation for logging severity only; cell count
	 * values are unchanged from prior heuristics.
	 */
	static final class LiquidCellEstimate
	{
		final long cells;
		final boolean lowConfidence;
		final String warningKey;
		final double pitchMl;

		LiquidCellEstimate(long cells, boolean lowConfidence, String warningKey,
			double pitchMl)
		{
			this.cells = cells;
			this.lowConfidence = lowConfidence;
			this.warningKey = warningKey;
			this.pitchMl = pitchMl;
		}

		static LiquidCellEstimate normal(long cells, double pitchMl)
		{
			return new LiquidCellEstimate(cells, false, null, pitchMl);
		}

		static LiquidCellEstimate lowConfidence(long cells, String warningKey,
			double pitchMl)
		{
			return new LiquidCellEstimate(cells, true, warningKey, pitchMl);
		}
	}

	/*-------------------------------------------------------------------------*/
	static void estimateCellCountIfMissing(YeastCulture culture, ProcessLog log)
	{
		if (culture.getCellCount() > 0L)
		{
			return;
		}

		Yeast yeast = culture.getYeast();
		if (yeast == null)
		{
			return;
		}

		String yeastName = yeast.getName();
		long estimated = 0L;
		Yeast.Form form = yeast.getForm();

		if (form == Yeast.Form.DRY)
		{
			double grams;
			try
			{
				grams = culture.getQuantity().get(GRAMS);
			}
			catch (BrewdayException e)
			{
				estimated = conservativeDryCellFallback();
				culture.setCellCount(estimated);
				logCellEstimateWarning(
					log,
					"ferment.log.cells.estimate.quantity.missing",
					yeastName,
					estimated);
				return;
			}

			if (grams <= 0D)
			{
				estimated = conservativeDryCellFallback();
				culture.setCellCount(estimated);
				logCellEstimateWarning(
					log,
					"ferment.log.cells.estimate.quantity.zero",
					yeastName,
					estimated);
			}
			else
			{
				estimated = (long)(grams * DRY_YEAST_CELLS_PER_GRAM);
				culture.setCellCount(estimated);
				logDryYeastCellEstimate(log, yeastName, estimated);
			}
			return;
		}

		if (form == Yeast.Form.LIQUID || form == Yeast.Form.CULTURE)
		{
			LiquidCellEstimate result = estimateLiquidPitchCells(culture, log);
			if (result.cells > 0L)
			{
				culture.setCellCount(result.cells);
				if (result.lowConfidence)
				{
					logCellEstimateWarning(log, result.warningKey, yeastName, result.cells);
				}
				else
				{
					logLiquidYeastCellEstimate(log, yeastName, result.cells, result.pitchMl);
				}
			}
			else
			{
				log.addWarning(StringUtils.getProcessString(
					"ferment.log.cells.estimate.failed",
					yeastName));
			}
			return;
		}

		if (form == Yeast.Form.SLANT)
		{
			estimated = LIQUID_PACKAGE_CELLS / 2;
			culture.setCellCount(estimated);
			logCellEstimateWarning(
				log,
				"ferment.log.cells.estimate.slant",
				yeastName,
				estimated);
			return;
		}

		log.addWarning(StringUtils.getProcessString(
			"ferment.log.cells.estimate.failed",
			yeastName));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * HEURISTIC: White &amp; Zainasheff — ~100B cells per commercial liquid
	 * package, scaled by volume.
	 */
	static long conservativeDryCellFallback()
	{
		// EMPIRICAL: half of a typical 11 g dry pitch
		return (long)(5.5D * DRY_YEAST_CELLS_PER_GRAM);
	}

	/*-------------------------------------------------------------------------*/
	static LiquidCellEstimate estimateLiquidPitchCells(YeastCulture culture,
		ProcessLog log)
	{
		double pitchMl;
		try
		{
			pitchMl = culture.getQuantity().get(MILLILITRES);
		}
		catch (BrewdayException e)
		{
			return LiquidCellEstimate.lowConfidence(
				LIQUID_PACKAGE_CELLS,
				"ferment.log.cells.estimate.liquid.quantity",
				0D);
		}

		if (pitchMl <= 0D)
		{
			return LiquidCellEstimate.lowConfidence(
				LIQUID_PACKAGE_CELLS / 2,
				"ferment.log.cells.estimate.quantity.zero",
				pitchMl);
		}

		double fraction = pitchMl / LIQUID_REFERENCE_PACKAGE_ML;
		long cells = (long)(LIQUID_PACKAGE_CELLS * fraction);
		cells = Math.min(cells, LIQUID_PACKAGE_CELLS * LIQUID_MAX_PACKAGE_EQUIVALENTS);

		if (pitchMl < 5D)
		{
			cells = Math.min(cells, (long)(LIQUID_PACKAGE_CELLS * 0.2D));
		}

		return LiquidCellEstimate.normal(cells, pitchMl);
	}

	/*-------------------------------------------------------------------------*/
	static String formatCells(long cells)
	{
		if (cells < 0L)
		{
			cells = 0L;
		}

		if (cells >= 1_000_000_000_000L)
		{
			return formatCompact(cells / 1_000_000_000_000D, "T");
		}
		if (cells >= 1_000_000_000L)
		{
			return formatCompact(cells / 1_000_000_000D, "B");
		}
		if (cells >= 1_000_000L)
		{
			return formatCompact(cells / 1_000_000D, "M");
		}

		return Long.toString(cells);
	}

	/*-------------------------------------------------------------------------*/
	static String formatCompact(double scaled, String suffix)
	{
		if (scaled >= 10D)
		{
			return String.format(Locale.ROOT, "%.0f%s", scaled, suffix);
		}
		return String.format(Locale.ROOT, "%.1f%s", scaled, suffix);
	}

	/*-------------------------------------------------------------------------*/
	static void logDryYeastCellEstimate(ProcessLog log, String yeastName,
		long estimated)
	{
		log.addMessage(StringUtils.getProcessString(
			"ferment.log.cells.estimated.dry",
			yeastName,
			formatCells(estimated),
			formatCells(DRY_YEAST_CELLS_PER_GRAM)));
	}

	/*-------------------------------------------------------------------------*/
	static void logLiquidYeastCellEstimate(
		ProcessLog log,
		String yeastName,
		long estimated,
		double pitchMl)
	{
		double refMl = LIQUID_REFERENCE_PACKAGE_ML;
		boolean fullPackage = Math.abs(pitchMl - refMl) <= 2D;

		if (fullPackage)
		{
			log.addMessage(StringUtils.getProcessString(
				"ferment.log.cells.estimated.liquid.package",
				yeastName,
				formatCells(estimated)));
		}
		else
		{
			String pitchMlText = formatPitchMl(pitchMl);
			String refMlText = formatPitchMl(refMl);
			log.addMessage(StringUtils.getProcessString(
				"ferment.log.cells.estimated.liquid.scaled",
				yeastName,
				formatCells(estimated),
				pitchMlText,
				refMlText));
		}
	}

	/*-------------------------------------------------------------------------*/
	static String formatPitchMl(double pitchMl)
	{
		if (pitchMl == Math.rint(pitchMl))
		{
			return String.format(Locale.ROOT, "%.0f", pitchMl);
		}
		return String.format(Locale.ROOT, "%.1f", pitchMl);
	}

	/*-------------------------------------------------------------------------*/
	static void logCellEstimateWarning(
		ProcessLog log,
		String messageKey,
		String yeastName,
		long fallbackCells)
	{
		log.addWarning(StringUtils.getProcessString(
			messageKey,
			yeastName,
			formatCells(fallbackCells)));
	}

	/*-------------------------------------------------------------------------*/
	static void defaultViabilityIfMissing(YeastCulture culture, ProcessLog log)
	{
		if (culture.getViability() != null)
		{
			return;
		}

		double defaultViab = switch (culture.getSourceType())
		{
			case REPITCHED_SLURRY, HARVESTED, BOTTLE_DREGS -> 0.90D;
			default -> 0.96D;
		};

		culture.setViability(new PercentageUnit(defaultViab, true));
		log.addMessage(StringUtils.getProcessString(
			"ferment.log.viability.defaulted",
			culture.getYeast().getName(),
			defaultViab * 100D));
	}

	/*-------------------------------------------------------------------------*/
	static double calcEffectiveCells(YeastCulture culture)
	{
		return culture.getCellCount() * viabilityFraction(culture.getViability());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Briggs / White &amp; Zainasheff — effective-cells-weighted pitch rate
	 * (million / mL / °Plato).
	 */
	static double calcWeightedPitchRate(List<YeastCulture> cultures,
		TemperatureUnit avgTemp)
	{
		double weightedRate = 0D;
		double totalEffective = 0D;

		for (YeastCulture culture : cultures)
		{
			double effective = calcEffectiveCells(culture);
			double rate = pitchRatePerMlPlato(culture, avgTemp);
			weightedRate += effective * rate;
			totalEffective += effective;
		}

		if (totalEffective <= 0D)
		{
			return pitchRatePerMlPlato(cultures.isEmpty() ? null : cultures.get(0), avgTemp);
		}

		return weightedRate / totalEffective;
	}

	/*-------------------------------------------------------------------------*/
	static double calcRequiredCells(double volumeMl, double plato,
		List<YeastCulture> cultures, TemperatureUnit avgTemp)
	{
		if (volumeMl <= 0D || plato <= 0D || cultures.isEmpty())
		{
			return 0D;
		}

		return volumeMl * plato * calcWeightedPitchRate(cultures, avgTemp);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Returns recommended pitch rate in:
	 * <p>
	 * actual cells / mL / degree Plato
	 * <p>
	 * IMPORTANT: Internally we use absolute cell counts, NOT "millions of cells"
	 * shorthand units.
	 * <p>
	 * References: - White & Zainasheff — Yeast - Briggs — Brewing Science and
	 * Practice
	 * <p>
	 * Standard commercial heuristics:
	 * <p>
	 * Ale: 0.75 million cells / mL / °P
	 * <p>
	 * Lager: 1.5 million cells / mL / °P
	 * <p>
	 * Warm-fermented lagers require less aggressive pitching because yeast
	 * growth is significantly faster at elevated temperatures.
	 */
	static double pitchRatePerMlPlato(
		YeastCulture culture,
		TemperatureUnit avgTemp)
	{
		// absolute cells, not "millions"
		double baseRate;

		switch (culture.getYeast().getType())
		{
			case LAGER:
				baseRate = 1_500_000D;
				break;

			default:
				baseRate = 750_000D;
				break;
		}

		if (avgTemp == null)
		{
			return baseRate;
		}

		double tempC = avgTemp.get(CELSIUS);

		//
		// Warm-fermented lagers need lower pitch rates because
		// growth/reproduction is more active at elevated temps.
		//
		if (culture.getYeast().getType() == Yeast.Type.LAGER)
		{
			// >=20C approaches ale-like pitch requirements
			if (tempC >= 20D)
			{
				baseRate *= 0.5D;
			}
			else if (tempC > 10D)
			{
				double t = (tempC - 10D) / 10D;

				baseRate *= lerp(
					1.0D,
					0.5D,
					t);
			}
		}

		return baseRate;
	}

	/*-------------------------------------------------------------------------*/
	static double calcPitchFactor(double pitchRatio)
	{
		if (pitchRatio <= 0D)
		{
			return PITCH_FACTOR_ANCHORS[0];
		}
		if (pitchRatio >= PITCH_RATIO_ANCHORS[PITCH_RATIO_ANCHORS.length - 1])
		{
			return PITCH_FACTOR_ANCHORS[PITCH_FACTOR_ANCHORS.length - 1];
		}
		return interpolatePiecewise(pitchRatio, PITCH_RATIO_ANCHORS, PITCH_FACTOR_ANCHORS);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates a fermentation-performance temperature factor.
	 * <p>
	 * IMPORTANT: This is intentionally NOT a symmetric bell-curve model.
	 * <p>
	 * Brewing yeast generally behaves as follows:
	 * <p>
	 * - Below preferred range: attenuation and metabolic activity decline
	 * rapidly.
	 * <p>
	 * - Within preferred range: normal attenuation.
	 * <p>
	 * - Slightly above preferred range: attenuation often remains strong or may
	 * slightly increase.
	 * <p>
	 * - Far above preferred range: attenuation eventually declines due to
	 * thermal stress.
	 * <p>
	 * This models fermentation performance ONLY.
	 * <p>
	 * Yeast stress / flavour quality / viability impacts are handled separately
	 * elsewhere in the fermentation model.
	 * <p>
	 * References: - White & Zainasheff — Yeast - Kunze — Technology Brewing and
	 * Malting
	 * <p>
	 * This is intentionally heuristic rather than biochemical.
	 */
	static double calcTemperatureFactor(
		Yeast yeast,
		TemperatureUnit avgTemp)
	{
		if (yeast == null || avgTemp == null)
		{
			return 1D;
		}

		if (yeast.getMinTemp() == null ||
			yeast.getMaxTemp() == null)
		{
			return 1D;
		}

		double temp =
			avgTemp.get(CELSIUS);

		double min =
			yeast.getMinTemp().get(CELSIUS);

		double max =
			yeast.getMaxTemp().get(CELSIUS);

		//
		// Below preferred range:
		// strong attenuation decline
		//
		if (temp < min)
		{
			double delta = min - temp;

			// 0C below -> 1.0
			// 2C below -> 0.9
			// 5C below -> 0.6
			// 10C below -> 0.2

			if (delta >= 10D)
			{
				return 0.2;
			}
			else if (delta >= 5D)
			{
				return lerp(
					0.6,
					0.2,
					(delta - 5D) / 5D);
			}
			else if (delta >= 2D)
			{
				return lerp(
					0.9,
					0.6,
					(delta - 2D) / 3D);
			}
			else
			{
				return lerp(
					1.0,
					0.9,
					delta / 2D);
			}
		}

		//
		// Within preferred range:
		// optimal attenuation
		//
		if (temp <= max)
		{
			return 1D;
		}

		//
		// Slightly above preferred range:
		// attenuation may improve slightly
		//
		double delta = temp - max;

		// +0C -> 1.0
		// +3C -> 1.05
		// +5C -> 1.03
		// +10C -> 0.8
		// +15C -> 0.3

		if (delta <= 3D)
		{
			return lerp(
				1.0,
				1.05,
				delta / 3D);
		}
		else if (delta <= 5D)
		{
			return lerp(
				1.05,
				1.03,
				(delta - 3D) / 2D);
		}
		else if (delta <= 10D)
		{
			return lerp(
				1.03,
				0.8,
				(delta - 5D) / 5D);
		}
		else if (delta <= 15D)
		{
			return lerp(
				0.8,
				0.3,
				(delta - 10D) / 5D);
		}
		else
		{
			return 0.3;
		}
	}

	/*-------------------------------------------------------------------------*/
	private static double lerp(
		double a,
		double b,
		double t)
	{
		t = Math.max(0D, Math.min(1D, t));
		return a + (b - a) * t;
	}

	/*-------------------------------------------------------------------------*/
	static double calcDurationFactor(double fermentationDays)
	{
		if (fermentationDays <= 0D)
		{
			return 0D;
		}

		double factor =
			1D - Math.exp(
				-DURATION_RATE_PER_DAY * fermentationDays);

		//
		// Numerical stability:
		// treat near-complete phases as complete.
		//
		if (factor > 0.999D)
		{
			factor = 1D;
		}

		return clamp(factor, 0D, 1D);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the apparent attenuation capability of a yeast strain
	 * for the supplied wort.
	 *
	 * IMPORTANT:
	 *
	 * Wort fermentability is an upper attenuation LIMIT,
	 * not a target attenuation.
	 *
	 * Example:
	 * - a highly fermentable wort allows a highly attenuative
	 *   strain (e.g. US-05) to fully dry the beer
	 * - but does NOT make a low-attenuation English strain
	 *   (e.g. Windsor) behave like Chico yeast
	 *
	 * Therefore:
	 * - yeast attenuation defines the primary attenuation capability
	 * - wort fermentability only caps the maximum achievable attenuation
	 *
	 * References:
	 * - White & Zainasheff — Yeast
	 * - Lallemand Windsor technical specifications
	 * - Fermentis US-05 technical specifications
	 */
	static double calcStrainAttenuation(
		Yeast yeast,
		Volume input)
	{
		double wortLimit =
			DEFAULT_WORT_FERMENTABILITY;

		if (input.getFermentability() != null)
		{
			wortLimit =
				input.getFermentability().get(PERCENTAGE);
		}

		//
		// Base strain attenuation capability.
		//
		double yeastAttenuation =
			DEFAULT_STRAIN_ATTENUATION;

		if (yeast != null &&
			yeast.getAttenuation() != null)
		{
			yeastAttenuation =
				yeast.getAttenuation().get(PERCENTAGE);
		}

		//
		// Wort fermentability is ONLY an upper limit.
		//
		return Math.min(
			yeastAttenuation,
			wortLimit);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates blended apparent attenuation capability for all active
	 * cultures.
	 * <p>
	 * IMPORTANT:
	 * <p>
	 * Wort fermentability is an upper attenuation LIMIT, NOT a target
	 * attenuation.
	 * <p>
	 * Yeast strain character must remain dominant.
	 * <p>
	 * Example: - highly fermentable wort should allow US-05 to fully attenuate -
	 * but should NOT cause Windsor to behave like Chico
	 * <p>
	 * Therefore: - yeast attenuation defines the primary attenuation capability
	 * - wort fermentability only caps the maximum achievable attenuation
	 * <p>
	 * References: - White & Zainasheff — Yeast - Lallemand Windsor technical
	 * sheets - Fermentis US-05 technical sheets
	 */
	static double calcBlendAttenuation(
		Volume input,
		List<YeastCulture> cultures,
		TemperatureUnit avgTempC)
	{
		double weightSum = 0D;
		double weightedAtten = 0D;

		for (YeastCulture culture : cultures)
		{
			double effectiveCells =
				calcEffectiveCells(culture);

			if (effectiveCells <= 0D)
			{
				continue;
			}

			double weight =
				Math.pow(
					effectiveCells,
					CULTURE_DOMINANCE_EXPONENT);

			//
			// Base strain attenuation capability.
			//
			double strainAtten =
				calcStrainAttenuation(
					culture.getYeast(),
					input);

			//
			// Temperature modifies attenuation capability,
			// but should not radically transform strain character.
			//
			double tempFactor =
				calcTemperatureFactor(
					culture.getYeast(),
					avgTempC);

			double cultureAtten =
				strainAtten * tempFactor;

			//
			// Wort fermentability is ONLY an upper limit.
			//
			if (input.getFermentability() != null)
			{
				cultureAtten =
					Math.min(
						cultureAtten,
						input.getFermentability().get(PERCENTAGE));
			}
			else
			{
				cultureAtten =
					Math.min(
						cultureAtten,
						DEFAULT_WORT_FERMENTABILITY);
			}

			cultureAtten =
				clamp(cultureAtten, 0D, 1D);

			weightedAtten +=
				weight * cultureAtten;

			weightSum += weight;
		}

		if (weightSum <= 0D)
		{
			return 0D;
		}

		return clamp(
			weightedAtten / weightSum,
			0D,
			1D);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the resulting FG for a fermentation phase.
	 * <p>
	 * IMPORTANT:
	 * <p>
	 * Separate:
	 * <p>
	 * 1. attenuation capability -> determines theoretical terminal FG
	 * <p>
	 * 2. fermentation phase progress -> determines how much of remaining
	 * attenuation occurs during THIS phase
	 * <p>
	 * Previous implementations incorrectly applied attenuation scaling twice,
	 * which caused healthy high-attenuation strains (e.g. US-05) to
	 * underattenuate substantially.
	 * <p>
	 * References: - White & Zainasheff — Yeast - Briggs — Brewing Science and
	 * Practice
	 */
	static DensityUnit calcStepFg(
		DensityUnit og,
		DensityUnit current,
		double blendAttenuation,
		double phaseProgress,
		PercentageUnit wortLimit)
	{
		if (og == null || current == null)
		{
			return null;
		}

		double ogGu =
			og.get(GU);

		double currentGu =
			current.get(GU);

		//
		// Determine attenuation ceiling.
		//
		double maxAttenuation =
			blendAttenuation;

		if (wortLimit != null)
		{
			maxAttenuation =
				Math.min(
					maxAttenuation,
					wortLimit.get(PERCENTAGE));
		}
		else
		{
			maxAttenuation =
				Math.min(
					maxAttenuation,
					DEFAULT_WORT_FERMENTABILITY);
		}

		//
		// Theoretical terminal FG.
		//
		double terminalFgGu =
			ogGu * (1D - maxAttenuation);

		//
		// Residual extract safety buffer.
		//
		terminalFgGu +=
			RESIDUAL_EXTRACT_BUFFER_FRAC * ogGu;

		//
		// Remaining attenuation opportunity from CURRENT state.
		//
		double remainingDeltaGu =
			currentGu - terminalFgGu;

		//
		// Already effectively terminal.
		//
		if (remainingDeltaGu <= 0D)
		{
			return new DensityUnit(
				currentGu,
				GU,
				true);
		}

		//
		// Apply ONLY phase progress kinetics here.
		//
		// IMPORTANT:
		// Do NOT re-apply attenuation capability scaling.
		//
		double attenuationThisPhaseGu =
			remainingDeltaGu * phaseProgress;

		double stepFgGu =
			currentGu - attenuationThisPhaseGu;

		//
		// Fermentation must be monotonic.
		//
		stepFgGu =
			Math.min(stepFgGu, currentGu);

		//
		// Clamp against terminal FG.
		//
		stepFgGu =
			Math.max(stepFgGu, terminalFgGu);

		//
		// Numerical sanity.
		//
		stepFgGu =
			Math.max(0D, stepFgGu);

		return new DensityUnit(
			stepFgGu,
			GU,
			true);
	}

	/*-------------------------------------------------------------------------*/
	static double calcStepProgress(
		DensityUnit current,
		DensityUnit og,
		double blendAttenuation,
		double effectiveAttenuation)
	{
		if (og == null || current == null || blendAttenuation <= 0D)
		{
			return effectiveAttenuation;
		}

		double ogGu = og.get(GU);
		double currentGu = current.get(GU);
		double targetFgGu = ogGu * (1D - blendAttenuation);
		double totalDrop = currentGu - targetFgGu;
		if (totalDrop <= 0D)
		{
			return 1D;
		}

		double stepDrop = effectiveAttenuation * totalDrop;
		return clamp(stepDrop / totalDrop, 0D, 1D);
	}

	/*-------------------------------------------------------------------------*/
	static boolean shouldIncrementGeneration(YeastCulture culture,
		boolean isNewPitch)
	{
		if (!isNewPitch)
		{
			return false;
		}

		return switch (culture.getSourceType())
		{
			case REPITCHED_SLURRY, HARVESTED, BOTTLE_DREGS -> true;
			default -> culture.getGeneration() == 0;
		};
	}

	/*-------------------------------------------------------------------------*/
	static List<YeastCulture> evolveCultures(
		List<CulturePhaseContext> contexts,
		double fermentationDays,
		TemperatureUnit avgTempC,
		double stepProgress,
		double abvFraction,
		ProcessLog log)
	{
		List<YeastCulture> evolved = new ArrayList<>();

		for (CulturePhaseContext ctx : contexts)
		{
			YeastCulture culture = ctx.getCulture();
			YeastCulture next = (YeastCulture)culture.clone();
			Yeast yeast = next.getYeast();

			if (shouldIncrementGeneration(culture, ctx.isNewPitchThisStep()))
			{
				next.setGeneration(next.getGeneration() + 1);
			}
			else if (!ctx.isNewPitchThisStep())
			{
				log.addMessage(StringUtils.getProcessString(
					"ferment.log.generation.unchanged",
					yeast.getName(),
					next.getGeneration()));
			}

			double tempFactor = calcTemperatureFactor(yeast, avgTempC);
			double decayRate = calcViabilityDecayRate(avgTempC, tempFactor, abvFraction, yeast);

			double viab = viabilityFraction(next.getViability());
			viab *= Math.exp(-decayRate * fermentationDays);

			YeastActivityState newState = resolveActivityState(
				next,
				avgTempC,
				fermentationDays,
				stepProgress,
				tempFactor < 1D,
				isAbvStressed(yeast, abvFraction));

			viab *= activityStateViabilityMultiplier(newState);

			next.setViability(new PercentageUnit(clamp(viab, 0D, 1D), true));
			next.setActivityState(newState);
			evolved.add(next);

			YeastActivityState oldState = culture.getActivityState();
			if (oldState != newState)
			{
				log.addMessage(StringUtils.getProcessString(
					"ferment.log.state.transition",
					yeast.getName(),
					oldState,
					newState));
			}

			log.addMessage(StringUtils.getProcessString(
				"ferment.log.viability.decay",
				yeast.getName(),
				viabilityFraction(next.getViability()) * 100D,
				next.getGeneration()));
		}

		return evolved;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * EMPIRICAL: Kunze / White — temperature and alcohol increase effective
	 * decay rate.
	 */
	static double calcViabilityDecayRate(
		TemperatureUnit avgTempC,
		double tempFactor,
		double abvFraction,
		Yeast yeast)
	{
		double k = VIABILITY_DECAY_BASE;
		k *= tempDecayMultiplier(avgTempC);

		if (tempFactor < 1D)
		{
			k *= (2D - tempFactor);
		}

		if (isAbvStressed(yeast, abvFraction))
		{
			double excess = abvFraction - abvStressThreshold(yeast);
			k *= 1D + 4D * excess;
		}

		return k;
	}

	/*-------------------------------------------------------------------------*/
	static double tempDecayMultiplier(TemperatureUnit avgTempC)
	{
		if (avgTempC.get(CELSIUS) <= 4D)
		{
			return 0.25D;
		}
		if (avgTempC.get(CELSIUS) <= 12D)
		{
			return 0.5D;
		}
		if (avgTempC.get(CELSIUS) >= 24D)
		{
			return 1.3D;
		}
		if (avgTempC.get(CELSIUS) >= 20D)
		{
			return 1.1D;
		}
		return 1D;
	}

	/*-------------------------------------------------------------------------*/
	static double activityStateViabilityMultiplier(YeastActivityState state)
	{
		return switch (state)
		{
			case DORMANT -> 0.99D;
			case FLOCCULATED -> 0.97D;
			case STRESSED -> 0.90D;
			case EXHAUSTED -> 0.85D;
			default -> 1D;
		};
	}

	/*-------------------------------------------------------------------------*/
	static YeastActivityState resolveActivityState(
		YeastCulture culture,
		TemperatureUnit avgTempC,
		double fermentationDays,
		double stepProgress,
		boolean tempStressed,
		boolean abvStressed)
	{
		double viab =
			viabilityFraction(culture.getViability());

		if (viab < 0.10D)
		{
			return YeastActivityState.EXHAUSTED;
		}

		double tempC =
			avgTempC.get(CELSIUS);

		//
		// Near-terminal cold phases are typically:
		// - flocculation
		// - dormancy
		// NOT stress.
		//
		boolean nearTerminal =
			stepProgress < 0.10D;

		boolean coldPhase =
			tempC <= 10D;

		//
		// Long cold conditioning/lagering.
		//
		if (coldPhase && fermentationDays >= 14D)
		{
			return YeastActivityState.DORMANT;
		}

		//
		// Cold crash / post-fermentation settling.
		//
		if (coldPhase && nearTerminal)
		{
			return YeastActivityState.FLOCCULATED;
		}

		//
		// Stress should primarily represent:
		// - warm thermal abuse
		// - alcohol stress
		// during active fermentation.
		//
		boolean activelyFermenting =
			stepProgress >= 0.10D;

		if (activelyFermenting &&
			(tempStressed || abvStressed))
		{
			return YeastActivityState.STRESSED;
		}

		//
		// Fermentation substantially complete.
		//
		if (stepProgress >= 0.90D)
		{
			return YeastActivityState.FLOCCULATED;
		}

		return YeastActivityState.ACTIVE;
	}

	/*-------------------------------------------------------------------------*/
	static boolean isAbvStressed(Yeast yeast, double abvFraction)
	{
		return abvFraction > abvStressThreshold(yeast);
	}

	/*-------------------------------------------------------------------------*/
	static double abvStressThreshold(Yeast yeast)
	{
		return switch (yeast == null ? null : yeast.getType())
		{
			case LAGER -> 0.10D;
			case WINE, CHAMPAGNE -> 0.12D;
			case null -> 0.08D;
			default -> 0.08D;
		};
	}

	/*-------------------------------------------------------------------------*/
	static TemperatureUnit calcAverageTempC(TemperatureUnit startTemp,
		TemperatureUnit endTemp)
	{
		double startC = startTemp == null ? 20D : startTemp.get(CELSIUS);
		double endC = endTemp == null ? startC : endTemp.get(CELSIUS);
		return new TemperatureUnit((startC + endC) / 2D, CELSIUS, true);
	}

	/*-------------------------------------------------------------------------*/
	static double interpolatePiecewise(double x, double[] xs, double[] ys)
	{
		if (x <= xs[0])
		{
			return ys[0];
		}

		for (int i = 1; i < xs.length; i++)
		{
			if (x <= xs[i])
			{
				double t = (x - xs[i - 1]) / (xs[i] - xs[i - 1]);
				return ys[i - 1] + t * (ys[i] - ys[i - 1]);
			}
		}

		return ys[ys.length - 1];
	}

	/*-------------------------------------------------------------------------*/
	static double clamp(double value, double min, double max)
	{
		return Math.max(min, Math.min(max, value));
	}

	/*-------------------------------------------------------------------------*/
	static double viabilityFraction(PercentageUnit viability)
	{
		return viability == null ? 1D : viability.get(PERCENTAGE);
	}

	/*-------------------------------------------------------------------------*/
	static String cultureKey(YeastCulture culture)
	{
		String yeastName = culture.getYeast() == null ? "" : culture.getYeast().getName();
		YeastSourceType source = culture.getSourceType() == null
			? YeastSourceType.DIRECT_PITCH
			: culture.getSourceType();
		return yeastName + "|" + source.name();
	}

	/*-------------------------------------------------------------------------*/
	static void logCultures(List<YeastCulture> cultures, ProcessLog log)
	{
		for (YeastCulture culture : cultures)
		{
			log.addMessage(StringUtils.getProcessString(
				"ferment.log.culture",
				culture.getYeast().getName(),
				formatCells(culture.getCellCount()),
				viabilityFraction(culture.getViability()) * 100D,
				culture.getGeneration(),
				culture.getSourceType(),
				culture.getActivityState()));
		}
	}

	/*-------------------------------------------------------------------------*/
	static void logWeightedPitchRate(
		ProcessLog log,
		double weightedRate,
		double requiredCells,
		double pitchRatio)
	{
		log.addMessage(StringUtils.getProcessString(
			"ferment.log.pitch.weightedRate",
			weightedRate / 1e6D,
			requiredCells,
			pitchRatio));
	}

	/*-------------------------------------------------------------------------*/
	static void logAttenuation(
		ProcessLog log,
		double blendAttenuation,
		double effectiveAttenuation,
		double pitchRatio,
		double pitchFactor,
		double durationFactor,
		DensityUnit estimatedFg)
	{
		double fgSg = estimatedFg == null
			? Double.NaN
			: estimatedFg.get(SPECIFIC_GRAVITY);

		log.addMessage(StringUtils.getProcessString(
			"ferment.log.attenuation",
			blendAttenuation * 100D,
			effectiveAttenuation * 100D,
			pitchRatio,
			pitchFactor,
			durationFactor,
			fgSg));
	}
}
