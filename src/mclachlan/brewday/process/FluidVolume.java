package mclachlan.brewday.process;

/**
 *
 */
public abstract class FluidVolume implements Volume
{
	private String name;
	/** volume in ml */
	private double volume;
	/** temp in deg C */
	private double temperature;
	/** gravity in GU */
	private double gravity;
	/** colour in SRM */
	private double colour;
	/** bitterness in IBU */
	private double bitterness;
	/** ABV in % */
	private double abv;

	public FluidVolume(double temperature,
		double colour, double bitterness, double gravity, double volume, double abv)
	{
		this.setTemperature(temperature);
		this.setColour(colour);
		this.setBitterness(bitterness);
		this.setGravity(gravity);
		this.setVolume(volume);
		this.setAbv(abv);
	}

	/** gravity in GU */
	public double getGravity()
	{
		return gravity;
	}

	/** colour in SRM */
	public double getColour()
	{
		return colour;
	}

	/** bitterness in IBU */
	public double getBitterness()
	{
		return bitterness;
	}

	/** volume in ml */
	public double getVolume()
	{
		return volume;
	}

	/** temp in deg C */
	public double getTemperature()
	{
		return temperature;
	}

	/** ABV in % */
	public double getAbv()
	{
		return abv;
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

	public void setVolume(double volume)
	{
		this.volume = volume;
	}

	public void setTemperature(double temperature)
	{
		this.temperature = temperature;
	}

	public void setGravity(double gravity)
	{
		this.gravity = gravity;
	}

	public void setColour(double colour)
	{
		this.colour = colour;
	}

	public void setBitterness(double bitterness)
	{
		this.bitterness = bitterness;
	}

	public void setAbv(double abv)
	{
		this.abv = abv;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("FluidVolume{");
		sb.append("name='").append(name).append('\'');
		sb.append(", volume=").append(volume);
		sb.append(", temperature=").append(temperature);
		sb.append(", gravity=").append(gravity);
		sb.append(", colour=").append(colour);
		sb.append(", bitterness=").append(bitterness);
		sb.append(", abv=").append(abv);
		sb.append('}');
		return sb.toString();
	}
}
