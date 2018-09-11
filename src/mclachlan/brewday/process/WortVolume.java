package mclachlan.brewday.process;

/**
 *
 */
public class WortVolume extends FluidVolume
{
	public WortVolume(
		double volume,
		double temperature,
		double gravity,
		double abv,
		double colour,
		double bitterness)
	{
		super(temperature, colour, bitterness, gravity, volume, abv);
	}

	@Override
	public String describe()
	{
		return String.format("Wort: '%s', %.1fl at %.1fC", getName(), getVolume()/1000, getTemperature());
	}
}
