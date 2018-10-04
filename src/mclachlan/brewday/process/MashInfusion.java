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
import mclachlan.brewday.recipe.WaterAddition;

/**
 * Represents an infusion into an existing mash.
 */
public class MashInfusion extends ProcessStep
{
	private String inputMashVolume;
	private String waterVol;
	private String outputMashVolume;

	/** duration in minutes */
	private double duration;

	// calculated from water infusion
	private double mashTemp;

	/*-------------------------------------------------------------------------*/
	public MashInfusion()
	{
	}

	/*-------------------------------------------------------------------------*/
	public MashInfusion(
		String name,
		String description,
		String inputMashVolume,
		String waterVol,
		String outputMashVolume,
		double duration)
	{
		super(name, description, Type.MASH_INFUSION);

		this.inputMashVolume = inputMashVolume;
		this.outputMashVolume = outputMashVolume;

		this.waterVol = waterVol;
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public MashInfusion(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH_INFUSION), "Mash infusion", Type.MASH_INFUSION);

		inputMashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH);
		waterVol = recipe.getVolumes().getVolumeByType(Volume.Type.WATER);
		duration = 60;

		outputMashVolume = getName()+" mash vol";
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, Recipe recipe,
		ErrorsAndWarnings log)
	{
		if (!volumes.contains(inputMashVolume))
		{
			log.addError("volume does not exist ["+inputMashVolume+"]");
			return;
		}
		if (!volumes.contains(waterVol))
		{
			log.addError("volume does not exist ["+waterVol+"]");
			return;
		}

		MashVolume inputMash = (MashVolume)volumes.getVolume(inputMashVolume);
		WaterAddition infusionWater = (WaterAddition)volumes.getVolume(waterVol);

		// todo: research mash infusion temp change: is treating it as two fluids valid?
		mashTemp = Equations.calcNewFluidTemperature(
			inputMash.getVolume(),
			inputMash.getTemperature(),
			infusionWater.getVolume(),
			infusionWater.getTemperature());

		// we're assuming no further absorption by grains happens
		double volumeOut = inputMash.getVolume() + infusionWater.getVolume();

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputMash.getVolume(),
			inputMash.getGravity(),
			volumeOut);

		double colourOut = Equations.calcColourWithVolumeChange(
			inputMash.getVolume(),
			inputMash.getColour(),
			volumeOut);

		String combinedWaterName = getName() + " combined water";

		WaterAddition combinedWater =
			inputMash.getWater().combineWith(
				combinedWaterName,
				infusionWater);

		volumes.addVolume(combinedWaterName, combinedWater);

		volumes.addVolume(
			outputMashVolume,
			new MashVolume(
				volumeOut,
				inputMash.getFermentables(),
				combinedWater,
				mashTemp,
				gravityOut,
				colourOut));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return String.format("Mash Infusion: '%s' @ %.1fC", getName(), mashTemp);
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public String getWaterVol()
	{
		return waterVol;
	}

	public double getDuration()
	{
		return duration;
	}

	public void setWaterVolume(String waterVolume)
	{
		this.waterVol = waterVolume;
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
		return Arrays.asList(inputMashVolume, waterVol);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Arrays.asList(outputMashVolume);
	}

	public String getInputMashVolume()
	{
		return inputMashVolume;
	}

	public void setInputMashVolume(String inputMashVolume)
	{
		this.inputMashVolume = inputMashVolume;
	}
}
