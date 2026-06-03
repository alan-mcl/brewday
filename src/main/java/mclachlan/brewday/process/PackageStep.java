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

	/*-------------------------------------------------------------------------*/
	public enum PackagingType
	{
		BOTTLE, KEG;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("package."+name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum CarbonationMethod
	{
		FORCE_CARB, PRIMING_SUGAR;

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
		super(name, description, Type.PACKAGE, inputVolume, outputVolume);
		setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
		this.styleId = styleId;
		this.packagingLoss = packagingLoss;
		this.packagingType = packagingType;
		this.carbonationMethod = carbonationMethod;
		this.forcedCarbonation = forcedCarbonation;
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
		// represent. NB: the package step does not currently fold this mass into the volume's hop-acid
		// inventory (see bug-backlog), so this is informational for now.
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
			// Packaging fermentation: assumes 100% attenuation of all priming fermentables.
			// Output carbonation and ABV represent finished conditioned beer, not beer
			// immediately after sugar addition. OG/FG on the output volume are unchanged.
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
		}

		carbonationOut = new CarbonationUnit(totalCarb, Quantity.Unit.VOLUMES, carbEstimated);
		abvOut = new PercentageUnit(totalAbv, abvEstimated);

		Volume volOut = new Volume(
			getOutputVolume(),
			volumeIn.getType(),
			volumeIn.getMetrics(),
			volumeIn.getIngredientAdditions());

		volOut.setOriginalGravity(volumeIn.getOriginalGravity());
		volOut.setVolume(volumeOut);
		volOut.setAbv(abvOut);
		volOut.setCarbonation(carbonationOut);

		//
		// IBU and hop-acid masses scale down with packaged volume so bitterness per litre stays consistent.
		//
		HopAcidVolumes.applyProportionalToVolume(volumeIn, volumeInBefore, volumeOut, volOut);
		BitternessVolumes.syncReportedDerived(
			volOut,
			Settings.parseReportedFormulas(Database.getInstance().getSettings()));

		//
		// When a BJCP style is linked, attach it and log warnings for out-of-range OG, FG, IBU, colour, ABV, or CO2.
		//
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

		//
		// Publish the packaged beer volume for reporting and export.
		//
		volumes.addOrUpdateOutputVolume(getOutputVolume(), volOut);
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

		return method;
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
			this.forcedCarbonation == null ? null : new CarbonationUnit(this.forcedCarbonation));
	}
}
