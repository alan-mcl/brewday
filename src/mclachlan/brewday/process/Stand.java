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
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Stand extends FluidVolumeProcessStep
{
	/** stand duration in minutes */
	private double duration;

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

	public Stand(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.STAND), "Stand", Type.STAND, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(getName()+" output");

		duration = 30;
	}

	@Override
	public List<String> apply(Volumes v, Recipe recipe)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double tempOut = input.getTemperature() - (Const.HEAT_LOSS*duration/60D);

		double volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), input.getTemperature() - tempOut);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		// todo: account for hop stand bitterness
		double bitternessOut = input.getBitterness();

		v.addVolume(
			getOutputVolume(),
			new WortVolume(
				volumeOut,
				tempOut,
				gravityOut,
				abvOut,
				colourOut,
				bitternessOut));

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Stand '%s' %.0f min", getInputVolume(), duration);
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
