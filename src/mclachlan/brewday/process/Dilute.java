package mclachlan.brewday.process;

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Dilute extends ProcessStep
{
	/** target volume in l*/
	private double volumeTarget;

	/** temp of water addition in deg C */
	private double additionTemp;

	protected Dilute(String number, String name, String description, double volumeTarget, double additionTemp)
	{
		super(number, name, description);
		this.volumeTarget = volumeTarget;
		this.additionTemp = additionTemp;
	}

	@Override
	public FluidVolume apply(FluidVolume input)
	{
		double volumeAddition = volumeTarget - input.getVolume();

		double volumeOut = volumeTarget;

		double tempOut = Equations.calcNewFluidTemperature(
			input.getVolume(), input.getTemperature(), volumeAddition, this.additionTemp);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		return new FluidVolume(volumeOut, tempOut, gravityOut, abvOut);
	}

}
