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
import mclachlan.brewday.recipe.HopAdditionList;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration in minutes */
	private double duration;

	private String inputWortVolume;
	private String outputWortVolume;

	/** hops added at the start of this boil */
	private String hopAdditionVolume;

	public Boil(
		String name,
		String description,
		String inputWortVolume,
		String outputWortVolume,
		String hopAdditionVolume,
		double duration)
	{
		super(name, description, Type.BOIL);
		this.inputWortVolume = inputWortVolume;
		this.outputWortVolume = outputWortVolume;
		this.hopAdditionVolume = hopAdditionVolume;
		this.duration = duration;
	}

	public Boil(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BOIL), "Boil", Type.BOIL);

		// todo: find last wort vol?
		this.inputWortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT);
		this.outputWortVolume = getName()+" output";
		this.hopAdditionVolume = recipe.getVolumes().getVolumeByType(Volume.Type.HOPS);
		this.duration = 60;
	}

	@Override
	public List<String> apply(Volumes volumes, Recipe recipe)
	{
		WortVolume input = (WortVolume)(volumes.getVolume(inputWortVolume));

		// todo multiple hop additions
		HopAdditionList hopCharge = null;
		if (hopAdditionVolume != null)
		{
			hopCharge = (HopAdditionList)volumes.getVolume(hopAdditionVolume);
		}

		double tempOut = 100D;

		double volumeOut = input.getVolume() - (Const.BOIL_OFF_PER_HOUR * duration/60);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// todo: account for kettle caramelisation darkening?
		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		double bitternessOut = input.getBitterness();
		if (hopCharge != null)
		{
			 bitternessOut +=
				Equations.calcIbuTinseth(
					hopCharge,
					duration,
					(gravityOut + input.getGravity()) / 2,
					(volumeOut + input.getVolume()) / 2);
		}

		volumes.addVolume(
			outputWortVolume,
			new WortVolume(
				volumeOut,
				tempOut,
				gravityOut,
				abvOut,
				colourOut,
				bitternessOut));

		ArrayList<String> result = new ArrayList<String>();
		result.add(outputWortVolume);
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Boil '%s' for %.0f min", inputWortVolume, duration);
	}

	public Object getHopAdditionVolume()
	{
		return hopAdditionVolume;
	}

	public String getInputWortVolume()
	{
		return inputWortVolume;
	}

	public String getOutputWortVolume()
	{
		return outputWortVolume;
	}

	public double getDuration()
	{
		return duration;
	}

	public void setDuration(double duration)
	{
		this.duration = duration;
	}

	public void setInputWortVolume(String inputWortVolume)
	{
		this.inputWortVolume = inputWortVolume;
	}

	public void setOutputWortVolume(String outputWortVolume)
	{
		this.outputWortVolume = outputWortVolume;
	}

	public void setHopAdditionVolume(String hopAdditionVolume)
	{
		this.hopAdditionVolume = hopAdditionVolume;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Arrays.asList(inputWortVolume, hopAdditionVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Arrays.asList(outputWortVolume);
	}

}
