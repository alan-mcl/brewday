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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;
import mclachlan.brewday.util.StringUtils;

/**
 * Creates and output volume for this batch.
 */
public class PackageStep extends FluidVolumeProcessStep
{
	/** packaging loss in ml */
	private VolumeUnit packagingLoss;

	/** The style ID of this package step */
	private String styleId;

	/** kegging or bottling */
	private PackagingType packagingType;

	/** how carbonation is achieved */
	private CarbonationMethod carbonationMethod;

	/** target carbonation for {@link CarbonationMethod#FORCE_CARB} */
	private CarbonationUnit forcedCarbonation;

	/** read-only WORT volume used as Speise when {@link CarbonationMethod#SPEISE} */
	private String speiseVolume;

	/** source recipe name when {@link CarbonationMethod#KRAUSENING} */
	private String krausenRecipeName;

	/** source volume name within {@link #krausenRecipeName} when {@link CarbonationMethod#KRAUSENING} */
	private String krausenVolumeName;

	/** negligible remaining extract (kg) for warnings */
	private static final double KRAUSEN_NEGLIGIBLE_EXTRACT_KG = 0.001D;

	/*-------------------------------------------------------------------------*/
	public enum PackagingType
	{
		BOTTLE, KEG, CASK;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("package."+name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum CarbonationMethod
	{
		FORCE_CARB, PRIMING_SUGAR, SPEISE, SPUNDING, KRAUSENING;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("package.carbonation."+name());
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Vessel and carbonation method inferred from pre-refactor persisted
	 * {@code packagingType} when {@code carbonationMethod} is absent.
	 */
	public static final class LegacyPackaging
	{
		public final PackagingType packagingType;
		public final CarbonationMethod carbonationMethod;

		public LegacyPackaging(
			PackagingType packagingType,
			CarbonationMethod carbonationMethod)
		{
			this.packagingType = packagingType;
			this.carbonationMethod = carbonationMethod;
		}
	}

	/*-------------------------------------------------------------------------*/
	public static LegacyPackaging migrateLegacyPackaging(String legacyPackagingType)
	{
		switch (legacyPackagingType)
		{
			case "BOTTLE":
				return new LegacyPackaging(PackagingType.BOTTLE, CarbonationMethod.PRIMING_SUGAR);
			case "KEG":
				return new LegacyPackaging(PackagingType.KEG, CarbonationMethod.FORCE_CARB);
			case "KEG_WITH_PRIMING":
				return new LegacyPackaging(PackagingType.KEG, CarbonationMethod.PRIMING_SUGAR);
			default:
				throw new BrewdayException("Unknown legacy packagingType: "+legacyPackagingType);
		}
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep()
	{
		packagingType = PackagingType.BOTTLE;
		carbonationMethod = CarbonationMethod.PRIMING_SUGAR;
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(
		String name,
		String description,
		List<IngredientAddition> ingredientAdditions,
		String inputVolume,
		String outputVolume,
		VolumeUnit packagingLoss,
		String styleId,
		PackagingType packagingType,
		CarbonationMethod carbonationMethod,
		CarbonationUnit forcedCarbonation)
	{
		this(name, description, ingredientAdditions, inputVolume, outputVolume,
			packagingLoss, styleId, packagingType, carbonationMethod, forcedCarbonation, null, null, null);
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(
		String name,
		String description,
		List<IngredientAddition> ingredientAdditions,
		String inputVolume,
		String outputVolume,
		VolumeUnit packagingLoss,
		String styleId,
		PackagingType packagingType,
		CarbonationMethod carbonationMethod,
		CarbonationUnit forcedCarbonation,
		String speiseVolume)
	{
		this(name, description, ingredientAdditions, inputVolume, outputVolume,
			packagingLoss, styleId, packagingType, carbonationMethod, forcedCarbonation,
			speiseVolume, null, null);
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(
		String name,
		String description,
		List<IngredientAddition> ingredientAdditions,
		String inputVolume,
		String outputVolume,
		VolumeUnit packagingLoss,
		String styleId,
		PackagingType packagingType,
		CarbonationMethod carbonationMethod,
		CarbonationUnit forcedCarbonation,
		String speiseVolume,
		String krausenRecipeName,
		String krausenVolumeName)
	{
		super(name, description, Type.PACKAGE, inputVolume, outputVolume);
		setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
		this.styleId = styleId;
		this.packagingLoss = packagingLoss;
		this.packagingType = packagingType;
		this.carbonationMethod = carbonationMethod;
		this.forcedCarbonation = forcedCarbonation;
		this.speiseVolume = speiseVolume;
		this.krausenRecipeName = krausenRecipeName;
		this.krausenVolumeName = krausenVolumeName;
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(Recipe recipe)
	{
		super(
			recipe.getUniqueStepName(Type.PACKAGE),
			StringUtils.getProcessString("package.desc"),
			Type.PACKAGE,
			null,
			null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.BEER, recipe));
		setOutputVolume(StringUtils.getProcessString("package.output", getName()));

		packagingLoss = new VolumeUnit(500);
		packagingType = PackagingType.BOTTLE;
		carbonationMethod = CarbonationMethod.PRIMING_SUGAR;
		forcedCarbonation = null;
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(PackageStep other)
	{
		super(other.getName(), other.getDescription(), Type.PACKAGE, other.getInputVolume(), other.getOutputVolume());

		this.packagingLoss = other.packagingLoss;
		this.styleId = other.styleId;
		this.packagingType = other.packagingType;
		this.carbonationMethod = other.carbonationMethod;
		this.forcedCarbonation = other.forcedCarbonation;
		this.speiseVolume = other.speiseVolume;
		this.krausenRecipeName = other.krausenRecipeName;
		this.krausenVolumeName = other.krausenVolumeName;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (!super.validateInputVolumes(volumes, log))
		{
			return false;
		}

		if (carbonationMethod == CarbonationMethod.SPEISE)
		{
			if (speiseVolume == null || !volumes.contains(speiseVolume))
			{
				log.addError(StringUtils.getProcessString(
					"volumes.does.not.exist",
					speiseVolume == null ? "" : speiseVolume));
				return false;
			}

			Volume speise = volumes.getVolume(speiseVolume);
			if (speise.getType() != Volume.Type.WORT)
			{
				log.addError(StringUtils.getProcessString(
					"package.speise.not.wort",
					speiseVolume,
					speise.getType()));
				return false;
			}

			if (speise.getVolume() == null || speise.getVolume().get() <= 0)
			{
				log.addError(StringUtils.getProcessString(
					"package.speise.zero.volume",
					speiseVolume));
				return false;
			}
		}

		if (carbonationMethod == CarbonationMethod.KRAUSENING && isKrausenFromCurrentRecipe())
		{
			if (krausenVolumeName == null || !volumes.contains(krausenVolumeName))
			{
				log.addError(StringUtils.getProcessString(
					"volumes.does.not.exist",
					krausenVolumeName == null ? "" : krausenVolumeName));
				return false;
			}

			Volume krausen = volumes.getVolume(krausenVolumeName);
			if (krausen.getType() != Volume.Type.WORT && krausen.getType() != Volume.Type.BEER)
			{
				log.addError(StringUtils.getProcessString(
					"package.krausen.invalid.type",
					krausenVolumeName,
					krausen.getType()));
				return false;
			}

			if (krausen.getVolume() == null || krausen.getVolume().get() <= 0)
			{
				log.addError(StringUtils.getProcessString(
					"package.krausen.zero.volume",
					krausenVolumeName));
				return false;
			}
		}

		return true;
	}

	/*-------------------------------------------------------------------------*/
	private boolean isKrausenFromCurrentRecipe()
	{
		return krausenRecipeName != null
			&& getRecipe() != null
			&& krausenRecipeName.equals(getRecipe().getName());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getInputVolumes()
	{
		if (carbonationMethod == CarbonationMethod.SPEISE && speiseVolume != null)
		{
			return Arrays.asList(getInputVolume(), speiseVolume);
		}
		if (carbonationMethod == CarbonationMethod.KRAUSENING
			&& isKrausenFromCurrentRecipe()
			&& krausenVolumeName != null)
		{
			return Arrays.asList(getInputVolume(), krausenVolumeName);
		}
		return super.getInputVolumes();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		//
		// Require finished beer (or wort) volume before packaging loss and carbonation are applied.
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume volumeIn = getInputVolume(volumes);

		VolumeUnit volumeInBefore = volumeIn.getVolume();

		//
		// Packaging removes transfer loss (hose, spillage, sample volume): packaged volume is what
		// remains in keg, bottle, or bright tank.
		//
		VolumeUnit volumeOut = new VolumeUnit(
			volumeIn.getVolume().get()
				- packagingLoss.get());

		VolumeUnit hopAbsorptionLoss = Equations.calcTotalHopAbsorptionLoss(getHopAdditions());
		if (hopAbsorptionLoss.get() > 0)
		{
			volumeOut = new VolumeUnit(volumeOut.get() - hopAbsorptionLoss.get());
			log.addVerboseMessage(StringUtils.getProcessString("package.hop.absorption.loss",
				hopAbsorptionLoss.get(Quantity.Unit.LITRES)));
		}

		//
		// Late/dry hop additions at packaging do not isomerise; report the alpha-acid mass they
		// represent (inventory is folded into the packaged volume in publishPackagedVolume).
		//
		for (HopAddition hop : getHopAdditions())
		{
			boolean preIsomerized = hop.getForm() != null
				&& hop.getForm().isPreIsomerized();
			log.addVerboseMessage(StringUtils.getProcessString("log.hop.addition.dryhop",
				describeHopAddition(hop, Quantity.Unit.DAYS),
				formatDryHopAlpha(Equations.calcHopAlphaAcidsMg(hop), preIsomerized)));
		}

		CarbonationMethod method = validatePackagingConfiguration(log);

		CarbonationUnit carbonationOut = volumeIn.getCarbonation();
		if (carbonationOut == null)
		{
			carbonationOut = new CarbonationUnit(0);
		}
		double totalCarb = carbonationOut.get(Quantity.Unit.VOLUMES);
		boolean carbEstimated = carbonationOut.isEstimated();

		PercentageUnit abvOut = volumeIn.getAbv();
		if (abvOut == null)
		{
			abvOut = new PercentageUnit(0);
		}
		double totalAbv = abvOut.get();
		boolean abvEstimated = abvOut.isEstimated();

		if (method == CarbonationMethod.FORCE_CARB)
		{
			if (this.forcedCarbonation != null)
			{
				totalCarb = this.forcedCarbonation.get(Quantity.Unit.VOLUMES);
				carbEstimated = this.forcedCarbonation.isEstimated();
			}
		}
		else if (method == CarbonationMethod.PRIMING_SUGAR)
		{
			//
			// Packaging fermentation: 1 g fermentable extract → 0.5 g ethanol + 0.5 g CO₂ (× yield).
			// Assumes 100% attenuation of priming fermentables. OG/FG unchanged on output.
			//
			DensityUnit gravityIn = volumeIn.getGravity();

			for (IngredientAddition ia : getIngredientAdditions())
			{
				if (ia instanceof FermentableAddition fa)
				{
					if (!Equations.isPrimingFermentable(fa))
					{
						continue;
					}

					CarbonationUnit addedCarbonation = Equations.calcCarbonation(
						volumeIn.getVolume(), fa);
					totalCarb += addedCarbonation.get(Quantity.Unit.VOLUMES);
					carbEstimated = carbEstimated || addedCarbonation.isEstimated();

					PercentageUnit abvAdded = Equations.calcPackagingFermentationAbvIncrease(
						volumeIn.getVolume(), gravityIn, fa);
					totalAbv += abvAdded.get();
					abvEstimated = abvEstimated || abvAdded.isEstimated();
				}
			}

			publishPackagedVolume(
				volumes, log, volumeIn, volumeInBefore, volumeOut, totalCarb, carbEstimated,
				totalAbv, abvEstimated, null, null);
			return;
		}
		else if (method == CarbonationMethod.SPEISE)
		{
			applySpeise(volumes, log, volumeIn, volumeInBefore, volumeOut, totalCarb, carbEstimated,
				totalAbv, abvEstimated);
			return;
		}
		else if (method == CarbonationMethod.SPUNDING)
		{
			applySpunding(volumes, log, volumeIn, volumeInBefore, volumeOut, totalCarb, carbEstimated,
				totalAbv, abvEstimated);
			return;
		}
		else if (method == CarbonationMethod.KRAUSENING)
		{
			applyKrausening(volumes, log, volumeIn, volumeInBefore, volumeOut);
			return;
		}

		publishPackagedVolume(
			volumes, log, volumeIn, volumeInBefore, volumeOut, totalCarb, carbEstimated,
			totalAbv, abvEstimated, null, null);
	}

	/*-------------------------------------------------------------------------*/
	private void applySpeise(
		Volumes volumes,
		ProcessLog log,
		Volume volumeIn,
		VolumeUnit volumeInBefore,
		VolumeUnit beerVolumeAfterLoss,
		double totalCarb,
		boolean carbEstimated,
		double totalAbv,
		boolean abvEstimated)
	{
		Volume speise = volumes.getVolume(speiseVolume);

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.speise.source", speiseVolume));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.speise.volume",
			speise.getVolume().get(Quantity.Unit.LITRES)));

		DensityUnit speiseGravity = speise.getGravity();
		if (speiseGravity != null)
		{
			log.addVerboseMessage(StringUtils.getProcessString(
				"package.speise.gravity",
				speiseGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}
		else
		{
			log.addWarning(StringUtils.getProcessString("package.speise.no.gravity", speiseVolume));
		}

		Volume beerWorking = new Volume("_pkg_beer", volumeIn);
		beerWorking.setVolume(beerVolumeAfterLoss);

		Volume blended = Combine.blendLikeCombine(
			beerWorking,
			speise,
			"_pkg_blend",
			Volume.Type.BEER);

		if (blended == null)
		{
			log.addError(StringUtils.getProcessString(
				"package.speise.blend.failed",
				getInputVolume(),
				speiseVolume));
			return;
		}

		VolumeUnit packageVol = new VolumeUnit(
			beerVolumeAfterLoss.get() + speise.getVolume().get());

		if (speiseGravity != null)
		{
			WeightUnit fermentableExtract = Equations.calcFermentableExtractFromWort(
				speise.getVolume(),
				speiseGravity,
				speise.getFermentability());

			log.addVerboseMessage(StringUtils.getProcessString(
				"package.speise.fermentable.extract",
				fermentableExtract.get(Quantity.Unit.KILOGRAMS)));

			PackagingFermentationResult fermentation = Equations.calcPackagingFermentationFromExtract(
				packageVol,
				fermentableExtract,
				new PercentageUnit(1D));

			totalCarb += fermentation.carbonation.get(Quantity.Unit.VOLUMES);
			carbEstimated = carbEstimated || fermentation.carbonation.isEstimated();
			totalAbv += fermentation.abvIncrease.get();
			abvEstimated = abvEstimated || fermentation.abvIncrease.isEstimated();

			log.addVerboseMessage(StringUtils.getProcessString(
				"package.speise.carb.added",
				fermentation.carbonation.get(Quantity.Unit.VOLUMES)));

			log.addVerboseMessage(StringUtils.getProcessString(
				"package.speise.abv.added",
				fermentation.abvIncrease.get(Quantity.Unit.PERCENTAGE_DISPLAY)));
		}

		publishPackagedVolume(
			volumes, log, volumeIn, volumeInBefore, packageVol, totalCarb, carbEstimated,
			totalAbv, abvEstimated, blended, null);
	}

	/*-------------------------------------------------------------------------*/
	private void applyKrausening(
		Volumes volumes,
		ProcessLog log,
		Volume volumeIn,
		VolumeUnit volumeInBefore,
		VolumeUnit beerVolumeAfterLoss)
	{
		Volume krausen = KrausenSourceResolver.resolveSnapshot(
			krausenRecipeName,
			krausenVolumeName,
			getRecipe(),
			volumes,
			log);
		if (krausen == null)
		{
			return;
		}

		if (!validateKrausenSnapshot(krausen, log))
		{
			return;
		}

		logKrausenSource(krausen, log);

		Volume beerWorking = new Volume("_pkg_beer", volumeIn);
		beerWorking.setVolume(beerVolumeAfterLoss);

		Volume blended = Combine.blendLikeCombine(
			beerWorking,
			krausen,
			"_pkg_blend",
			Volume.Type.BEER);

		if (blended == null)
		{
			log.addError(StringUtils.getProcessString(
				"package.krausen.blend.failed",
				getInputVolume(),
				krausenVolumeName,
				krausenRecipeName));
			return;
		}

		VolumeUnit packageVol = new VolumeUnit(
			beerVolumeAfterLoss.get() + krausen.getVolume().get());

		CarbonationUnit blendedCarb = blended.getCarbonation();
		double totalCarb = blendedCarb != null
			? blendedCarb.get(Quantity.Unit.VOLUMES)
			: 0D;
		boolean carbEstimated = blendedCarb != null && blendedCarb.isEstimated();

		PercentageUnit blendedAbv = blended.getAbv();
		double totalAbv = blendedAbv != null ? blendedAbv.get() : 0D;
		boolean abvEstimated = blendedAbv != null && blendedAbv.isEstimated();

		WeightUnit fermentableExtract = calcKrausenFermentableExtract(krausen, log);
		if (fermentableExtract == null)
		{
			return;
		}

		double extractKg = fermentableExtract.get(Quantity.Unit.KILOGRAMS);
		log.addVerboseMessage(StringUtils.getProcessString(
			"package.krausen.remaining.extract",
			extractKg));

		if (extractKg <= KRAUSEN_NEGLIGIBLE_EXTRACT_KG)
		{
			log.addWarning(StringUtils.getProcessString("package.krausen.negligible.extract"));
		}

		if (extractKg > 0D)
		{
			PackagingFermentationResult fermentation = Equations.calcPackagingFermentationFromExtract(
				packageVol,
				fermentableExtract,
				new PercentageUnit(1D));

			totalCarb += fermentation.carbonation.get(Quantity.Unit.VOLUMES);
			carbEstimated = carbEstimated || fermentation.carbonation.isEstimated();
			totalAbv += fermentation.abvIncrease.get();
			abvEstimated = abvEstimated || fermentation.abvIncrease.isEstimated();

			log.addVerboseMessage(StringUtils.getProcessString(
				"package.krausen.carb.added",
				fermentation.carbonation.get(Quantity.Unit.VOLUMES)));

			log.addVerboseMessage(StringUtils.getProcessString(
				"package.krausen.abv.added",
				fermentation.abvIncrease.get(Quantity.Unit.PERCENTAGE_DISPLAY)));
		}

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.krausen.carb.final",
			totalCarb));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.krausen.abv.final",
			new PercentageUnit(totalAbv, abvEstimated).get(Quantity.Unit.PERCENTAGE_DISPLAY)));

		if (totalCarb > CarbonationCalculator.SAFE_PACKAGING_MAX_VOL)
		{
			log.addWarning(StringUtils.getProcessString(
				"package.krausen.high.carbonation",
				totalCarb));
		}

		DensityUnit outputGravity = calcKrausenConditionedPackageFg(
			beerVolumeAfterLoss,
			volumeIn.getGravity(),
			krausen,
			log);

		publishPackagedVolume(
			volumes, log, volumeIn, volumeInBefore, packageVol, totalCarb, carbEstimated,
			totalAbv, abvEstimated, blended, outputGravity);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Post-conditioning FG: blend finished packaged beer at its current FG with krausen
	 * liquid fully attenuated to its terminal FG (not {@link FermentationCalculator}
	 * on the whole package, which would incorrectly re-attenuate the main beer stream).
	 */
	private DensityUnit calcKrausenConditionedPackageFg(
		VolumeUnit beerVolumeAfterLoss,
		DensityUnit beerGravity,
		Volume krausen,
		ProcessLog log)
	{
		if (beerGravity == null || krausen.getVolume() == null)
		{
			return beerGravity;
		}

		DensityUnit krausenTerminal = FermentationCalculator.calcPredictedTerminalFg(
			krausen,
			Collections.emptyList(),
			log);
		if (krausenTerminal == null)
		{
			krausenTerminal = krausen.getGravity();
		}
		if (krausenTerminal == null)
		{
			return beerGravity;
		}

		return Equations.calcCombinedGravity(
			beerVolumeAfterLoss,
			beerGravity,
			krausen.getVolume(),
			krausenTerminal);
	}

	/*-------------------------------------------------------------------------*/
	private boolean validateKrausenSnapshot(Volume krausen, ProcessLog log)
	{
		if (krausen.getVolume() == null || krausen.getVolume().get() <= 0)
		{
			log.addError(StringUtils.getProcessString(
				"package.krausen.zero.volume",
				krausenVolumeName));
			return false;
		}

		Volume.Type type = krausen.getType();
		if (type != Volume.Type.WORT && type != Volume.Type.BEER)
		{
			log.addError(StringUtils.getProcessString(
				"package.krausen.invalid.type",
				krausenVolumeName,
				type));
			return false;
		}

		if (krausen.getGravity() == null)
		{
			log.addError(StringUtils.getProcessString(
				"package.krausen.no.gravity",
				krausenVolumeName));
			return false;
		}

		if (type == Volume.Type.BEER)
		{
			DensityUnit terminalFg = FermentationCalculator.calcPredictedTerminalFg(
				krausen,
				Collections.emptyList(),
				log);
			if (terminalFg == null)
			{
				log.addError(StringUtils.getProcessString(
					"package.krausen.no.terminal.fg",
					krausenVolumeName));
				return false;
			}

			if (krausen.getGravity().get(DensityUnit.Unit.GU)
				< terminalFg.get(DensityUnit.Unit.GU))
			{
				log.addError(StringUtils.getProcessString(
					"package.krausen.gravity.invalid",
					krausen.getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY),
					terminalFg.get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
				return false;
			}

			if (krausen.getGravity().get(DensityUnit.Unit.GU)
				<= terminalFg.get(DensityUnit.Unit.GU) + 0.5D)
			{
				log.addWarning(StringUtils.getProcessString("package.krausen.fully.attenuated"));
			}
		}

		return true;
	}

	/*-------------------------------------------------------------------------*/
	private WeightUnit calcKrausenFermentableExtract(Volume krausen, ProcessLog log)
	{
		VolumeUnit krausenVol = krausen.getVolume();
		DensityUnit gravity = krausen.getGravity();

		if (krausen.getType() == Volume.Type.WORT)
		{
			return Equations.calcFermentableExtractFromWort(
				krausenVol,
				gravity,
				krausen.getFermentability());
		}

		DensityUnit terminalFg = FermentationCalculator.calcPredictedTerminalFg(
			krausen,
			Collections.emptyList(),
			log);
		if (terminalFg == null)
		{
			log.addError(StringUtils.getProcessString(
				"package.krausen.no.terminal.fg",
				krausenVolumeName));
			return null;
		}

		return Equations.calcRemainingFermentableExtractInBeer(
			krausenVol,
			gravity,
			terminalFg);
	}

	/*-------------------------------------------------------------------------*/
	private void logKrausenSource(Volume krausen, ProcessLog log)
	{
		log.addVerboseMessage(StringUtils.getProcessString(
			"package.krausen.source.recipe",
			krausenRecipeName));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.krausen.source.volume",
			krausenVolumeName));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.krausen.source.type",
			krausen.getType()));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.krausen.source.amount",
			krausen.getVolume().get(Quantity.Unit.LITRES)));

		DensityUnit gravity = krausen.getGravity();
		if (gravity != null)
		{
			log.addVerboseMessage(StringUtils.getProcessString(
				"package.krausen.source.gravity",
				gravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}

		if (krausen.getType() == Volume.Type.BEER)
		{
			DensityUnit terminalFg = FermentationCalculator.calcPredictedTerminalFg(
				krausen,
				Collections.emptyList(),
				log);
			if (terminalFg != null)
			{
				log.addVerboseMessage(StringUtils.getProcessString(
					"package.krausen.source.terminal.fg",
					terminalFg.get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
			}
		}

		PercentageUnit abv = krausen.getAbv();
		if (abv != null)
		{
			log.addVerboseMessage(StringUtils.getProcessString(
				"package.krausen.source.abv",
				abv.get(Quantity.Unit.PERCENTAGE_DISPLAY)));
		}

		CarbonationUnit carb = krausen.getCarbonation();
		if (carb != null)
		{
			log.addVerboseMessage(StringUtils.getProcessString(
				"package.krausen.source.carbonation",
				carb.get(Quantity.Unit.VOLUMES)));
		}
	}

	/*-------------------------------------------------------------------------*/
	private void applySpunding(
		Volumes volumes,
		ProcessLog log,
		Volume volumeIn,
		VolumeUnit volumeInBefore,
		VolumeUnit volumeOut,
		double totalCarb,
		boolean carbEstimated,
		double totalAbv,
		boolean abvEstimated)
	{
		DensityUnit packagingGravity = volumeIn.getGravity();
		if (packagingGravity == null)
		{
			log.addError(StringUtils.getProcessString("package.spunding.no.packaging.gravity"));
			return;
		}

		DensityUnit predictedFinalGravity = FermentationCalculator.calcPredictedTerminalFg(
			volumeIn,
			getYeastAdditions(),
			log);

		if (predictedFinalGravity == null)
		{
			log.addError(StringUtils.getProcessString("package.spunding.no.predicted.fg"));
			return;
		}

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.spunding.packaging.gravity",
			packagingGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY)));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.spunding.predicted.fg",
			predictedFinalGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY)));

		if (packagingGravity.get(DensityUnit.Unit.GU)
			<= predictedFinalGravity.get(DensityUnit.Unit.GU))
		{
			log.addError(StringUtils.getProcessString(
				"package.spunding.gravity.invalid",
				packagingGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				predictedFinalGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
			return;
		}

		//
		// Packaging fermentation: 1 g fermentable extract → 0.5 g ethanol + 0.5 g CO₂ (× yield).
		// Remaining extract from gravity delta at pre-loss volume; CO₂/ABV per post-loss package volume.
		//
		WeightUnit remainingExtract = Equations.calcRemainingFermentableExtractInBeer(
			volumeInBefore,
			packagingGravity,
			predictedFinalGravity);

		if (remainingExtract.get(Quantity.Unit.KILOGRAMS) <= 0D)
		{
			log.addError(StringUtils.getProcessString("package.spunding.no.remaining.extract"));
			return;
		}

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.spunding.remaining.extract",
			remainingExtract.get(Quantity.Unit.KILOGRAMS)));

		PackagingFermentationResult fermentation = Equations.calcPackagingFermentationFromExtract(
			volumeOut,
			remainingExtract,
			new PercentageUnit(1D));

		totalCarb += fermentation.carbonation.get(Quantity.Unit.VOLUMES);
		carbEstimated = carbEstimated || fermentation.carbonation.isEstimated();
		totalAbv += fermentation.abvIncrease.get();
		abvEstimated = abvEstimated || fermentation.abvIncrease.isEstimated();

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.spunding.carb.added",
			fermentation.carbonation.get(Quantity.Unit.VOLUMES)));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.spunding.abv.added",
			fermentation.abvIncrease.get(Quantity.Unit.PERCENTAGE_DISPLAY)));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.spunding.carb.final",
			totalCarb));

		log.addVerboseMessage(StringUtils.getProcessString(
			"package.spunding.abv.final",
			totalAbv));

		publishPackagedVolume(
			volumes, log, volumeIn, volumeInBefore, volumeOut, totalCarb, carbEstimated,
			totalAbv, abvEstimated, null, predictedFinalGravity);
	}

	/*-------------------------------------------------------------------------*/
	private void publishPackagedVolume(
		Volumes volumes,
		ProcessLog log,
		Volume volumeIn,
		VolumeUnit volumeInBefore,
		VolumeUnit volumeOut,
		double totalCarb,
		boolean carbEstimated,
		double totalAbv,
		boolean abvEstimated,
		Volume speiseBlend,
		DensityUnit outputGravity)
	{
		CarbonationUnit carbonationOut = new CarbonationUnit(
			totalCarb, Quantity.Unit.VOLUMES, carbEstimated);
		PercentageUnit abvOut = new PercentageUnit(totalAbv, abvEstimated);

		Volume volOut;
		if (speiseBlend != null)
		{
			volOut = new Volume(
				getOutputVolume(),
				Volume.Type.BEER,
				volumeOut,
				speiseBlend.getTemperature(),
				volumeIn.getOriginalGravity(),
				volumeIn.getGravity(),
				abvOut,
				speiseBlend.getColour(),
				BitternessVolumes.zero());
			BitternessVolumes.copyAll(speiseBlend, volOut);
			HopAcidVolumes.copyAll(speiseBlend, volOut);
			PhVolumes.copyAll(speiseBlend, volOut);
			volOut.setIngredientAdditions(speiseBlend.getIngredientAdditions());
		}
		else
		{
			volOut = new Volume(
				getOutputVolume(),
				volumeIn.getType(),
				volumeIn.getMetrics(),
				volumeIn.getIngredientAdditions());

			volOut.setOriginalGravity(volumeIn.getOriginalGravity());
			volOut.setVolume(volumeOut);
		}

		volOut.setAbv(abvOut);
		volOut.setCarbonation(carbonationOut);

		if (speiseBlend == null)
		{
			volOut.setGravity(outputGravity != null ? outputGravity : volumeIn.getGravity());
			HopAcidVolumes.applyProportionalToVolume(volumeIn, volumeInBefore, volumeOut, volOut);
		}
		else if (outputGravity != null)
		{
			volOut.setGravity(outputGravity);
		}

		applyPackagingHopAdditions(volOut);

		BitternessVolumes.syncReportedDerived(
			volOut,
			Settings.parseReportedFormulas(Database.getInstance().getSettings()));

		if (volOut.getType() == Volume.Type.BEER)
		{
			Style style = Database.getInstance().getStyles().get(this.styleId);
			if (style != null)
			{
				volOut.setStyle(style);
				validateStyle(volOut, log, style);
			}
			else if (this.styleId != null)
			{
				log.addError(StringUtils.getProcessString("style.unknown", this.styleId));
			}
		}

		volumes.addOrUpdateOutputVolume(getOutputVolume(), volOut);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Folds packaging-step hop additions into the packaged volume hop-acid
	 * inventory (no boil isomerisation), matching {@link Ferment}.
	 */
	private void applyPackagingHopAdditions(Volume volOut)
	{
		if (getHopAdditions().isEmpty())
		{
			return;
		}

		List<Settings.HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());
		boolean reportSmph = reportedFormulas.contains(Settings.HopBitternessFormula.SMPH);

		for (HopAddition hop : getHopAdditions())
		{
			boolean preIsomerized = hop.getForm() != null
				&& hop.getForm().isPreIsomerized();
			if (preIsomerized)
			{
				HopAcidVolumes.add(volOut, Volume.Metric.ISO_ALPHA_ACIDS_MG,
					Equations.calcHopAlphaAcidsMg(hop));
			}
			else
			{
				HopAcidVolumes.addHopAlpha(volOut, hop);
			}

			if (reportSmph && !preIsomerized)
			{
				PhUnit beerPh = PhVolumes.getPrimary(volOut);
				BitternessUnit dryIbu = SmphEquations.calcDryHopIbuSmph(
					hop, volOut.getVolume(), beerPh);
				BitternessVolumes.add(
					volOut, Settings.HopBitternessFormula.SMPH, dryIbu);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return carbonation method to use for this apply (defaults to priming when unset).
	 */
	private CarbonationMethod validatePackagingConfiguration(ProcessLog log)
	{
		CarbonationMethod method = this.carbonationMethod;
		if (method == null)
		{
			log.addWarning(StringUtils.getProcessString("package.carbonation.method.missing"));
			method = CarbonationMethod.PRIMING_SUGAR;
		}

		int primingCount = countPrimingFermentables();
		int fermentableCount = getFermentableAdditions().size();

		if (method == CarbonationMethod.PRIMING_SUGAR)
		{
			if (primingCount == 0 && fermentableCount == 0)
			{
				log.addWarning(StringUtils.getProcessString("package.priming.no.fermentable"));
			}
			else if (primingCount == 0 && fermentableCount > 0)
			{
				log.addWarning(StringUtils.getProcessString("package.priming.fermentables.not.soluble"));
			}
		}
		else if (method == CarbonationMethod.FORCE_CARB)
		{
			if (this.forcedCarbonation == null)
			{
				log.addWarning(StringUtils.getProcessString("package.force.carb.no.target"));
			}
			if (primingCount > 0)
			{
				log.addWarning(StringUtils.getProcessString("package.force.carb.priming.ignored"));
			}
		}
		else if (method == CarbonationMethod.SPEISE)
		{
			if (speiseVolume == null)
			{
				log.addWarning(StringUtils.getProcessString("package.speise.not.set"));
			}
			if (primingCount > 0)
			{
				log.addWarning(StringUtils.getProcessString("package.speise.priming.ignored"));
			}
		}
		else if (method == CarbonationMethod.SPUNDING)
		{
			if (primingCount > 0)
			{
				log.addWarning(StringUtils.getProcessString("package.spunding.priming.ignored"));
			}
		}
		else if (method == CarbonationMethod.KRAUSENING)
		{
			if (krausenRecipeName == null || krausenVolumeName == null)
			{
				log.addWarning(StringUtils.getProcessString("package.krausen.not.set"));
			}
			if (primingCount > 0)
			{
				log.addWarning(StringUtils.getProcessString("package.krausen.priming.ignored"));
			}
		}

		if (method != CarbonationMethod.SPEISE && speiseVolume != null)
		{
			log.addWarning(StringUtils.getProcessString("package.speise.ignored"));
		}

		if (method != CarbonationMethod.KRAUSENING
			&& (krausenRecipeName != null || krausenVolumeName != null))
		{
			log.addWarning(StringUtils.getProcessString("package.krausen.ignored"));
		}

		warnPackagingCarbonationCombination(log, method);

		if (this.packagingType == PackagingType.CASK && method == CarbonationMethod.FORCE_CARB)
		{
			log.addWarning(StringUtils.getProcessString("package.warn.cask.force.carb"));
		}

		return method;
	}

	/*-------------------------------------------------------------------------*/
	private void warnPackagingCarbonationCombination(ProcessLog log, CarbonationMethod method)
	{
		if (this.packagingType != PackagingType.BOTTLE || method == null)
		{
			return;
		}
		String key = switch (method)
		{
			case FORCE_CARB -> "package.warn.bottle.force.carb";
			case SPEISE -> "package.warn.bottle.speise";
			case SPUNDING -> "package.warn.bottle.spunding";
			case KRAUSENING -> "package.warn.bottle.krausening";
			case PRIMING_SUGAR -> null;
		};
		if (key != null)
		{
			log.addWarning(StringUtils.getProcessString(key));
		}
	}

	/*-------------------------------------------------------------------------*/
	private int countPrimingFermentables()
	{
		int count = 0;
		for (FermentableAddition fa : getFermentableAdditions())
		{
			if (Equations.isPrimingFermentable(fa))
			{
				count++;
			}
		}
		return count;
	}

	/*-------------------------------------------------------------------------*/
	private void validateStyle(Volume beer, ProcessLog log, Style style)
	{
		DensityUnit fg = beer.getGravity();
		DensityUnit og = beer.getOriginalGravity();
		int ibu = (int)Math.round(beer.getBitterness().get(Quantity.Unit.IBU));
		int srm = (int)Math.round(beer.getColour().get(Quantity.Unit.SRM));
		PercentageUnit abv = beer.getAbv();
		CarbonationUnit carb = beer.getCarbonation();

		if (og.get() > style.getOgMax().get())
		{
			log.addWarning(StringUtils.getProcessString("style.og.too.high",
				og.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getOgMax().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}
		if (og.get() < style.getOgMin().get())
		{
			log.addWarning(StringUtils.getProcessString("style.og.too.low",
				og.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getOgMin().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}

		if (fg.get() > style.getFgMax().get())
		{
			log.addWarning(StringUtils.getProcessString("style.fg.too.high",
				fg.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getFgMax().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));

		}
		if (fg.get() < style.getFgMin().get())
		{
			log.addWarning(StringUtils.getProcessString("style.fg.too.low",
				fg.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getFgMin().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}
		
		if (ibu > style.getIbuMax().get(Quantity.Unit.IBU))
		{
			log.addWarning(StringUtils.getProcessString("style.ibu.too.high", ibu, style.getIbuMax().get()));
		}
		if (ibu < style.getIbuMin().get(Quantity.Unit.IBU))
		{
			log.addWarning(StringUtils.getProcessString("style.ibu.too.low", ibu, style.getIbuMin().get()));
		}
		
		if (srm > style.getColourMax().get(Quantity.Unit.SRM))
		{
			log.addWarning(StringUtils.getProcessString("style.srm.too.high", srm, style.getColourMax().get()));
		}
		if (srm < style.getColourMin().get(Quantity.Unit.SRM))
		{
			log.addWarning(StringUtils.getProcessString("style.srm.too.low", srm, style.getColourMin().get()));
		}

		if (abv.get(Quantity.Unit.PERCENTAGE) > style.getAbvMax().get(Quantity.Unit.PERCENTAGE))
		{
			log.addWarning(StringUtils.getProcessString("style.abv.too.high",
				abv.get(Quantity.Unit.PERCENTAGE_DISPLAY), style.getAbvMax().get(Quantity.Unit.PERCENTAGE_DISPLAY)));
		}
		if (abv.get(Quantity.Unit.PERCENTAGE) < style.getAbvMin().get(Quantity.Unit.PERCENTAGE))
		{
			log.addWarning(StringUtils.getProcessString("style.abv.too.low",
				abv.get(Quantity.Unit.PERCENTAGE_DISPLAY), style.getAbvMin().get(Quantity.Unit.PERCENTAGE_DISPLAY)));
		}

		if (carb.get(Quantity.Unit.VOLUMES) < style.getCarbMin().get(Quantity.Unit.VOLUMES))
		{
			log.addWarning(StringUtils.getProcessString("style.carb.too.low",
				carb.get(Quantity.Unit.VOLUMES), style.getCarbMin().get(Quantity.Unit.VOLUMES)));
		}
		if (carb.get(Quantity.Unit.VOLUMES) > style.getCarbMax().get(Quantity.Unit.VOLUMES))
		{
			log.addWarning(StringUtils.getProcessString("style.carb.too.high",
				carb.get(Quantity.Unit.VOLUMES), style.getCarbMax().get(Quantity.Unit.VOLUMES)));
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addOrUpdateOutputVolume(getOutputVolume(),
			new Volume(getOutputVolume(), Volume.Type.BEER));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("packagingType", String.valueOf(packagingType));
		result.put("carbonationMethod", String.valueOf(carbonationMethod));
		result.put("packagingLoss", packagingLoss == null ? "null" : packagingLoss.get(Quantity.Unit.MILLILITRES) + "ml");
		result.put("styleId", String.valueOf(styleId));
		result.put("forcedCarbonation", forcedCarbonation == null ? "null" : String.valueOf(forcedCarbonation.get(CarbonationUnit.Unit.VOLUMES)));
		result.put("speiseVolume", String.valueOf(speiseVolume));
		result.put("krausenRecipeName", String.valueOf(krausenRecipeName));
		result.put("krausenVolumeName", String.valueOf(krausenVolumeName));
		result.put("inputVolume", String.valueOf(getInputVolume()));
		result.put("outputVolume", String.valueOf(getOutputVolume()));
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("package.step.desc", getOutputVolume());
	}

	/*-------------------------------------------------------------------------*/
	public VolumeUnit getPackagingLoss()
	{
		return packagingLoss;
	}

	public void setPackagingLoss(VolumeUnit packagingLoss)
	{
		this.packagingLoss = packagingLoss;
	}

	public String getStyleId()
	{
		return styleId;
	}

	public void setStyleId(String styleId)
	{
		this.styleId = styleId;
	}

	public PackagingType getPackagingType()
	{
		return packagingType;
	}

	public void setPackagingType(
		PackagingType packagingType)
	{
		this.packagingType = packagingType;
	}

	public CarbonationMethod getCarbonationMethod()
	{
		return carbonationMethod;
	}

	public void setCarbonationMethod(CarbonationMethod carbonationMethod)
	{
		this.carbonationMethod = carbonationMethod;
	}

	public CarbonationUnit getForcedCarbonation()
	{
		return forcedCarbonation;
	}

	public void setForcedCarbonation(CarbonationUnit forcedCarbonation)
	{
		this.forcedCarbonation = forcedCarbonation;
	}

	public String getSpeiseVolume()
	{
		return speiseVolume;
	}

	public void setSpeiseVolume(String speiseVolume)
	{
		this.speiseVolume = speiseVolume;
	}

	public String getKrausenRecipeName()
	{
		return krausenRecipeName;
	}

	public void setKrausenRecipeName(String krausenRecipeName)
	{
		this.krausenRecipeName = krausenRecipeName;
	}

	public String getKrausenVolumeName()
	{
		return krausenVolumeName;
	}

	public void setKrausenVolumeName(String krausenVolumeName)
	{
		this.krausenVolumeName = krausenVolumeName;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(IngredientAddition.Type.values());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.FERMENTABLES ||
				ia.getType() == IngredientAddition.Type.MISC ||
				ia.getType() == IngredientAddition.Type.YEAST)
			{
				result.add(StringUtils.getDocString("package.fermentable.addition", ia.describe()));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		Volume outputVol = getRecipe().getVolumes().getVolume(this.getOutputVolume());
		result.add(StringUtils.getDocString("package.output.vol", outputVol.describe()));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		return new PackageStep(
			newName,
			this.getDescription(),
			cloneIngredients(getIngredientAdditions()),
			this.getInputVolume(),
			StringUtils.getProcessString("package.output", newName),
			new VolumeUnit(this.packagingLoss.get()),
			this.styleId,
			this.packagingType,
			this.carbonationMethod,
			this.forcedCarbonation == null ? null : new CarbonationUnit(this.forcedCarbonation),
			this.speiseVolume,
			this.krausenRecipeName,
			this.krausenVolumeName);
	}
}
