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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
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
		String styleId)
	{
		super(name, description, Type.PACKAGE, inputVolume, outputVolume);
		setIngredients(ingredientAdditions);
		this.styleId = styleId;
		this.setOutputVolume(outputVolume);
		this.packagingLoss = packagingLoss;
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
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(PackageStep step)
	{
		super(step.getName(), step.getDescription(), Type.PACKAGE, step.getInputVolume(), step.getOutputVolume());

		this.packagingLoss = step.packagingLoss;
		this.styleId = step.styleId;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes v,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolume(v, log))
		{
			return;
		}

		Volume volumeIn = getInputVolume(v);

		VolumeUnit volumeOut = new VolumeUnit(
			volumeIn.getVolume().get()
				- packagingLoss.get());

		CarbonationUnit carbonation = volumeIn.getCarbonation();

		for (IngredientAddition ia : getIngredients())
		{
			if (ia instanceof FermentableAddition)
			{
				CarbonationUnit addedCarbonation = Equations.calcCarbonation(volumeIn.getVolume(), (FermentableAddition)ia);
				carbonation.set(carbonation.get() + addedCarbonation.get());
			}
		}

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
		volOut.setCarbonation(carbonation);


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

		v.addOrUpdateOutputVolume(getOutputVolume(), volOut);
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
		
		if (ibu > style.getIbuMax())
		{
			log.addWarning(StringUtils.getProcessString("style.ibu.too.high", ibu, style.getIbuMax()));
		}
		if (ibu < style.getIbuMin())
		{
			log.addWarning(StringUtils.getProcessString("style.ibu.too.low", ibu, style.getIbuMin()));
		}
		
		if (srm > style.getColourMax())
		{
			log.addWarning(StringUtils.getProcessString("style.srm.too.high", srm, style.getColourMax()));
		}
		if (srm < style.getColourMin())
		{
			log.addWarning(StringUtils.getProcessString("style.srm.too.low", srm, style.getColourMin()));
		}

		if (abv.get() > style.getAbvMax())
		{
			log.addWarning(StringUtils.getProcessString("style.abv.too.high",
				abv.get()*100, style.getAbvMax()*100));
		}
		if (abv.get() < style.getAbvMin())
		{
			log.addWarning(StringUtils.getProcessString("style.abv.too.low",
				abv.get()*100, style.getAbvMin()*100));
		}

		if (carb.get(Quantity.Unit.VOLUMES) < style.getCarbMin())
		{
			log.addWarning(StringUtils.getProcessString("style.carb.too.low",
				carb.get(Quantity.Unit.VOLUMES), style.getCarbMin()));
		}
		if (carb.get(Quantity.Unit.VOLUMES) > style.getCarbMax())
		{
			log.addWarning(StringUtils.getProcessString("style.carb.too.high",
				carb.get(Quantity.Unit.VOLUMES), style.getCarbMax()));
		}

	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("package.step.desc", getOutputVolume());
	}

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

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		// todo: yeast additions
		return Arrays.asList(
			IngredientAddition.Type.MISC,
			IngredientAddition.Type.FERMENTABLES);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredients())
		{
			if (ia.getType() == IngredientAddition.Type.FERMENTABLES || ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"package.fermentable.addition",
						ia.getQuantity().get(Quantity.Unit.GRAMS),
						ia.getName()));
			}
			else
			{
				throw new BrewdayException("Invalid "+ia.getType());
			}
		}

		Volume outputVol = getRecipe().getVolumes().getVolume(this.getOutputVolume());
		result.add(StringUtils.getDocString("package.output.vol", outputVol.describe()));

		return result;
	}

	@Override
	public ProcessStep clone()
	{
		return new PackageStep(
			this.getName(),
			this.getDescription(),
			cloneIngredients(getIngredients()),
			this.getInputVolume(),
			this.getOutputVolume(),
			new VolumeUnit(this.packagingLoss.get()),
			this.styleId);
	}
}
