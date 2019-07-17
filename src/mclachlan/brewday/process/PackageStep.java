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

import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Creates and output volume for this batch.
 */
public class PackageStep extends FluidVolumeProcessStep
{
	/** packaging loss in ml */
	private double packagingLoss;

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
		double packagingLoss)
	{
		super(name, description, Type.PACKAGE, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.packagingLoss = packagingLoss;
	}

	/*-------------------------------------------------------------------------*/
	public PackageStep(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.PACKAGE), "Package", Type.PACKAGE, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.BEER));
		setOutputVolume(getName()+" output");

		packagingLoss = 500;
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

		double volumeOut = input.getVolume() - packagingLoss;

		DensityUnit gravityOut = input.getGravity();

		double tempOut = input.getTemperature();

		// todo: carbonation change in ABV
		double abvOut = input.getAbv();

		double colourOut = input.getColour();

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
			log.addError("Unknown style ["+recipe.getStyle()+"]");
			return;
		}

		DensityUnit fg = beer.getGravity();
		DensityUnit og = beer.getOriginalGravity();
		int ibu = (int)Math.round(beer.getBitterness());
		int srm = (int)Math.round(beer.getColour());
		double abv = beer.getAbv();
		// todo: carbonation

		if (og.get() > style.getOgMax().get())
		{
			log.addWarning(String.format("OG (%.3f) too high for style max (%.3f)",
				og.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getOgMax().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}
		if (og.get() < style.getOgMin().get())
		{
			log.addWarning(String.format("OG (%.3f) too low for style min (%.3f)",
				og.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getOgMin().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}

		if (fg.get() > style.getFgMax().get())
		{
			log.addWarning(String.format("FG (%.3f) too high for style max (%.3f)",
				fg.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getFgMax().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));

		}
		if (fg.get() < style.getFgMin().get())
		{
			log.addWarning(String.format("FG (%.3f) too low for style min (%.3f)",
				fg.get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				style.getFgMin().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}
		
		if (ibu > style.getIbuMax())
		{
			log.addWarning(String.format("IBU (%d) too high for style max (%d)", ibu, style.getIbuMax()));
		}
		if (ibu < style.getIbuMin())
		{
			log.addWarning(String.format("IBU (%d) too low for style min (%d)", ibu, style.getIbuMin()));
		}
		
		if (srm > style.getColourMax())
		{
			log.addWarning(String.format("SRM (%d) too high for style max (%d)", srm, style.getColourMax()));
		}
		if (srm < style.getColourMin())
		{
			log.addWarning(String.format("SRM (%d) too low for style min (%d)", srm, style.getColourMin()));
		}

		if (abv > style.getAbvMax())
		{
			log.addWarning(String.format("ABV (%.1f%%) too high for style max (%.1f%%)",
				abv*100, style.getAbvMax()*100));
		}
		if (abv < style.getAbvMin())
		{
			log.addWarning(String.format("ABV (%.1f%%) too low for style min (%.1f%%)",
				abv*100, style.getAbvMin()*100));
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return String.format("Package '%s'", getOutputVolume());
	}

	public double getPackagingLoss()
	{
		return packagingLoss;
	}

	public void setPackagingLoss(double packagingLoss)
	{
		this.packagingLoss = packagingLoss;
	}
}
