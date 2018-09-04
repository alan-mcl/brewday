package mclachlan.brewday.process;

/**
 *
 */
public class FluidVolume
{
	/** volume in ml */
	private double volume;

	/** temp in deg C */
	private double temperature;

	/** gravity in GU */
	private double gravity;

	/** ABV in % */
	private double abv;

	public FluidVolume(double volume, double temperature, double gravity,
		double abv)
	{
		this.volume = volume;
		this.temperature = temperature;
		this.gravity = gravity;
		this.abv = abv;
	}

	public double getVolume()
	{
		return volume;
	}

	public double getTemperature()
	{
		return temperature;
	}

	public double getGravity()
	{
		return gravity;
	}

	public double getAbv()
	{
		return abv;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("FluidVolume{");
		sb.append("volume=").append(volume);
		sb.append(", temperature=").append(temperature);
		sb.append(", gravity=").append(gravity);
		sb.append(", abv=").append(abv);
		sb.append('}');
		return sb.toString();
	}
}
