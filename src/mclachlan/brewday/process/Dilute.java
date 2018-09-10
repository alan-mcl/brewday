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

	public Dilute(String number, String name, String description,
		String inputVolume, double volumeTarget, double additionTemp)
	{
		super(number, name, description, inputVolume);
		this.volumeTarget = volumeTarget;
		this.additionTemp = additionTemp;
	}

	@Override
	public void apply(Volumes v)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double volumeAddition = volumeTarget - input.getVolume();

		double volumeOut = volumeTarget;

		double tempOut = Equations.calcNewFluidTemperature(
			input.getVolume(), input.getTemperature(), volumeAddition, this.additionTemp);

		double gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// assuming the water is at 0SRM
		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);


		// todo: account for bitterness reduction
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
