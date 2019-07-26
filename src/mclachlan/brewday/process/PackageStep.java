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
import mclachlan.brewday.math.*;
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

	/*-------------------------------------------------------------------------*/
	public PackageStep()
	{
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		VolumeUnit packagingLoss)
	{
		super(name, description, Type.PACKAGE, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.packagingLoss = packagingLoss;
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.PACKAGE), StringUtils.getProcessString("package.desc"), Type.PACKAGE, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.BEER));
		setOutputVolume(StringUtils.getProcessString("package.output", getName()));

		packagingLoss = new VolumeUnit(500);
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(PackageStep step)
	{
		super(step.getName(), step.getDescription(), Type.PACKAGE, step.getInputVolume(), step.getOutputVolume());

		this.packagingLoss = step.packagingLoss;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes v, Recipe recipe,
		ErrorsAndWarnings log)
	{
		if (!validateInputVolume(v, log))
		{
			return;
		}

		FluidVolume input = (FluidVolume)getInputVolume(v);

		VolumeUnit volumeOut = new VolumeUnit(
			input.getVolume().get()
				- packagingLoss.get());

		DensityUnit gravityOut = input.getGravity();

		TemperatureUnit tempOut = new TemperatureUnit(input.getTemperature());

		// todo: carbonation change in ABV
		double abvOut = input.getAbv();

		ColourUnit colourOut = new ColourUnit(input.getColour());

		FluidVolume volOut;
		if (input instanceof WortVolume)
		{
			volOut = new WortVolume(
				volumeOut,
				tempOut,
				((WortVolume)input).getFermentability(),
				gravityOut,
				abvOut,
				colourOut,
				input.getBitterness());
		}
		else if (input instanceof BeerVolume)
		{
			volOut = new BeerVolume(
				volumeOut,
				tempOut,
				((BeerVolume)input).getOriginalGravity(),
				gravityOut,
				abvOut,
				colourOut,
				input.getBitterness());

			validateStyle(recipe, (BeerVolume)volOut, log);
		}
		else
		{
			throw new BrewdayException("Invalid volume type "+input);
		}

		v.addOutputVolume(getOutputVolume(), volOut);
	}

	/*-------------------------------------------------------------------------*/
	private void validateStyle(Recipe recipe, BeerVolume beer, ErrorsAndWarnings log)
	{
		Style style = Database.getInstance().getStyles().get(recipe.getStyle());

		if (style == null)
		{
			log.addError(StringUtils.getProcessString("style.unknown", recipe.getStyle()));
			return;
		}

		DensityUnit fg = beer.getGravity();
		DensityUnit og = beer.getOriginalGravity();
		int ibu = (int)Math.round(beer.getBitterness().get(Quantity.Unit.IBU));
		int srm = (int)Math.round(beer.getColour().get(Quantity.Unit.SRM));
		double abv = beer.getAbv();
		// todo: carbonation

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

		if (abv > style.getAbvMax())
		{
			log.addWarning(StringUtils.getProcessString("style.abv.too.high",
				abv*100, style.getAbvMax()*100));
		}
		if (abv < style.getAbvMin())
		{
			log.addWarning(StringUtils.getProcessString("style.abv.too.low",
				abv*100, style.getAbvMin()*100));
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

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		// todo: yeast additions, fermentable/carbonation additions
		return Collections.singletonList(IngredientAddition.Type.MISC);
	}
}
