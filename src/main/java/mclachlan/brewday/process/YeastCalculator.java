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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Locale;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Yeast cell-count estimation, viability heuristics, and pitch-rate calculations.
 * <p>
 * Shared by {@link FermentationCalculator} and the Tools yeast calculator UI.
 * References: White &amp; Zainasheff — <em>Yeast</em>; Briggs et al.
 */
public final class YeastCalculator
{
	/** HEURISTIC: White &amp; Zainasheff — dry yeast ≈ 20 billion cells per gram. */
	public static final long DRY_YEAST_CELLS_PER_GRAM = 20_000_000_000L;

	/** EMPIRICAL: ~100 billion cells per commercial liquid unit. */
	public static final long LIQUID_PACKAGE_CELLS = 100_000_000_000L;

	/** HEURISTIC: Wyeast/Omega-style pouch reference size. */
	public static final double LIQUID_REFERENCE_PACKAGE_ML = 125D;

	public static final int LIQUID_MAX_PACKAGE_EQUIVALENTS = 4;

	/**
	 * EMPIRICAL: conservative repitch/harvest slurry density when not measured
	 * (cells per mL).
	 */
	public static final double SLURRY_DEFAULT_CELLS_PER_ML = 1_000_000_000D;

	// EMPIRICAL: monthly viability loss at cold storage (fraction per month)
	private static final double DRY_VIABILITY_LOSS_PER_MONTH_COLD = 0.04D;
	private static final double LIQUID_VIABILITY_LOSS_PER_MONTH_COLD = 0.18D;
	private static final double SLANT_VIABILITY_LOSS_PER_MONTH_COLD = 0.12D;
	private static final double CULTURE_VIABILITY_LOSS_PER_MONTH_COLD = 0.15D;

	private YeastCalculator()
	{
	}

	/*-------------------------------------------------------------------------*/
	public enum WarningSeverity
	{
		INFO,
		WARNING,
		ERROR
	}

	/*-------------------------------------------------------------------------*/
	public enum Status
	{
		OK,
		MISSING_DATA,
		LOW_CONFIDENCE
	}

	/*-------------------------------------------------------------------------*/
	public enum CellCountMode
	{
		ESTIMATE_FROM_QUANTITY,
		MANUAL_TOTAL,
		SLURRY_DENSITY
	}

	/*-------------------------------------------------------------------------*/
	public enum ViabilityMode
	{
		MANUAL,
		DEFAULT_BY_SOURCE,
		FROM_PACKAGE_AGE
	}

	/*-------------------------------------------------------------------------*/
	public enum StorageTemperature
	{
		COLD_4C(0.75D),
		FRIDGE_10C(1.0D),
		ROOM_20C(2.5D);

		private final double lossMultiplier;

		StorageTemperature(double lossMultiplier)
		{
			this.lossMultiplier = lossMultiplier;
		}

		public double getLossMultiplier()
		{
			return lossMultiplier;
		}
	}

	/*-------------------------------------------------------------------------*/
	public record Warning(
		WarningSeverity severity,
		String messageKey,
		Object[] args)
	{
	}

	/*-------------------------------------------------------------------------*/
	public record CellEstimate(
		long cells,
		boolean lowConfidence,
		String warningKey,
		double pitchMl)
	{
		public static CellEstimate normal(long cells, double pitchMl)
		{
			return new CellEstimate(cells, false, null, pitchMl);
		}

		public static CellEstimate lowConfidence(long cells, String warningKey,
			double pitchMl)
		{
			return new CellEstimate(cells, true, warningKey, pitchMl);
		}
	}

	/*-------------------------------------------------------------------------*/
	public record PitchInput(
		Yeast yeast,
		Quantity pitchQuantity,
		Quantity.Unit pitchUnit,
		YeastSourceType sourceType,
		CellCountMode cellCountMode,
		Long manualCellCount,
		Double slurryCellsPerMl,
		ViabilityMode viabilityMode,
		PercentageUnit manualViability,
		LocalDate productionDate,
		LocalDate pitchDate,
		StorageTemperature storageTemperature,
		double wortVolumeLitres,
		double plato,
		TemperatureUnit fermentationTemp)
	{
	}

	/*-------------------------------------------------------------------------*/
	public record Result(
		Status status,
		long cellCount,
		double viabilityFraction,
		double effectiveCells,
		double requiredCells,
		double pitchRatio,
		double weightedPitchRatePerMlPlato,
		double recommendedDryGrams,
		double recommendedLiquidMl,
		List<Warning> warnings)
	{
		public Result
		{
			warnings = warnings == null ? List.of() : List.copyOf(warnings);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static Result calculate(PitchInput input)
	{
		List<Warning> warnings = new ArrayList<>();
		Status status = Status.OK;

		if (input == null || input.yeast() == null)
		{
			return new Result(
				Status.MISSING_DATA,
				0L,
				0D,
				0D,
				0D,
				0D,
				0D,
				0D,
				0D,
				List.of(new Warning(
					WarningSeverity.ERROR,
					"tools.yeast.calculator.error.no.yeast",
					null)));
		}

		YeastCulture culture = buildCultureFromInput(input);
		long cells = resolveCellCount(input, culture, warnings);
		culture.setCellCount(cells);

		if (cells <= 0L)
		{
			status = Status.MISSING_DATA;
			warnings.add(new Warning(
				WarningSeverity.ERROR,
				"ferment.log.cells.estimate.failed",
				new Object[] {input.yeast().getName()}));
		}

		PercentageUnit viability = resolveViability(input, warnings);
		culture.setViability(viability);
		double viabFrac = viabilityFraction(viability);

		double volumeMl = input.wortVolumeLitres() * 1000D;
		double plato = input.plato();
		List<YeastCulture> cultures = List.of(culture);

		double required = calcRequiredCells(volumeMl, plato, cultures, input.fermentationTemp());
		double effective = calcEffectiveCells(culture);
		double pitchRatio = required > 0D ? effective / required : 0D;
		double rate = calcWeightedPitchRate(cultures, input.fermentationTemp());

		if (warnings.stream().anyMatch(w -> w.severity() == WarningSeverity.WARNING))
		{
			status = status == Status.MISSING_DATA ? status : Status.LOW_CONFIDENCE;
		}

		double recDryG = 0D;
		double recLiquidMl = 0D;
		if (pitchRatio > 0D && pitchRatio < 1D && cells > 0L)
		{
			double scale = 1D / pitchRatio;
			Yeast.Form form = input.yeast().getForm();
			if (form == Yeast.Form.DRY)
			{
				try
				{
					recDryG = input.pitchQuantity().get(GRAMS) * scale;
				}
				catch (BrewdayException ignored)
				{
					recDryG = (cells * scale) / (double)DRY_YEAST_CELLS_PER_GRAM;
				}
			}
			else if (form == Yeast.Form.LIQUID || form == Yeast.Form.CULTURE)
			{
				try
				{
					recLiquidMl = input.pitchQuantity().get(MILLILITRES) * scale;
				}
				catch (BrewdayException ignored)
				{
					recLiquidMl = LIQUID_REFERENCE_PACKAGE_ML * scale;
				}
			}
		}

		return new Result(
			status,
			cells,
			viabFrac,
			effective,
			required,
			pitchRatio,
			rate,
			recDryG,
			recLiquidMl,
			warnings);
	}

	/*-------------------------------------------------------------------------*/
	public static void estimateCellCountIfMissing(YeastCulture culture, ProcessLog log)
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
		CellEstimate estimate = estimateCellsForCulture(culture);

		if (estimate.cells() > 0L)
		{
			culture.setCellCount(estimate.cells());
			if (estimate.lowConfidence())
			{
				logCellEstimateWarning(
					log,
					estimate.warningKey(),
					yeastName,
					estimate.cells());
			}
			else if (yeast.getForm() == Yeast.Form.DRY)
			{
				logDryYeastCellEstimate(log, yeastName, estimate.cells());
			}
			else if (yeast.getForm() == Yeast.Form.LIQUID
				|| yeast.getForm() == Yeast.Form.CULTURE)
			{
				logLiquidYeastCellEstimate(log, yeastName, estimate.cells(), estimate.pitchMl());
			}
			return;
		}

		log.addWarning(StringUtils.getProcessString(
			"ferment.log.cells.estimate.failed",
			yeastName));
	}

	/*-------------------------------------------------------------------------*/
	public static CellEstimate estimateCellsForCulture(YeastCulture culture)
	{
		Yeast yeast = culture.getYeast();
		if (yeast == null)
		{
			return CellEstimate.normal(0L, 0D);
		}

		Yeast.Form form = yeast.getForm();
		YeastSourceType source = culture.getSourceType() == null
			? YeastSourceType.DIRECT_PITCH
			: culture.getSourceType();

		if (isSlurrySourceType(source)
			&& (form == Yeast.Form.LIQUID || form == Yeast.Form.CULTURE))
		{
			return estimateSlurryPitchCells(culture, SLURRY_DEFAULT_CELLS_PER_ML,
				"ferment.log.cells.estimate.slurry.default");
		}

		if (form == Yeast.Form.DRY)
		{
			double grams;
			try
			{
				grams = culture.getQuantity().get(GRAMS);
			}
			catch (BrewdayException e)
			{
				long cells = conservativeDryCellFallback();
				return CellEstimate.lowConfidence(
					cells,
					"ferment.log.cells.estimate.quantity.missing",
					0D);
			}

			if (grams <= 0D)
			{
				long cells = conservativeDryCellFallback();
				return CellEstimate.lowConfidence(
					cells,
					"ferment.log.cells.estimate.quantity.zero",
					0D);
			}

			return CellEstimate.normal(
				(long)(grams * DRY_YEAST_CELLS_PER_GRAM),
				0D);
		}

		if (form == Yeast.Form.LIQUID || form == Yeast.Form.CULTURE)
		{
			return estimateLiquidPitchCells(culture);
		}

		if (form == Yeast.Form.SLANT)
		{
			long cells = LIQUID_PACKAGE_CELLS / 2;
			return CellEstimate.lowConfidence(
				cells,
				"ferment.log.cells.estimate.slant",
				0D);
		}

		return CellEstimate.normal(0L, 0D);
	}

	/*-------------------------------------------------------------------------*/
	public static CellEstimate estimateSlurryPitchCells(
		YeastCulture culture,
		double cellsPerMl,
		String warningKey)
	{
		double pitchMl;
		try
		{
			pitchMl = culture.getQuantity().get(MILLILITRES);
		}
		catch (BrewdayException e)
		{
			return CellEstimate.lowConfidence(
				(long)LIQUID_PACKAGE_CELLS,
				"ferment.log.cells.estimate.liquid.quantity",
				0D);
		}

		if (pitchMl <= 0D)
		{
			long fallback = (long)(cellsPerMl * 50D);
			return CellEstimate.lowConfidence(
				fallback,
				"ferment.log.cells.estimate.quantity.zero",
				pitchMl);
		}

		long cells = (long)(pitchMl * cellsPerMl);
		return CellEstimate.lowConfidence(cells, warningKey, pitchMl);
	}

	/*-------------------------------------------------------------------------*/
	public static long conservativeDryCellFallback()
	{
		return (long)(5.5D * DRY_YEAST_CELLS_PER_GRAM);
	}

	/*-------------------------------------------------------------------------*/
	public static CellEstimate estimateLiquidPitchCells(YeastCulture culture)
	{
		double pitchMl;
		try
		{
			pitchMl = culture.getQuantity().get(MILLILITRES);
		}
		catch (BrewdayException e)
		{
			return CellEstimate.lowConfidence(
				LIQUID_PACKAGE_CELLS,
				"ferment.log.cells.estimate.liquid.quantity",
				0D);
		}

		if (pitchMl <= 0D)
		{
			return CellEstimate.lowConfidence(
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

		return CellEstimate.normal(cells, pitchMl);
	}

	/*-------------------------------------------------------------------------*/
	public static boolean isSlurrySourceType(YeastSourceType sourceType)
	{
		return sourceType == YeastSourceType.REPITCHED_SLURRY
			|| sourceType == YeastSourceType.HARVESTED;
	}

	/*-------------------------------------------------------------------------*/
	public static double defaultViabilityFraction(YeastSourceType sourceType)
	{
		if (sourceType == null)
		{
			return 0.96D;
		}
		return switch (sourceType)
		{
			case REPITCHED_SLURRY, HARVESTED, BOTTLE_DREGS -> 0.90D;
			default -> 0.96D;
		};
	}

	/*-------------------------------------------------------------------------*/
	public static void defaultViabilityIfMissing(YeastCulture culture, ProcessLog log)
	{
		if (culture.getViability() != null)
		{
			return;
		}

		double defaultViab = defaultViabilityFraction(culture.getSourceType());
		culture.setViability(new PercentageUnit(defaultViab, true));
		log.addVerboseMessage(StringUtils.getProcessString(
			"ferment.log.viability.defaulted",
			culture.getYeast().getName(),
			defaultViab * 100D));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * EMPIRICAL: linear monthly viability loss from package age (White &amp;
	 * Zainasheff — order-of-magnitude storage guidance).
	 */
	public static PercentageUnit estimateViabilityFromAge(
		Yeast.Form form,
		LocalDate productionDate,
		LocalDate pitchDate,
		StorageTemperature storage)
	{
		if (productionDate == null || pitchDate == null || !pitchDate.isAfter(productionDate))
		{
			double fallback = form == Yeast.Form.DRY ? 0.96D : 0.90D;
			return new PercentageUnit(fallback, true);
		}

		long days = ChronoUnit.DAYS.between(productionDate, pitchDate);
		double months = days / 30.4375D;
		double baseLoss = monthlyViabilityLoss(form);
		double storageMult = storage == null
			? StorageTemperature.FRIDGE_10C.getLossMultiplier()
			: storage.getLossMultiplier();
		double loss = months * baseLoss * storageMult;
		double viab = clamp(1D - loss, 0D, 1D);
		return new PercentageUnit(viab, true);
	}

	/*-------------------------------------------------------------------------*/
	static double monthlyViabilityLoss(Yeast.Form form)
	{
		if (form == null)
		{
			return LIQUID_VIABILITY_LOSS_PER_MONTH_COLD;
		}
		return switch (form)
		{
			case DRY -> DRY_VIABILITY_LOSS_PER_MONTH_COLD;
			case SLANT -> SLANT_VIABILITY_LOSS_PER_MONTH_COLD;
			case CULTURE -> CULTURE_VIABILITY_LOSS_PER_MONTH_COLD;
			default -> LIQUID_VIABILITY_LOSS_PER_MONTH_COLD;
		};
	}

	/*-------------------------------------------------------------------------*/
	public static long projectStarterGrowth(long cells, double phaseProgress)
	{
		if (cells <= 0L)
		{
			return cells;
		}
		double progress = clamp(phaseProgress, 0D, 1D);
		double growthFactor = 1D + 3D * progress;
		return Math.max(cells, (long)(cells * growthFactor));
	}

	/*-------------------------------------------------------------------------*/
	public static double calcEffectiveCells(YeastCulture culture)
	{
		return culture.getCellCount() * viabilityFraction(culture.getViability());
	}

	/*-------------------------------------------------------------------------*/
	public static double calcWeightedPitchRate(
		List<YeastCulture> cultures,
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
			return pitchRatePerMlPlato(
				cultures.isEmpty() ? null : cultures.get(0),
				avgTemp);
		}

		return weightedRate / totalEffective;
	}

	/*-------------------------------------------------------------------------*/
	public static double calcRequiredCells(
		double volumeMl,
		double plato,
		List<YeastCulture> cultures,
		TemperatureUnit avgTemp)
	{
		if (volumeMl <= 0D || plato <= 0D || cultures.isEmpty())
		{
			return 0D;
		}

		return volumeMl * plato * calcWeightedPitchRate(cultures, avgTemp);
	}

	/*-------------------------------------------------------------------------*/
	public static double pitchRatePerMlPlato(
		YeastCulture culture,
		TemperatureUnit avgTemp)
	{
		if (culture == null || culture.getYeast() == null)
		{
			return 750_000D;
		}

		double baseRate = culture.getYeast().getType() == Yeast.Type.LAGER
			? 1_500_000D
			: 750_000D;

		if (avgTemp == null)
		{
			return baseRate;
		}

		double tempC = avgTemp.get(CELSIUS);

		if (culture.getYeast().getType() == Yeast.Type.LAGER)
		{
			if (tempC >= 20D)
			{
				baseRate *= 0.5D;
			}
			else if (tempC > 10D)
			{
				double t = (tempC - 10D) / 10D;
				baseRate *= lerp(1.0D, 0.5D, t);
			}
		}

		return baseRate;
	}

	/*-------------------------------------------------------------------------*/
	public static double viabilityFraction(PercentageUnit viability)
	{
		return viability == null ? 1D : viability.get(PERCENTAGE);
	}

	/*-------------------------------------------------------------------------*/
	public static String formatCells(long cells)
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
	public static String formatCompact(double scaled, String suffix)
	{
		if (scaled >= 10D)
		{
			return String.format(Locale.ROOT, "%.0f%s", scaled, suffix);
		}
		return String.format(Locale.ROOT, "%.1f%s", scaled, suffix);
	}

	/*-------------------------------------------------------------------------*/
	static void logDryYeastCellEstimate(ProcessLog log, String yeastName, long estimated)
	{
		log.addVerboseMessage(StringUtils.getProcessString(
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
			log.addVerboseMessage(StringUtils.getProcessString(
				"ferment.log.cells.estimated.liquid.package",
				yeastName,
				formatCells(estimated)));
		}
		else
		{
			String pitchMlText = formatPitchMl(pitchMl);
			String refMlText = formatPitchMl(refMl);
			log.addVerboseMessage(StringUtils.getProcessString(
				"ferment.log.cells.estimated.liquid.scaled",
				yeastName,
				formatCells(estimated),
				pitchMlText,
				refMlText));
		}
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
	static String formatPitchMl(double pitchMl)
	{
		if (pitchMl == Math.rint(pitchMl))
		{
			return String.format(Locale.ROOT, "%.0f", pitchMl);
		}
		return String.format(Locale.ROOT, "%.1f", pitchMl);
	}

	/*-------------------------------------------------------------------------*/
	private static YeastCulture buildCultureFromInput(PitchInput input)
	{
		return new YeastCulture(
			input.yeast(),
			input.pitchQuantity(),
			input.pitchUnit(),
			0L,
			null,
			0,
			YeastActivityState.ACTIVE,
			input.sourceType() == null
				? YeastSourceType.DIRECT_PITCH
				: input.sourceType());
	}

	/*-------------------------------------------------------------------------*/
	private static long resolveCellCount(
		PitchInput input,
		YeastCulture culture,
		List<Warning> warnings)
	{
		CellCountMode mode = input.cellCountMode() == null
			? CellCountMode.ESTIMATE_FROM_QUANTITY
			: input.cellCountMode();

		if (mode == CellCountMode.MANUAL_TOTAL
			&& input.manualCellCount() != null
			&& input.manualCellCount() > 0L)
		{
			return input.manualCellCount();
		}

		if (mode == CellCountMode.SLURRY_DENSITY)
		{
			double density = input.slurryCellsPerMl() == null
				? SLURRY_DEFAULT_CELLS_PER_ML
				: input.slurryCellsPerMl();
			CellEstimate est = estimateSlurryPitchCells(
				culture,
				density,
				density == SLURRY_DEFAULT_CELLS_PER_ML
					? "ferment.log.cells.estimate.slurry.default"
					: null);
			if (est.lowConfidence() && est.warningKey() != null)
			{
				addProcessWarning(warnings, est.warningKey(), input.yeast().getName(), est.cells());
			}
			return est.cells();
		}

		CellEstimate est = estimateCellsForCulture(culture);
		if (est.lowConfidence() && est.warningKey() != null)
		{
			addProcessWarning(warnings, est.warningKey(), input.yeast().getName(), est.cells());
		}
		return est.cells();
	}

	/*-------------------------------------------------------------------------*/
	private static PercentageUnit resolveViability(
		PitchInput input,
		List<Warning> warnings)
	{
		ViabilityMode mode = input.viabilityMode() == null
			? ViabilityMode.DEFAULT_BY_SOURCE
			: input.viabilityMode();

		return switch (mode)
		{
			case MANUAL -> input.manualViability() != null
				? input.manualViability()
				: new PercentageUnit(
					defaultViabilityFraction(input.sourceType()),
					true);
			case FROM_PACKAGE_AGE -> estimateViabilityFromAge(
				input.yeast().getForm(),
				input.productionDate(),
				input.pitchDate(),
				input.storageTemperature());
			default -> new PercentageUnit(
				defaultViabilityFraction(input.sourceType()),
				true);
		};
	}

	/*-------------------------------------------------------------------------*/
	private static void addProcessWarning(
		List<Warning> warnings,
		String messageKey,
		String yeastName,
		long cells)
	{
		warnings.add(new Warning(
			WarningSeverity.WARNING,
			messageKey,
			new Object[] {yeastName, formatCells(cells)}));
	}

	/*-------------------------------------------------------------------------*/
	private static double lerp(double a, double b, double t)
	{
		t = Math.max(0D, Math.min(1D, t));
		return a + (b - a) * t;
	}

	/*-------------------------------------------------------------------------*/
	private static double clamp(double value, double min, double max)
	{
		return Math.max(min, Math.min(max, value));
	}
}
