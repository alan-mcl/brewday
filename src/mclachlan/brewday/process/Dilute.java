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
public class Dilute extends FluidVolumeProcessStep
{
	/** target volume in ml*/
	private double volumeTarget;

	/** temp of water addition in deg C */
	private double additionTemp;

	public Dilute(String name,
		String description,
		String inputVolume,
		String outputVolume,
		double volumeTarget,
		double additionTemp)
	{
		super(name, description, Type.DILUTE, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.volumeTarget = volumeTarget;
		this.additionTemp = additionTemp;
	}

	public Dilute(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.DILUTE), "Dilute", Type.DILUTE, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(getName()+" output");

		if (getInputVolume() != null)
		{
			WortVolume wortV = (WortVolume)recipe.getVolumes().getVolume(getInputVolume());
			volumeTarget = wortV.getVolume() + 5000;
		}
		else
		{
			volumeTarget = 20000;
		}

		additionTemp = 20;
	}

	@Override
	public java.util.List<String> apply(Volumes v, Recipe recipe)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double volumeAddition = volumeTarget - input.getVolume();

		double volumeOut = volumeTarget;

		double tempOut = Equations.calcNewFluidTemperature(
			input.getVolume(), input.getTemperature(), volumeAddition, this.additionTemp);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// assuming the water is at 0SRM
		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);


		// todo: account for bitterness reduction
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
		return String.format("Dilute '%s' to %.1fL", getInputVolume(), volumeTarget/1000);
	}

	public double getAdditionTemp()
	{
		return additionTemp;
	}

	public double getVolumeTarget()
	{
		return volumeTarget;
	}

	public void setVolumeTarget(double volumeTarget)
	{
		this.volumeTarget = volumeTarget;
	}

	public void setAdditionTemp(double additionTemp)
	{
		this.additionTemp = additionTemp;
	}
}
