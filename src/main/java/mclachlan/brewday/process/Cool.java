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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Cool extends FluidVolumeProcessStep
{
	private TemperatureUnit targetTemp;

	/*-------------------------------------------------------------------------*/
	public Cool()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Cool(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TemperatureUnit targetTemp)
	{
		super(name, description, Type.COOL, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.targetTemp = targetTemp;
	}

	/*-------------------------------------------------------------------------*/
	public Cool(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.COOL), StringUtils.getProcessString("cool.desc"), Type.COOL, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(StringUtils.getProcessString("cool.output", getName()));
		targetTemp = new TemperatureUnit(20);
	}

	/*-------------------------------------------------------------------------*/
	public Cool(Cool step)
	{
		super(step.getName(), step.getDescription(), Type.COOL, step.getInputVolume(), step.getOutputVolume());

		this.targetTemp = step.targetTemp;
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

		TemperatureUnit tempDecrease = new TemperatureUnit(
			input.getTemperature().get(Quantity.Unit.CELSIUS)
				- targetTemp.get(Quantity.Unit.CELSIUS),
			Quantity.Unit.CELSIUS,
			false);

		VolumeUnit volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), tempDecrease);

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		Volume volOut = input.clone();

		volOut.setVolume(volumeOut);
		volOut.setTemperature(targetTemp);
		volOut.setGravity(gravityOut);
		volOut.setAbv(abvOut);
		volOut.setColour(colourOut);

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);
	}

	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString(
			"cool.step.desc",
			targetTemp.get(Quantity.Unit.CELSIUS));
	}

	public TemperatureUnit getTargetTemp()
	{
		return targetTemp;
	}

	public void setTargetTemp(TemperatureUnit targetTemp)
	{
		this.targetTemp = targetTemp;
	}

	@Override
	public List<String> getInstructions()
	{
		return List.of(
			StringUtils.getDocString(
				"cool.to",
				this.getInputVolume(),
				this.targetTemp.describe(Quantity.Unit.CELSIUS)));
	}

	@Override
	public ProcessStep clone()
	{
		return new Cool(
			this.getName(),
			this.getDescription(),
			this.getInputVolume(),
			this.getOutputVolume(),
			new TemperatureUnit(this.targetTemp.get()));
	}
}
