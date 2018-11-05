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
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Cool extends FluidVolumeProcessStep
{
	/** target temp in C */
	private double targetTemp;

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
		double targetTemp)
	{
		super(name, description, Type.COOL, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.targetTemp = targetTemp;
	}

	/*-------------------------------------------------------------------------*/
	public Cool(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.COOL), "Cool", Type.COOL, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(getName()+" output");
		targetTemp = 20;
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

		double volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), input.getTemperature() - targetTemp);

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		double colourOut = Equations.calcColourWithVolumeChange(
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
		return String.format("Cool: to %.1fC", targetTemp);
	}

	public double getTargetTemp()
	{
		return targetTemp;
	}

	public void setTargetTemp(double targetTemp)
	{
		this.targetTemp = targetTemp;
	}
}
