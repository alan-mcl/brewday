package mclachlan.brewday.process;

import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class BatchSparge extends ProcessStep
{
	private String drainedMash;
	private Water spargeWater;

	public BatchSparge(String number, String name, String description,
		String inputVolume, String drainedMash,
		Water spargeWater)
	{
		super(number, name, description, inputVolume);
		this.drainedMash = drainedMash;
		this.spargeWater = spargeWater;
	}

	@Override
	public void apply(Volumes volumes)
	{
		WortVolume input = (WortVolume)volumes.getVolume(getInputVolume());

		double volumeOut = input.getVolume() + spargeWater.getVolume();

		double tempOut =
			Equations.calcNewFluidTemperature(
				input.getVolume(),
				input.getTemperature(),
				spargeWater.getVolume(),
				spargeWater.getTemperature());

		// todo: incorrect, fix for sparging!
		double gravityOut = input.getGravity();

		// todo: incorrect, fix for sparging!
		double colourOut = input.getColour();

		volumes.replaceVolume(
			getInputVolume(),
			new WortVolume(
				volumeOut,
				tempOut,
				gravityOut,
				0D,
				colourOut,
				0D));
	}
}
