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

	public Boil(String number, String name, String description,
		String inputVolume, double duration)
	{
		super(number, name, description, inputVolume);
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

		// todo: account for boil darkening
		double colourOut = input.getColour();

		// todo: account for hop bittering
		double bitternessOut = input.getBitterness();

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
