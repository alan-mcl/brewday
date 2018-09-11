package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Stand extends ProcessStep
{
	/** stand duration in minutes */
	private double duration;

	public Stand(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double duration)
	{
		super(name, description, inputVolume, outputVolume);
		this.duration = duration;
	}

	@Override
	public java.util.List<String> apply(Volumes v)
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
		return String.format("Stand '%s' %.0f min", getInputVolume(), duration);
	}
}
