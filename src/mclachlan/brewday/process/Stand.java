package mclachlan.brewday.process;

import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Stand extends ProcessStep
{
	/** stand duration in minutes */
	private double duration;

	protected Stand(String number, String name, String description)
	{
		super(number, name, description);
	}

	@Override
	public FluidVolume apply(FluidVolume input)
	{
		double tempOut = input.getTemperature() - (Const.HEAT_LOSS*duration/60D);

		double volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), input.getTemperature() - tempOut);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		return new FluidVolume(volumeOut, tempOut, gravityOut, abvOut);
	}
}
