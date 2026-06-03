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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Reverse and advisory carbonation calculations for {@link PackageStep} packaging.
 */
public final class CarbonationCalculator
{
	/** Typical safe upper limit for bottle/cask natural conditioning (vol CO₂). */
	public static final double SAFE_PACKAGING_MAX_VOL = 4.0D;

	/** Practical keg-regulator sanity check (gauge kPa). */
	public static final double HIGH_FORCE_CARB_PRESSURE_KPA = 140.0D;

	private static final double SPEISE_SEARCH_MAX_LITRES = 500D;
	private static final double KRAUSEN_SEARCH_MAX_LITRES = 500D;
	private static final int BINARY_SEARCH_ITERATIONS = 64;

	private CarbonationCalculator()
	{
	}

	/*-------------------------------------------------------------------------*/
	public enum Status
	{
		OK,
		NOT_ACHIEVABLE,
		MISSING_DATA,
		UNSUPPORTED
	}

	/*-------------------------------------------------------------------------*/
	public enum WarningSeverity
	{
		INFO,
		WARNING,
		ERROR
	}

	/*-------------------------------------------------------------------------*/
	public record SafetyWarning(
		WarningSeverity severity,
		String messageKey,
		Object[] args)
	{
	}

	/*-------------------------------------------------------------------------*/
	public record Result(
		Status status,
		CarbonationUnit baselineCarb,
		CarbonationUnit targetCarb,
		CarbonationUnit maxAchievableCarb,
		CarbonationUnit computedFinalCarb,
		FermentableAddition primingAddition,
		VolumeUnit requiredSpeiseVolume,
		VolumeUnit requiredKrausenVolume,
		PressureUnit equilibriumPressure,
		TemperatureUnit servingTemperature,
		CarbonationUnit styleCarbMin,
		CarbonationUnit styleCarbMax,
		List<SafetyWarning> warnings)
	{
		public Result
		{
			warnings = warnings == null ? List.of() : List.copyOf(warnings);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static Result calculate(
		PackageStep step,
		Recipe recipe,
		CarbonationUnit targetCarb,
		Fermentable primingFermentable,
		TemperatureUnit servingTemp)
	{
		if (step == null || recipe == null || targetCarb == null)
		{
			return missingData(null, targetCarb, null, servingTemp,
				List.of(new SafetyWarning(WarningSeverity.ERROR,
					"package.calc.missing.input", new Object[0])));
		}

		PackageStep.CarbonationMethod method = step.getCarbonationMethod();
		if (method == null)
		{
			method = PackageStep.CarbonationMethod.PRIMING_SUGAR;
		}

		if (method == PackageStep.CarbonationMethod.FORCE_CARB
			&& step.getPackagingType() == PackageStep.PackagingType.CASK)
		{
			return new Result(
				Status.UNSUPPORTED,
				null,
				targetCarb,
				null,
				null,
				null,
				null,
				null,
				null,
				servingTemp,
				null,
				null,
				List.of(new SafetyWarning(WarningSeverity.ERROR,
					"package.calc.force.carb.cask", new Object[0])));
		}

		Style style = resolveStyle(step.getStyleId());
		CarbonationUnit styleMin = style == null ? null : style.getCarbMin();
		CarbonationUnit styleMax = style == null ? null : style.getCarbMax();

		Volume beer = step.getInputVolume() == null
			? null
			: recipe.getVolumes().getVolume(step.getInputVolume());
		if (beer == null || beer.getVolume() == null)
		{
			return missingData(styleMin, targetCarb, styleMax, servingTemp,
				List.of(new SafetyWarning(WarningSeverity.ERROR,
					"package.calc.no.input.beer", new Object[0])));
		}

		CarbonationUnit baseline = beer.getCarbonation();
		if (baseline == null)
		{
			baseline = new CarbonationUnit(0);
		}

		VolumeUnit beerVol = beer.getVolume();
		VolumeUnit beerAfterLoss = calcBeerVolumeAfterLoss(step, beerVol);
		VolumeUnit beerBeforeLoss = beerVol;

		List<SafetyWarning> warnings = new ArrayList<>();
		Status status = Status.OK;
		FermentableAddition primingAddition = null;
		VolumeUnit requiredSpeise = null;
		VolumeUnit requiredKrausen = null;
		CarbonationUnit maxAchievable = null;
		CarbonationUnit computedFinal = targetCarb;
		PressureUnit equilibriumPressure = null;
		TemperatureUnit temp = servingTemp != null
			? servingTemp
			: new TemperatureUnit(4D, CELSIUS);

		switch (method)
		{
			case PRIMING_SUGAR ->
			{
				if (primingFermentable == null)
				{
					status = Status.MISSING_DATA;
					warnings.add(new SafetyWarning(WarningSeverity.ERROR,
						"package.calc.priming.no.fermentable", new Object[0]));
				}
				else
				{
					double deltaVol = targetCarb.get(VOLUMES) - baseline.get(VOLUMES);
					if (deltaVol <= 1e-6)
					{
						status = Status.NOT_ACHIEVABLE;
						warnings.add(new SafetyWarning(WarningSeverity.ERROR,
							"package.calc.priming.below.baseline", new Object[0]));
					}
					else
					{
						CarbonationUnit deltaTarget = new CarbonationUnit(deltaVol, VOLUMES, false);
						primingAddition = Equations.calcPrimingSugarAmount(
							beerVol, primingFermentable, deltaTarget);
						CarbonationUnit added = Equations.calcCarbonation(beerVol, primingAddition);
						computedFinal = new CarbonationUnit(
							baseline.get(VOLUMES) + added.get(VOLUMES),
							VOLUMES,
							baseline.isEstimated() || added.isEstimated());
					}
				}
			}
			case SPEISE ->
			{
				if (step.getSpeiseVolume() == null
					|| !recipe.getVolumes().contains(step.getSpeiseVolume()))
				{
					status = Status.MISSING_DATA;
					warnings.add(new SafetyWarning(WarningSeverity.ERROR,
						"package.calc.speise.not.set", new Object[0]));
				}
				else
				{
					Volume speiseRef = recipe.getVolumes().getVolume(step.getSpeiseVolume());
					requiredSpeise = calcRequiredSpeiseVolume(
						beerAfterLoss,
						baseline,
						speiseRef,
						targetCarb);
					if (requiredSpeise == null)
					{
						status = Status.NOT_ACHIEVABLE;
						warnings.add(new SafetyWarning(WarningSeverity.ERROR,
							"package.calc.speise.not.achievable", new Object[0]));
					}
					else
					{
						computedFinal = calcSpeiseFinalCarb(
							beerAfterLoss, baseline, speiseRef, requiredSpeise);
					}
				}
			}
			case SPUNDING ->
			{
				ProcessLog log = new ProcessLog();
				DensityUnit packagingGravity = beer.getGravity();
				if (packagingGravity == null)
				{
					status = Status.MISSING_DATA;
					warnings.add(new SafetyWarning(WarningSeverity.ERROR,
						"package.calc.spunding.no.gravity", new Object[0]));
				}
				else
				{
					DensityUnit terminalFg = FermentationCalculator.calcPredictedTerminalFg(
						beer, step.getYeastAdditions(), log);
					if (terminalFg == null
						|| packagingGravity.get(DensityUnit.Unit.GU)
							<= terminalFg.get(DensityUnit.Unit.GU))
					{
						status = Status.NOT_ACHIEVABLE;
						warnings.add(new SafetyWarning(WarningSeverity.ERROR,
							"package.calc.spunding.not.achievable", new Object[0]));
					}
					else
					{
						maxAchievable = calcSpundingMaxCarb(
							beerBeforeLoss, beerAfterLoss, baseline,
							packagingGravity, terminalFg);
						computedFinal = maxAchievable;
						if (targetCarb.get(VOLUMES) > maxAchievable.get(VOLUMES) + 1e-6)
						{
							status = Status.NOT_ACHIEVABLE;
							warnings.add(new SafetyWarning(WarningSeverity.ERROR,
								"package.calc.spunding.shortfall",
								new Object[] {
									maxAchievable.get(VOLUMES),
									targetCarb.get(VOLUMES)}));
						}
					}
				}
			}
			case KRAUSENING ->
			{
				ProcessLog log = new ProcessLog();
				Volume krausenRef = KrausenSourceResolver.resolveSnapshot(
					step.getKrausenRecipeName(),
					step.getKrausenVolumeName(),
					recipe,
					recipe.getVolumes(),
					log);
				if (krausenRef == null)
				{
					status = Status.MISSING_DATA;
					warnings.add(new SafetyWarning(WarningSeverity.ERROR,
						"package.calc.krausen.not.set", new Object[0]));
				}
				else
				{
					requiredKrausen = calcRequiredKrausenVolume(
						beerAfterLoss, baseline, krausenRef, targetCarb, log);
					if (requiredKrausen == null)
					{
						status = Status.NOT_ACHIEVABLE;
						warnings.add(new SafetyWarning(WarningSeverity.ERROR,
							"package.calc.krausen.not.achievable", new Object[0]));
					}
					else
					{
						computedFinal = calcKrausenFinalCarb(
							beerAfterLoss, baseline, krausenRef, requiredKrausen, log);
					}
				}
			}
			case FORCE_CARB ->
			{
				if (step.getPackagingType() != PackageStep.PackagingType.KEG)
				{
					status = Status.UNSUPPORTED;
					warnings.add(new SafetyWarning(WarningSeverity.ERROR,
						"package.calc.force.carb.keg.only", new Object[0]));
				}
				else
				{
					computedFinal = targetCarb;
					equilibriumPressure = Equations.calcEquilibriumPressureFromCo2(temp, targetCarb);
				}
			}
		}

		evaluateSafetyWarnings(
			step,
			method,
			targetCarb,
			computedFinal,
			maxAchievable,
			baseline,
			styleMin,
			styleMax,
			equilibriumPressure,
			warnings);

		return new Result(
			status,
			baseline,
			targetCarb,
			maxAchievable,
			computedFinal,
			primingAddition,
			requiredSpeise,
			requiredKrausen,
			equilibriumPressure,
			temp,
			styleMin,
			styleMax,
			warnings);
	}

	/*-------------------------------------------------------------------------*/
	public static CarbonationUnit defaultTarget(PackageStep step, Recipe recipe)
	{
		Style style = resolveStyle(step == null ? null : step.getStyleId());
		if (style != null && style.getCarbMin() != null && style.getCarbMax() != null)
		{
			double mid = (style.getCarbMin().get(VOLUMES) + style.getCarbMax().get(VOLUMES)) / 2D;
			return new CarbonationUnit(mid, VOLUMES, false);
		}
		if (step != null && step.getForcedCarbonation() != null)
		{
			return step.getForcedCarbonation();
		}
		if (step != null && step.getInputVolume() != null && recipe != null)
		{
			Volume beer = recipe.getVolumes().getVolume(step.getInputVolume());
			if (beer != null && beer.getCarbonation() != null)
			{
				return beer.getCarbonation();
			}
		}
		return new CarbonationUnit(2.4D, VOLUMES, false);
	}

	/*-------------------------------------------------------------------------*/
	public static VolumeUnit calcBeerVolumeAfterLoss(PackageStep step, VolumeUnit beerVol)
	{
		if (step == null || beerVol == null)
		{
			return beerVol;
		}
		double litres = beerVol.get(LITRES);
		if (step.getPackagingLoss() != null)
		{
			litres -= step.getPackagingLoss().get(LITRES);
		}
		VolumeUnit hopLoss = Equations.calcTotalHopAbsorptionLoss(step.getHopAdditions());
		if (hopLoss.get() > 0)
		{
			litres -= hopLoss.get(LITRES);
		}
		return new VolumeUnit(Math.max(0D, litres), LITRES, beerVol.isEstimated());
	}

	/*-------------------------------------------------------------------------*/
	private static Result missingData(
		CarbonationUnit styleMin,
		CarbonationUnit targetCarb,
		CarbonationUnit styleMax,
		TemperatureUnit servingTemp,
		List<SafetyWarning> warnings)
	{
		return new Result(
			Status.MISSING_DATA,
			null,
			targetCarb,
			null,
			null,
			null,
			null,
			null,
			null,
			servingTemp,
			styleMin,
			styleMax,
			warnings);
	}

	/*-------------------------------------------------------------------------*/
	private static Style resolveStyle(String styleId)
	{
		if (styleId == null || styleId.isBlank())
		{
			return null;
		}
		return Database.getInstance().getStyles().get(styleId);
	}

	/*-------------------------------------------------------------------------*/
	private static CarbonationUnit calcSpundingMaxCarb(
		VolumeUnit beerBeforeLoss,
		VolumeUnit beerAfterLoss,
		CarbonationUnit baseline,
		DensityUnit packagingGravity,
		DensityUnit terminalFg)
	{
		WeightUnit remaining = Equations.calcRemainingFermentableExtractInBeer(
			beerBeforeLoss, packagingGravity, terminalFg);
		PackagingFermentationResult fermentation = Equations.calcPackagingFermentationFromExtract(
			beerAfterLoss, remaining, new PercentageUnit(1D));
		return new CarbonationUnit(
			baseline.get(VOLUMES) + fermentation.carbonation.get(VOLUMES),
			VOLUMES,
			baseline.isEstimated() || fermentation.carbonation.isEstimated());
	}

	/*-------------------------------------------------------------------------*/
	private static VolumeUnit calcRequiredSpeiseVolume(
		VolumeUnit beerAfterLoss,
		CarbonationUnit baseline,
		Volume speiseRef,
		CarbonationUnit target)
	{
		double targetVol = target.get(VOLUMES);
		if (targetVol <= baseline.get(VOLUMES) + 1e-6)
		{
			return new VolumeUnit(0, LITRES, false);
		}

		DensityUnit gravity = speiseRef.getGravity();
		if (gravity == null)
		{
			return null;
		}

		double low = 0D;
		double high = SPEISE_SEARCH_MAX_LITRES;
		for (int i = 0; i < BINARY_SEARCH_ITERATIONS; i++)
		{
			double mid = (low + high) / 2D;
			CarbonationUnit finalCarb = calcSpeiseFinalCarbAtVolume(
				beerAfterLoss, baseline, speiseRef, mid);
			if (finalCarb.get(VOLUMES) < targetVol)
			{
				low = mid;
			}
			else
			{
				high = mid;
			}
		}

		CarbonationUnit atHigh = calcSpeiseFinalCarbAtVolume(
			beerAfterLoss, baseline, speiseRef, high);
		if (atHigh.get(VOLUMES) + 1e-4 < targetVol)
		{
			return null;
		}

		return new VolumeUnit(high, LITRES, beerAfterLoss.isEstimated());
	}

	/*-------------------------------------------------------------------------*/
	private static CarbonationUnit calcSpeiseFinalCarb(
		VolumeUnit beerAfterLoss,
		CarbonationUnit baseline,
		Volume speiseRef,
		VolumeUnit speiseVol)
	{
		return calcSpeiseFinalCarbAtVolume(
			beerAfterLoss, baseline, speiseRef, speiseVol.get(LITRES));
	}

	/*-------------------------------------------------------------------------*/
	private static CarbonationUnit calcSpeiseFinalCarbAtVolume(
		VolumeUnit beerAfterLoss,
		CarbonationUnit baseline,
		Volume speiseRef,
		double speiseLitres)
	{
		VolumeUnit speiseVol = new VolumeUnit(speiseLitres, LITRES);
		VolumeUnit packageVol = new VolumeUnit(
			beerAfterLoss.get(LITRES) + speiseLitres, LITRES);

		WeightUnit extract = Equations.calcFermentableExtractFromWort(
			speiseVol,
			speiseRef.getGravity(),
			speiseRef.getFermentability());
		PackagingFermentationResult fermentation = Equations.calcPackagingFermentationFromExtract(
			packageVol, extract, new PercentageUnit(1D));

		return new CarbonationUnit(
			baseline.get(VOLUMES) + fermentation.carbonation.get(VOLUMES),
			VOLUMES,
			baseline.isEstimated() || fermentation.carbonation.isEstimated());
	}

	/*-------------------------------------------------------------------------*/
	private static VolumeUnit calcRequiredKrausenVolume(
		VolumeUnit beerAfterLoss,
		CarbonationUnit baseline,
		Volume krausenRef,
		CarbonationUnit target,
		ProcessLog log)
	{
		double targetVol = target.get(VOLUMES);
		if (targetVol <= baseline.get(VOLUMES) + 1e-6)
		{
			return new VolumeUnit(0, LITRES, false);
		}

		double low = 0D;
		double high = KRAUSEN_SEARCH_MAX_LITRES;
		for (int i = 0; i < BINARY_SEARCH_ITERATIONS; i++)
		{
			double mid = (low + high) / 2D;
			CarbonationUnit finalCarb = calcKrausenFinalCarbAtVolume(
				beerAfterLoss, baseline, krausenRef, mid, log);
			if (finalCarb.get(VOLUMES) < targetVol)
			{
				low = mid;
			}
			else
			{
				high = mid;
			}
		}

		CarbonationUnit atHigh = calcKrausenFinalCarbAtVolume(
			beerAfterLoss, baseline, krausenRef, high, log);
		if (atHigh.get(VOLUMES) + 1e-4 < targetVol)
		{
			return null;
		}

		return new VolumeUnit(high, LITRES, beerAfterLoss.isEstimated());
	}

	/*-------------------------------------------------------------------------*/
	private static CarbonationUnit calcKrausenFinalCarb(
		VolumeUnit beerAfterLoss,
		CarbonationUnit baseline,
		Volume krausenRef,
		VolumeUnit krausenVol,
		ProcessLog log)
	{
		return calcKrausenFinalCarbAtVolume(
			beerAfterLoss, baseline, krausenRef, krausenVol.get(LITRES), log);
	}

	/*-------------------------------------------------------------------------*/
	private static CarbonationUnit calcKrausenFinalCarbAtVolume(
		VolumeUnit beerAfterLoss,
		CarbonationUnit baseline,
		Volume krausenRef,
		double krausenLitres,
		ProcessLog log)
	{
		VolumeUnit krausenVol = new VolumeUnit(krausenLitres, LITRES);
		CarbonationUnit krausenCarb = krausenRef.getCarbonation();
		if (krausenCarb == null)
		{
			krausenCarb = new CarbonationUnit(0);
		}

		double totalCarb = baseline.get(VOLUMES);
		if (krausenLitres > 0D && beerAfterLoss.get(LITRES) > 0D)
		{
			Quantity blended = Equations.calcCombinedLinearInterpolation(
				beerAfterLoss,
				baseline,
				krausenVol,
				krausenCarb);
			if (blended instanceof CarbonationUnit carb)
			{
				totalCarb = carb.get(VOLUMES);
			}
		}

		VolumeUnit packageVol = new VolumeUnit(
			beerAfterLoss.get(LITRES) + krausenLitres, LITRES);

		Volume krausenPartial = new Volume("_calc_krausen", krausenRef);
		krausenPartial.setVolume(krausenVol);

		WeightUnit extract = calcKrausenFermentableExtract(krausenPartial, log);
		if (extract != null && extract.get(KILOGRAMS) > 0D)
		{
			PackagingFermentationResult fermentation = Equations.calcPackagingFermentationFromExtract(
				packageVol, extract, new PercentageUnit(1D));
			totalCarb += fermentation.carbonation.get(VOLUMES);
		}

		return new CarbonationUnit(totalCarb, VOLUMES, baseline.isEstimated());
	}

	/*-------------------------------------------------------------------------*/
	private static WeightUnit calcKrausenFermentableExtract(Volume krausen, ProcessLog log)
	{
		VolumeUnit krausenVol = krausen.getVolume();
		DensityUnit gravity = krausen.getGravity();

		if (krausen.getType() == Volume.Type.WORT)
		{
			return Equations.calcFermentableExtractFromWort(
				krausenVol, gravity, krausen.getFermentability());
		}

		DensityUnit terminalFg = FermentationCalculator.calcPredictedTerminalFg(
			krausen, Collections.emptyList(), log);
		if (terminalFg == null)
		{
			return new WeightUnit(0);
		}

		return Equations.calcRemainingFermentableExtractInBeer(
			krausenVol, gravity, terminalFg);
	}

	/*-------------------------------------------------------------------------*/
	private static void evaluateSafetyWarnings(
		PackageStep step,
		PackageStep.CarbonationMethod method,
		CarbonationUnit target,
		CarbonationUnit computedFinal,
		CarbonationUnit maxAchievable,
		CarbonationUnit baseline,
		CarbonationUnit styleMin,
		CarbonationUnit styleMax,
		PressureUnit equilibriumPressure,
		List<SafetyWarning> warnings)
	{
		double targetVol = target.get(VOLUMES);

		if (styleMin != null && targetVol < styleMin.get(VOLUMES) - 1e-6)
		{
			warnings.add(new SafetyWarning(WarningSeverity.WARNING,
				"package.calc.warn.style.carb.low",
				new Object[] {targetVol, styleMin.get(VOLUMES)}));
		}
		if (styleMax != null && targetVol > styleMax.get(VOLUMES) + 1e-6)
		{
			warnings.add(new SafetyWarning(WarningSeverity.WARNING,
				"package.calc.warn.style.carb.high",
				new Object[] {targetVol, styleMax.get(VOLUMES)}));
		}

		PackageStep.PackagingType vessel = step.getPackagingType();
		if (vessel == PackageStep.PackagingType.BOTTLE
			|| vessel == PackageStep.PackagingType.CASK)
		{
			if (targetVol > SAFE_PACKAGING_MAX_VOL)
			{
				warnings.add(new SafetyWarning(WarningSeverity.WARNING,
					"package.calc.warn.safe.max.vessel",
					new Object[] {targetVol, SAFE_PACKAGING_MAX_VOL}));
			}
		}
		else if (vessel == PackageStep.PackagingType.KEG
			&& method != PackageStep.CarbonationMethod.FORCE_CARB
			&& targetVol > SAFE_PACKAGING_MAX_VOL)
		{
			warnings.add(new SafetyWarning(WarningSeverity.WARNING,
				"package.calc.warn.safe.max.natural",
				new Object[] {targetVol, SAFE_PACKAGING_MAX_VOL}));
		}

		double checkVol = computedFinal != null
			? computedFinal.get(VOLUMES)
			: targetVol;
		if (method != PackageStep.CarbonationMethod.FORCE_CARB
			&& method != PackageStep.CarbonationMethod.SPUNDING
			&& checkVol > SAFE_PACKAGING_MAX_VOL)
		{
			warnings.add(new SafetyWarning(WarningSeverity.WARNING,
				"package.calc.warn.safe.max.result",
				new Object[] {checkVol, SAFE_PACKAGING_MAX_VOL}));
		}

		if (method == PackageStep.CarbonationMethod.SPUNDING
			&& maxAchievable != null
			&& maxAchievable.get(VOLUMES) > SAFE_PACKAGING_MAX_VOL)
		{
			warnings.add(new SafetyWarning(WarningSeverity.WARNING,
				"package.calc.warn.spunding.over.safe",
				new Object[] {maxAchievable.get(VOLUMES), SAFE_PACKAGING_MAX_VOL}));
		}

		if (method != PackageStep.CarbonationMethod.FORCE_CARB
			&& method != PackageStep.CarbonationMethod.SPUNDING
			&& baseline != null
			&& targetVol <= baseline.get(VOLUMES) + 1e-6)
		{
			warnings.add(new SafetyWarning(WarningSeverity.INFO,
				"package.calc.warn.baseline",
				new Object[] {baseline.get(VOLUMES)}));
		}

		if (equilibriumPressure != null
			&& equilibriumPressure.get(KPA) > HIGH_FORCE_CARB_PRESSURE_KPA)
		{
			warnings.add(new SafetyWarning(WarningSeverity.WARNING,
				"package.calc.warn.high.pressure",
				new Object[] {
					equilibriumPressure.get(KPA),
					HIGH_FORCE_CARB_PRESSURE_KPA}));
		}
	}
}
