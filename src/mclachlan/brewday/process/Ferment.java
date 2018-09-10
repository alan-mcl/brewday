package mclachlan.brewday.process;

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Ferment extends ProcessStep
{
	private double targetGravity;

	public Ferment(String number, String name, String description,
		String inputVolume, double targetGravity)
	{
		super(number, name, description, inputVolume);
		this.targetGravity = targetGravity;
	}

	@Override
	public void apply(Volumes v)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double abvOut = Equations.calcAvbWithGravityChange(input.getGravity(), targetGravity);

		// todo: colour loss during fermentation?
		double colourOut = Equations.calcColourAfterFermentation(input.getColour());

		v.replaceVolume(
			getInputVolume(),
			new WortVolume(
				input.getVolume(),
				input.getTemperature(),
				targetGravity,
				input.getAbv() + abvOut,
				colourOut,
				input.getBitterness()));
	}
}
