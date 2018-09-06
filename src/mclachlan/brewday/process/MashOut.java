package mclachlan.brewday.process;

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class MashOut extends ProcessStep
{
	/** mash tun loss in ml */
	private double tunLoss;
	private String outputVolume;

	public MashOut(String number, String name, String description,
		String inputVolume, String outputVolume, double tunLoss)
	{
		super(number, name, description, inputVolume);
		this.outputVolume = outputVolume;
		this.tunLoss = tunLoss;
	}

	@Override
	public void apply(Volumes v)
	{
		MashVolume input = (MashVolume)getInputVolume(v);

		double volumeOut =
			Equations.calcWortVolume(
				input.getGrainBill().getGrainWeight(),
				input.getWater().getVolume())
			- tunLoss;

		v.addVolume(
			outputVolume,
			new WortVolume(
				volumeOut,
				input.getTemperature(),
				input.getGravity(),
				0D,
				input.getColour(),
				0D));
	}
}
