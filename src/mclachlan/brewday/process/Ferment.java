package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Ferment extends ProcessStep
{
	private double targetGravity;

	public Ferment(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double targetGravity)
	{
		super(name, description, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.targetGravity = targetGravity;
	}

	@Override
	public java.util.List<String> apply(Volumes v)
	{
		WortVolume input = (WortVolume)getInputVolume(v);

		double abvOut = Equations.calcAvbWithGravityChange(input.getGravity(), targetGravity);

		// todo: colour loss during fermentation?
		double colourOut = Equations.calcColourAfterFermentation(input.getColour());

		v.addVolume(
			getOutputVolume(),
			new BeerVolume(
				input.getVolume(),
				input.getTemperature(),
				targetGravity,
				input.getAbv() + abvOut,
				colourOut,
				input.getBitterness()));

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Ferment '%s' to %.0f", getInputVolume(), 1000+targetGravity);
	}
}
