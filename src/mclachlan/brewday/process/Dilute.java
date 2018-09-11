package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Dilute extends ProcessStep
{
	/** target volume label*/
	private double volumeTarget;

	/** temp of water addition in deg C */
	private double additionTemp;

	public Dilute(String name,
		String description,
		String inputVolume,
		String outputVolume,
		double volumeTarget,
		double additionTemp)
	{
		super(name, description, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.volumeTarget = volumeTarget;
		this.additionTemp = additionTemp;
	}

	@Override
	public java.util.List<String> apply(Volumes v)
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
		return String.format("Dilute '%s' to %.1fL", getInputVolume(), volumeTarget/1000);
	}

}
