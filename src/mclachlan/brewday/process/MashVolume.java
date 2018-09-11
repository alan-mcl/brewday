package mclachlan.brewday.process;

import mclachlan.brewday.ingredients.GrainBill;
import mclachlan.brewday.ingredients.Water;

/**
 *
 */
public class MashVolume implements Volume
{
	private String name;

	/** volume in ml */
	private double volume;

	/** temp in deg C */
	private double temperature;

	/** grains in the mash */
	private GrainBill grainBill;

	/** water in the mash */
	private Water water;

	/** gravity in GU */
	private double gravity;

	/** colour in SRM */
	private double colour;

	public MashVolume(
		double volume,
		GrainBill grainBill,
		Water water,
		double temperature,
		double gravity,
		double colour)
	{
		this.temperature = temperature;
		this.volume = volume;
		this.grainBill = grainBill;
		this.water = water;
		this.gravity = gravity;
		this.colour = colour;
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

	public double getVolume()
	{
		return volume;
	}

	public double getTemperature()
	{
		return temperature;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("MashVolume{");
		sb.append("volume=").append(getVolume());
		sb.append(", temp=").append(getTemperature());
		sb.append(", grainBill=").append(grainBill);
		sb.append(", water=").append(water);
		sb.append(", gravity=").append(gravity);
		sb.append(", colour=").append(colour);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public String describe()
	{
		return String.format("Mash: '%s'", name);
	}
}
