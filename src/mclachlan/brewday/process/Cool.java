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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Cool extends FluidVolumeProcessStep
{
	/** target temp in C */
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
	public void apply(Volumes v, Recipe recipe,
		ErrorsAndWarnings log)
	{
		if (!validateInputVolume(v, log))
		{
			return;
		}

		FluidVolume input = (FluidVolume)getInputVolume(v);

		TemperatureUnit tempDecrease = new TemperatureUnit(
			input.getTemperature().get()
				- targetTemp.get());

		VolumeUnit volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), tempDecrease);

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		FluidVolume volOut;
		if (input instanceof WortVolume)
		{
			volOut = new WortVolume(
				volumeOut,
				targetTemp,
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
				targetTemp,
				((BeerVolume)input).getOriginalGravity(),
				gravityOut,
				abvOut,
				colourOut,
				input.getBitterness());
		}
		else
		{
			throw new BrewdayException("Invalid volume type "+input);
		}

		v.addVolume(
			getOutputVolume(),
			volOut);
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
}
