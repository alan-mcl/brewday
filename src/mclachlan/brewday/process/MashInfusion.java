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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;

/**
 * Represents an infusion into an existing mash.
 */
public class MashInfusion extends ProcessStep
{
	private String inputMashVolume;
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
		String outputMashVolume,
		double duration)
	{
		super(name, description, Type.MASH_INFUSION);
		this.inputMashVolume = inputMashVolume;
		this.outputMashVolume = outputMashVolume;
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public MashInfusion(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH_INFUSION), StringUtils.getProcessString("mash.infusion.desc"), Type.MASH_INFUSION);

		inputMashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH);
		duration = 60;

		outputMashVolume = StringUtils.getProcessString("mash.mash.vol", getName());
	}

	/*-------------------------------------------------------------------------*/
	public MashInfusion(MashInfusion step)
	{
		super(step.getName(), step.getDescription(), Type.MASH_INFUSION);

		inputMashVolume = step.getInputMashVolume();
		duration = step.getDuration();
		outputMashVolume = step.getOutputMashVolume();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, Recipe recipe,
		ErrorsAndWarnings log)
	{
		if (!volumes.contains(inputMashVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputMashVolume));
			return;
		}

		MashVolume inputMash = (MashVolume)volumes.getVolume(inputMashVolume);
		WaterAddition infusionWater;
		IngredientAddition rli = getIngredientAddition(IngredientAddition.Type.WATER);

		if (rli == null)
		{
			log.addError(StringUtils.getProcessString("mash.infusion.no.water.addition"));
			return;
		}
		else
		{
			infusionWater = (WaterAddition)rli;
		}

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

		String combinedWaterName = StringUtils.getProcessString("mash.infusion.combined.water", getName());

		WaterAddition combinedWater =
			inputMash.getWater().getCombination(
				combinedWaterName,
				infusionWater);

		volumes.addVolume(
			outputMashVolume,
			new MashVolume(
				volumeOut,
				inputMash.getFermentables(),
				combinedWater,
				mashTemp,
				gravityOut,
				colourOut,
				0));
	}

	@Override
	public void dryRun(Recipe recipe, ErrorsAndWarnings log)
	{
		recipe.getVolumes().addVolume(outputMashVolume, new MashVolume());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("mash.infusion.step.desc", getName(), mashTemp);
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public double getDuration()
	{
		return duration;
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
		return Collections.singletonList(inputMashVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Collections.singletonList(outputMashVolume);
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
