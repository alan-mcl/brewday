package mclachlan.brewday.process;

/**
 *
 */
public class WortVolume extends Volume
{
	/** gravity in GU */
	private double gravity;

	/** ABV in % */
	private double abv;

	/** colour in SRM */
	private double colour;

	/** bitterness in IBU */
	private double bitterness;

	public WortVolume(double volume, double temperature, double gravity,
		double abv, double colour, double bitterness)
	{
		super(temperature, volume);
		this.gravity = gravity;
		this.abv = abv;
		this.colour = colour;
		this.bitterness = bitterness;
	}

	public double getGravity()
	{
		return gravity;
	}

	public double getAbv()
	{
		return abv;
	}

	public double getColour()
	{
		return colour;
	}

	public double getBitterness()
	{
		return bitterness;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("WortVolume{");
		sb.append("volume=").append(volume);
		sb.append(", temperature=").append(temperature);
		sb.append(", gravity=").append(gravity);
		sb.append(", abv=").append(abv);
		sb.append(", colour=").append(colour);
		sb.append(", bitterness=").append(bitterness);
		sb.append('}');
		return sb.toString();
	}
}
