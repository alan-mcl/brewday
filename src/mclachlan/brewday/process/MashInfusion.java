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
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
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
	private TemperatureUnit mashTemp;

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
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!volumes.contains(inputMashVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputMashVolume));
			return;
		}

		Volume inputMash = volumes.getVolume(inputMashVolume);
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
		VolumeUnit volumeOut = new VolumeUnit(
			inputMash.getVolume().get()
				+ infusionWater.getVolume().get());

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputMash.getVolume(),
			inputMash.getGravity(),
			volumeOut);

		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			inputMash.getVolume(),
			inputMash.getColour(),
			volumeOut);

		String combinedWaterName = StringUtils.getProcessString("mash.infusion.combined.water", getName());

		WaterAddition mashWater = (WaterAddition)inputMash.getIngredientAddition(IngredientAddition.Type.WATER);
		WaterAddition combinedWater = mashWater.getCombination(combinedWaterName, infusionWater);

		volumes.addVolume(
			outputMashVolume,
			new Volume(
				outputMashVolume,
				Volume.Type.MASH,
				volumeOut,
				inputMash.getIngredientAdditions(IngredientAddition.Type.FERMENTABLES),
				combinedWater,
				mashTemp,
				gravityOut,
				colourOut));
	}

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		recipe.getVolumes().addVolume(outputMashVolume, new Volume(Volume.Type.MASH));
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

	public TemperatureUnit getMashTemp()
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
