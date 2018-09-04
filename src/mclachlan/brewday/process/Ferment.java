package mclachlan.brewday.process;

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Ferment extends ProcessStep
{
	private double targetGravity;

	public Ferment(String number, String name, String description, double targetGravity)
	{
		super(number, name, description);
		this.targetGravity = targetGravity;
	}

	@Override
	public FluidVolume apply(FluidVolume input)
	{
		double abvOut = Equations.calcAvbWithGravityChange(input.getGravity(), targetGravity);

		return new FluidVolume(
			input.getVolume(),
			input.getTemperature(),
			targetGravity,
			input.getAbv() + abvOut);
	}
}
