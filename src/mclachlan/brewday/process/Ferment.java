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
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Ferment extends FluidVolumeProcessStep
{
	private double targetGravity;

	public Ferment(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double targetGravity)
	{
		super(name, description, Type.FERMENT, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.targetGravity = targetGravity;
	}

	public Ferment(Batch batch)
	{
		super(batch.getUniqueStepName(Type.FERMENT), "Ferment", Type.FERMENT, null, null);

		setInputVolume(batch.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(getName()+" output");
	}

	@Override
	public java.util.List<String> apply(Volumes v, Batch batch)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double abvOut = Equations.calcAvbWithGravityChange(input.getGravity(), targetGravity);

		// todo: colour loss during fermentation?
		double colourOut = Equations.calcColourAfterFermentation(input.getColour());

		v.addVolume(
			getOutputVolume(),
			new BeerVolume(
				input.getVolume(),
				input.getTemperature(),
				targetGravity,
				input.getAbv() + abvOut,
				colourOut,
				input.getBitterness()));

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Ferment '%s' to %.0f", getInputVolume(), 1000+targetGravity);
	}

	public double getTargetGravity()
	{
		return targetGravity;
	}
}
