package mclachlan.brewday.ingredients;

/**
 *
 */
public class Water
{
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
}
