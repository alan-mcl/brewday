package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class MashOut extends ProcessStep
{
	/** mash tun loss in ml */
	private double tunLoss;

	public MashOut(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double tunLoss)
	{
		super(name, description, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.tunLoss = tunLoss;
	}

	@Override
	public java.util.List<String> apply(Volumes v)
	{
		MashVolume input = (MashVolume)getInputVolume(v);

		double volumeOut =
			Equations.calcWortVolume(
				input.getGrainBill().getGrainWeight(),
				input.getWater().getVolume())
			- tunLoss;

		v.addVolume(
			getOutputVolume(),
			new WortVolume(
				volumeOut,
				input.getTemperature(),
				input.getGravity(),
				0D,
				input.getColour(),
				0D));

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Drain mash tun, '%s'", getInputVolume());
	}
}
