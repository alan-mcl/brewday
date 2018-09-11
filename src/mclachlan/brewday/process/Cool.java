package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Cool extends ProcessStep
{
	private double targetTemp;

	public Cool(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double targetTemp)
	{
		super(name, description, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.targetTemp = targetTemp;
	}

	@Override
	public java.util.List<String> apply(Volumes v)
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

		v.addVolume(
			getOutputVolume(),
			new WortVolume(
				volumeOut,
				targetTemp,
				gravityOut,
				abvOut,
				colourOut,
				input.getBitterness()));

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Cool '%s' to %.1fC", getInputVolume(), targetTemp);
	}
}
