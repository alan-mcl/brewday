package mclachlan.brewday.process;

/**
 *
 */
public class Volume
{
	/** volume in ml */
	protected double volume;
	/** temp in deg C */
	protected double temperature;

	public Volume(double temperature, double volume)
	{
		this.temperature = temperature;
		this.volume = volume;
	}

	public double getVolume()
	{
		return volume;
	}

	public double getTemperature()
	{
		return temperature;
	}
}
