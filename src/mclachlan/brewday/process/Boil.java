package mclachlan.brewday.process;

import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration in minutes */
	private double duration;

	public Boil(String number, String name, String description, double duration)
	{
		super(number, name, description);
		this.duration = duration;
	}

	@Override
	public FluidVolume apply(FluidVolume input)
	{
		double tempOut = 100D;

		double volumeOut = input.getVolume() - (Const.BOIL_OFF_PER_HOUR * duration/60);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		return new FluidVolume(volumeOut, tempOut, gravityOut, abvOut);
	}
}
