package mclachlan.brewday.process;

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Cool extends ProcessStep
{
	private double targetTemp;

	public Cool(String number, String name, String description, double targetTemp)
	{
		super(number, name, description);
		this.targetTemp = targetTemp;
	}

	@Override
	public FluidVolume apply(FluidVolume input)
	{
		double volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), input.getTemperature() - targetTemp);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		return new FluidVolume(volumeOut, targetTemp, gravityOut, abvOut);
	}
}
