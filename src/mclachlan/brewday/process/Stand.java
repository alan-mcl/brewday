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

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Stand extends FluidVolumeProcessStep
{
	/** stand duration in minutes */
	private double duration;

	/*-------------------------------------------------------------------------*/
	public Stand()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Stand(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double duration)
	{
		super(name, description, Type.STAND, inputVolume, outputVolume);
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.STAND), StringUtils.getProcessString("stand.desc"), Type.STAND, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(StringUtils.getProcessString("stand.output", getName()));

		duration = 30;
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Stand step)
	{
		super(step.getName(), step.getDescription(), Type.STAND, step.getInputVolume(), step.getOutputVolume());

		this.duration = step.duration;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes v,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolume(v, log))
		{
			return;
		}

		Volume input = getInputVolume(v);

		TemperatureUnit tempOut = new TemperatureUnit(
			input.getTemperature().get(Quantity.Unit.CELSIUS) -
				(Const.HEAT_LOSS*duration/60D));

		VolumeUnit volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(),
			new TemperatureUnit(input.getTemperature().get(Quantity.Unit.CELSIUS)
				- tempOut.get(Quantity.Unit.CELSIUS)));

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		// todo: account for hop stand bitterness
		BitternessUnit bitternessOut = input.getBitterness();

		v.addVolume(
			getOutputVolume(),
			new Volume(
				getOutputVolume(),
				Volume.Type.WORT,
				volumeOut,
				tempOut,
				input.getFermentability(),
				gravityOut,
				abvOut,
				colourOut,
				bitternessOut));
	}

	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("stand.step.desc", duration);
	}

	public double getDuration()
	{
		return duration;
	}

	public void setDuration(double duration)
	{
		this.duration = duration;
	}
}
