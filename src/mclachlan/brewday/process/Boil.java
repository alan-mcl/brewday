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
import mclachlan.brewday.ingredients.HopAddition;
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration in minutes */
	private double duration;

	/** hops added at the start of this boil */
	private String hopAdditionVol;

	public Boil(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		String hopAdditionVol,
		double duration)
	{
		super(name, description, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.hopAdditionVol = hopAdditionVol;
		this.duration = duration;
	}

	@Override
	public java.util.List<String> apply(Volumes volumes)
	{
		WortVolume input = (WortVolume)getInputVolume(volumes);
		HopAddition hopAddition = (HopAddition)volumes.getVolume(hopAdditionVol);

		double tempOut = 100D;

		double volumeOut = input.getVolume() - (Const.BOIL_OFF_PER_HOUR * duration/60);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// todo: account for kettle caramelisation darkening?
		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		// todo: account for hop bittering
		double bitternessOut = input.getBitterness() +
			Equations.calcIbuTinseth(
				hopAddition,
				duration,
				(gravityOut + input.getGravity()) /2,
				(volumeOut + input.getVolume()) /2);

		volumes.addVolume(
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
		return String.format("Boil '%s' %.0f min", getInputVolume(), duration);
	}
}
