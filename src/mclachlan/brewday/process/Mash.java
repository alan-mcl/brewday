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
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Convert;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.AdditionSchedule;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.FermentableAdditionList;
import mclachlan.brewday.recipe.WaterAddition;

public class Mash extends ProcessStep
{
	private String outputMashVolume;

	/** duration in minutes */
	private double duration;

	/** grain volume temp in C */
	private double grainTemp;

	// calculated from strike water
	private double mashTemp;

	/*-------------------------------------------------------------------------*/
	public Mash()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Mash(
		String name,
		String description,
		List<AdditionSchedule> mashAdditions,
		String outputMashVolume,
		double duration,
		double grainTemp)
	{
		super(name, description, Type.MASH);
		setIngredientAdditions(mashAdditions);

		this.outputMashVolume = outputMashVolume;
		this.duration = duration;
		this.grainTemp = grainTemp;
	}

	/*-------------------------------------------------------------------------*/
	public Mash(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH), "Initial mash infusion", Type.MASH);

		// todo: auto select unused grains and mash water volumes

		duration = 60;
		grainTemp = 20;

		outputMashVolume = getName()+" mash vol";
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log)
	{
		FermentableAdditionList grainBill = null;
		WaterAddition strikeWater = null;

		for (AdditionSchedule as : getIngredientAdditions())
		{
			if (!volumes.contains(as.getIngredientAddition()))
			{
				log.addError("Volume does not exist ["+as.getIngredientAddition()+"]");
				return;
			}

			Volume v = volumes.getVolume(as.getIngredientAddition());

			// seek the grains and water with the same time as the mash,
			// these are the initial combination

			if (as.getTime() == this.getDuration())
			{
				if (v instanceof FermentableAdditionList)
				{
					grainBill = (FermentableAdditionList)v;
				}
				else if (v instanceof WaterAddition)
				{
					strikeWater = (WaterAddition)v;
				}
			}
		}

		if (grainBill == null)
		{
			log.addError("No initial fermentable addition to mash");
			return;
		}
		if (strikeWater == null)
		{
			log.addError("No strike water for mash");
			return;
		}

		double grainWeight = grainBill.getCombinedWeight();

		mashTemp = Equations.calcMashTemp(grainBill, strikeWater, grainTemp);

		double volumeOut = Equations.calcMashVolume(grainWeight, strikeWater.getVolume());

		// source: https://byo.com/article/hitting-target-original-gravity-and-volume-advanced-homebrewing/
		double extractPoints = 0D;
		for (FermentableAddition g : grainBill.getIngredients())
		{
			extractPoints += Convert.gramsToLbs(g.getWeight()) * g.getFermentable().getExtractPotential();
		}

		// todo: externalise mash efficiency
		double actualExtract = extractPoints * Const.MASH_EFFICIENCY;

		DensityUnit gravityOut = new DensityUnit(actualExtract / Convert.mlToGallons(volumeOut));

		double colourOut = Equations.calcSrmMoreyFormula(grainBill, volumeOut);

		volumes.addVolume(
			outputMashVolume,
			new MashVolume(
				volumeOut,
				grainBill,
				strikeWater,
				mashTemp,
				gravityOut,
				colourOut));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return String.format("Mash: '%s'", getName());
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public double getDuration()
	{
		return duration;
	}

	public double getGrainTemp()
	{
		return grainTemp;
	}

	public void setGrainTemp(double grainTemp)
	{
		this.grainTemp = grainTemp;
	}

	public double getMashTemp()
	{
		return mashTemp;
	}

	public void setDuration(double duration)
	{
		this.duration = duration;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Arrays.asList();
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Arrays.asList(outputMashVolume);
	}

	@Override
	public List<Volume.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(Volume.Type.FERMENTABLES, Volume.Type.WATER);
	}
}
