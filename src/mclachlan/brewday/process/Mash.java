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
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.AdditionSchedule;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.FermentableAdditionList;
import mclachlan.brewday.recipe.WaterAddition;

public class Mash extends ProcessStep
{
	private String outputFirstRunnings;
	private String outputMashVolume;

	/** duration in minutes */
	private double duration;

	/** grain volume temp in C */
	private double grainTemp;

	/** mash tun loss in ml */
	private double tunLoss;

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
		String outputFirstRunnings,
		double duration,
		double grainTemp,
		double tunLoss)
	{
		super(name, description, Type.MASH);
		this.outputFirstRunnings = outputFirstRunnings;
		this.tunLoss = tunLoss;
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
		tunLoss = 3000;

		outputMashVolume = getName()+" mash vol";
		outputFirstRunnings = getName()+" first runnings";
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

		MashVolume mashVolumeOut = getMashVolumeOut(grainBill, strikeWater);
		volumes.addVolume(outputMashVolume, mashVolumeOut);

		WortVolume firstRunningsOut = getFirstRunningsOut(mashVolumeOut, grainBill);
		volumes.addVolume(outputFirstRunnings, firstRunningsOut);
	}

	/*-------------------------------------------------------------------------*/
	private WortVolume getFirstRunningsOut(
		MashVolume mashVolume,
		FermentableAdditionList ingredientAddition)
	{
		double grainWeight = 0D;
		for (FermentableAddition fermentableAddition : ingredientAddition.getIngredients())
		{
			grainWeight += fermentableAddition.getWeight();
		}

		double volumeOut =
			Equations.calcWortVolume(
				grainWeight,
				mashVolume.getWater().getVolume())
			- tunLoss;

		WortVolume.Fermentability fermentabilityOut;
		if (mashVolume.getTemperature() < 65.5D)
		{
			fermentabilityOut = WortVolume.Fermentability.HIGH;
		}
		else if (mashVolume.getTemperature() < 67.5D)
		{
			fermentabilityOut = WortVolume.Fermentability.MEDIUM;
		}
		else
		{
			fermentabilityOut = WortVolume.Fermentability.LOW;
		}

		return new WortVolume(
			volumeOut,
			mashVolume.getTemperature(),
			fermentabilityOut,
			mashVolume.getGravity(),
			0D,
			mashVolume.getColour(),
			0D);
	}

	/*-------------------------------------------------------------------------*/
	private MashVolume getMashVolumeOut(
		FermentableAdditionList grainBill,
		WaterAddition strikeWater)
	{
		double grainWeight = grainBill.getCombinedWeight();

		mashTemp = Equations.calcMashTemp(grainBill, strikeWater, grainTemp);

		double volumeOut = Equations.calcMashVolume(grainWeight, strikeWater.getVolume());

		DensityUnit gravityOut = Equations.calcMashExtractContent(grainBill, strikeWater);

		double colourOut = Equations.calcSrmMoreyFormula(grainBill, volumeOut);

		return new MashVolume(
			volumeOut,
			grainBill,
			strikeWater,
			mashTemp,
			gravityOut,
			colourOut,
			tunLoss);
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

	public String getOutputFirstRunnings()
	{
		return outputFirstRunnings;
	}

	public void setOutputFirstRunnings(String outputFirstRunnings)
	{
		this.outputFirstRunnings = outputFirstRunnings;
	}

	public void setOutputMashVolume(String outputMashVolume)
	{
		this.outputMashVolume = outputMashVolume;
	}

	public double getTunLoss()
	{
		return tunLoss;
	}

	public void setTunLoss(double tunLoss)
	{
		this.tunLoss = tunLoss;
	}

	public void setMashTemp(double mashTemp)
	{
		this.mashTemp = mashTemp;
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
