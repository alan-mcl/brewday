/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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

	/** equipment-profile kettle trub and chiller loss removed from outbound wort */
	private boolean removeTrubAndChillerLoss;

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
		this(name, description, inputVolume, outputVolume, targetTemp, false);
	}

	/*-------------------------------------------------------------------------*/
	public Cool(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TemperatureUnit targetTemp,
		boolean removeTrubAndChillerLoss)
	{
		super(name, description, Type.COOL, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.targetTemp = targetTemp;
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
	}

	/*-------------------------------------------------------------------------*/
	public Cool(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.COOL), StringUtils.getProcessString("cool.desc"), Type.COOL, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setOutputVolume(StringUtils.getProcessString("cool.output", getName()));
		targetTemp = new TemperatureUnit(20);
		this.removeTrubAndChillerLoss = false;
	}

	/*-------------------------------------------------------------------------*/
	public Cool(Cool step)
	{
		super(step.getName(), step.getDescription(), Type.COOL, step.getInputVolume(), step.getOutputVolume());

		this.targetTemp = step.targetTemp;
		this.removeTrubAndChillerLoss = step.removeTrubAndChillerLoss;
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

		if (!KettleTrubChillerLossSubtract.subtractIfEnabled(
			volOut, equipmentProfile, removeTrubAndChillerLoss, log))
		{
			return;
		}

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

	/*-------------------------------------------------------------------------*/
	public boolean isRemoveTrubAndChillerLoss()
	{
		return removeTrubAndChillerLoss;
	}

	public void setRemoveTrubAndChillerLoss(boolean removeTrubAndChillerLoss)
	{
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
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
	public ProcessStep clone(String newName)
	{
		return new Cool(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			StringUtils.getProcessString("cool.output", newName),
			new TemperatureUnit(this.targetTemp.get()),
			this.removeTrubAndChillerLoss);
	}
}
