package mclachlan.brewday.process;

import mclachlan.brewday.ingredients.GrainBill;
import mclachlan.brewday.ingredients.Water;

/**
 *
 */
public class MashVolume extends Volume
{

	/** grains in the mash */
	private GrainBill grainBill;

	/** water in the mash */
	private Water water;

	/** gravity in GU */
	private double gravity;
	private double colour;

	public MashVolume(
		double volume,
		GrainBill grainBill,
		Water water,
		double temperature,
		double gravity)
	{
		super(temperature, volume);
		this.grainBill = grainBill;
		this.water = water;
		this.gravity = gravity;
	}

	public GrainBill getGrainBill()
	{
		return grainBill;
	}

	public Water getWater()
	{
		return water;
	}

	public double getGravity()
	{
		return gravity;
	}

	public double getColour()
	{
		return colour;
	}

	public void setColour(double colour)
	{
		this.colour = colour;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("MashVolume{");
		sb.append("grainBill=").append(grainBill);
		sb.append(", water=").append(water);
		sb.append(", gravity=").append(gravity);
		sb.append(", colour=").append(colour);
		sb.append('}');
		return sb.toString();
	}
}
