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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Heat extends FluidVolumeProcessStep
{
	private TemperatureUnit targetTemp;
	private TimeUnit rampTime;
	private TimeUnit standTime;

	/*-------------------------------------------------------------------------*/
	public Heat()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Heat(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TemperatureUnit targetTemp,
		TimeUnit rampTime,
		TimeUnit standTime)
	{
		super(name, description, Type.HEAT, inputVolume, outputVolume);
		this.rampTime = rampTime;
		this.standTime = standTime;
		this.setOutputVolume(outputVolume);
		this.targetTemp = targetTemp;
	}

	/*-------------------------------------------------------------------------*/
	public Heat(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.HEAT), StringUtils.getProcessString("heat.desc"), Type.HEAT, null, null);

		// we guess that this is a temperature mash step
		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.MASH));
		setOutputVolume(StringUtils.getProcessString("heat.output", getName()));
		targetTemp = new TemperatureUnit(20, Quantity.Unit.CELSIUS);
		rampTime = new TimeUnit(5, Quantity.Unit.MINUTES);
		standTime = new TimeUnit(15, Quantity.Unit.MINUTES);
	}

	/*-------------------------------------------------------------------------*/
	public Heat(Heat step)
	{
		super(step.getName(), step.getDescription(), Type.HEAT, step.getInputVolume(), step.getOutputVolume());

		this.targetTemp = new TemperatureUnit(step.targetTemp);
		this.rampTime = new TimeUnit(step.rampTime);
		this.standTime = new TimeUnit(step.standTime);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume input = getInputVolume(volumes);

		TemperatureUnit tempIncrease = new TemperatureUnit(
			targetTemp.get(Quantity.Unit.CELSIUS) - input.getTemperature().get(Quantity.Unit.CELSIUS),
			Quantity.Unit.CELSIUS,
			false);

		// todo: heating volume change
		VolumeUnit volumeOut = new VolumeUnit(input.getVolume());
		DensityUnit gravityOut = new DensityUnit(input.getGravity());
		PercentageUnit abvOut = null;
		if (input.getAbv() != null)
		{
			abvOut = new PercentageUnit(input.getAbv());
		}
		ColourUnit colourOut = new ColourUnit(input.getColour());

		Volume volOut = input.clone();

		volOut.setVolume(volumeOut);
		volOut.setTemperature(targetTemp);
		volOut.setGravity(gravityOut);
		volOut.setAbv(abvOut);
		volOut.setColour(colourOut);

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString(
			"heat.step.desc",
			targetTemp.get(Quantity.Unit.CELSIUS));
	}

	/*-------------------------------------------------------------------------*/
	public TemperatureUnit getTargetTemp()
	{
		return targetTemp;
	}

	public void setTargetTemp(TemperatureUnit targetTemp)
	{
		this.targetTemp = targetTemp;
	}

	public TimeUnit getRampTime()
	{
		return rampTime;
	}

	public void setRampTime(TimeUnit rampTime)
	{
		this.rampTime = rampTime;
	}

	public TimeUnit getStandTime()
	{
		return standTime;
	}

	public void setStandTime(TimeUnit standTime)
	{
		this.standTime = standTime;
	}

	/*-------------------------------------------------------------------------*/


	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		result.add(StringUtils.getDocString(
			"heat.to",
			getInputVolume(),
			targetTemp.describe(Quantity.Unit.CELSIUS),
			getRampTime().describe(Quantity.Unit.MINUTES)));

		result.add(StringUtils.getDocString("heat.stand.time", getStandTime().describe(Quantity.Unit.MINUTES)));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone()
	{
		return new Heat(this);
	}
}
