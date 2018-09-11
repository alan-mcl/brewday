package mclachlan.brewday.ingredients;

import mclachlan.brewday.process.Volume;

/**
 *
 */
public class Water implements Volume
{
	private String name;
	private double volume;
	private double temperature;

	public Water(double volume, double temperature)
	{
		this.volume = volume;
		this.temperature = temperature;
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
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("Water{");
		sb.append("volume=").append(volume);
		sb.append(", temperature=").append(temperature);
		sb.append('}');
		return sb.toString();
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
	public String describe()
	{
		return String.format("Water: %s, %.1fl at %.1fC", name, volume/1000, temperature);
	}
}
