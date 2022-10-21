/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

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

	/** any forced carbonation */
	private CarbonationUnit forcedCarbonation;

	/*-------------------------------------------------------------------------*/
	public enum PackagingType
	{
		BOTTLE, KEG, KEG_WITH_PRIMING;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("package."+name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep()
	{
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
		CarbonationUnit forcedCarbonation)
	{
		super(name, description, Type.PACKAGE, inputVolume, outputVolume);
		setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
		this.styleId = styleId;
		this.packagingLoss = packagingLoss;
		this.packagingType = packagingType;
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

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.BEER));
		setOutputVolume(StringUtils.getProcessString("package.output", getName()));

		packagingLoss = new VolumeUnit(500);
		packagingType = PackagingType.BOTTLE;
		forcedCarbonation = null;
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(PackageStep other)
	{
		super(other.getName(), other.getDescription(), Type.PACKAGE, other.getInputVolume(), other.getOutputVolume());

		this.packagingLoss = other.packagingLoss;
		this.styleId = other.styleId;
		this.packagingType = other.packagingType;
		this.forcedCarbonation = other.forcedCarbonation;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume volumeIn = getInputVolume(volumes);

		VolumeUnit volumeOut = new VolumeUnit(
			volumeIn.getVolume().get()
				- packagingLoss.get());

		CarbonationUnit carbonationOut = volumeIn.getCarbonation();
		double totalCarb = carbonationOut.get(Quantity.Unit.VOLUMES);

		// Kegging sets the carbonation absolutely
		if (packagingType == PackagingType.KEG && this.forcedCarbonation != null)
		{
			totalCarb = this.forcedCarbonation.get(Quantity.Unit.VOLUMES);
		}

		// Carbonation from any fermentable additions
		//
		// This doesn't really work for a combination of forced carbonation and
		// carbonation fermentable additions: presumably the rate of forcing and
		// the rate of fermentation would intersect, and forced carbonation would
		// stop when it's target volume was reached, but fermentation wouldn't?
		//
		// Ignoring all that, someone else can do the PhD
		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia instanceof FermentableAddition)
			{
				CarbonationUnit addedCarbonation = Equations.calcCarbonation(volumeIn.getVolume(), (FermentableAddition)ia);
				totalCarb += addedCarbonation.get(Quantity.Unit.VOLUMES);
			}
		}

		carbonationOut = new CarbonationUnit(totalCarb, Quantity.Unit.VOLUMES, carbonationOut.isEstimated());

		// todo: carbonation change in ABV
		PercentageUnit abvOut = volumeIn.getAbv();

		Volume volOut = new Volume(
			getOutputVolume(),
			volumeIn.getType(),
			volumeIn.getMetrics(),
			volumeIn.getIngredientAdditions());

		volOut.setOriginalGravity(volumeIn.getOriginalGravity());
		volOut.setVolume(volumeOut);
		volOut.setAbv(abvOut);
		volOut.setCarbonation(carbonationOut);

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
	public ProcessStep clone()
	{
		return new PackageStep(
			this.getName(),
			this.getDescription(),
			cloneIngredients(getIngredientAdditions()),
			this.getInputVolume(),
			this.getOutputVolume(),
			new VolumeUnit(this.packagingLoss.get()),
			this.styleId,
			this.packagingType,
			this.forcedCarbonation);
	}
}
