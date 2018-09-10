package mclachlan.brewday.process;

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Cool extends ProcessStep
{
	private double targetTemp;

	public Cool(String number, String name, String description,
		String inputVolume, double targetTemp)
	{
		super(number, name, description, inputVolume);
		this.targetTemp = targetTemp;
	}

	@Override
	public void apply(Volumes v)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), input.getTemperature() - targetTemp);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		v.replaceVolume(
			getInputVolume(),
			new WortVolume(
				volumeOut,
				targetTemp,
				gravityOut,
				abvOut,
				colourOut,
				input.getBitterness()));
	}
}
