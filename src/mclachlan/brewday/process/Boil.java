package mclachlan.brewday.process;

import mclachlan.brewday.ingredients.HopAddition;
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration in minutes */
	private double duration;

	/** hops added at the start of this boil */
	private HopAddition hopAddition;

	public Boil(
		String number,
		String name,
		String description,
		String inputVolume,
		HopAddition hopAddition,
		double duration)
	{
		super(number, name, description, inputVolume);
		this.hopAddition = hopAddition;
		this.duration = duration;
	}

	@Override
	public void apply(Volumes volumes)
	{
		WortVolume input = (WortVolume)getInputVolume(volumes);

		double tempOut = 100D;

		double volumeOut = input.getVolume() - (Const.BOIL_OFF_PER_HOUR * duration/60);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// todo: account for kettle caramelisation darkening?
		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		// todo: account for hop bittering
		double bitternessOut = input.getBitterness() +
			Equations.calcIbuTinseth(
				hopAddition,
				duration,
				(gravityOut + input.getGravity()) /2,
				(volumeOut + input.getVolume()) /2);

		volumes.replaceVolume(
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
