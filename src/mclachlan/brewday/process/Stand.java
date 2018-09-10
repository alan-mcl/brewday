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

	public Stand(String number, String name, String description,
		String inputVolume, double duration)
	{
		super(number, name, description, inputVolume);
		this.duration = duration;
	}

	@Override
	public void apply(Volumes v)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double tempOut = input.getTemperature() - (Const.HEAT_LOSS*duration/60D);

		double volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(), input.getTemperature() - tempOut);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		// todo: account for hop stand bitterness
		double bitternessOut = input.getBitterness();

		v.replaceVolume(
			getInputVolume(),
			new WortVolume(
				volumeOut,
				tempOut,
				gravityOut,
				abvOut,
				colourOut,
				bitternessOut));
	}
}
