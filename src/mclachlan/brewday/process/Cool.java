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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Cool extends FluidVolumeProcessStep
{
	private double targetTemp;

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

	public Cool(Batch batch)
	{
		super(batch.getUniqueStepName(Type.COOL), "Cool", Type.COOL, null, null);

		setInputVolume(batch.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(getName()+" output");
		targetTemp = 20;
	}

	@Override
	public java.util.List<String> apply(Volumes v, Batch batch)
	{
		FluidVolume input = (FluidVolume)getInputVolume(v);

		double volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), input.getTemperature() - targetTemp);

		double gravityOut = Equations.calcGravityWithVolumeChange(
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

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Cool '%s' to %.1fC", getInputVolume(), targetTemp);
	}

	public double getTargetTemp()
	{
		return targetTemp;
	}
}
