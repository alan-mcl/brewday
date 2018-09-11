package mclachlan.brewday.process;

/**
 *
 */
public class BeerVolume extends FluidVolume
{
	public BeerVolume(double volume, double temperature, double gravity,
		double abv, double colour, double bitterness)
	{
		super(temperature, colour, bitterness, gravity, volume, abv);
	}

	@Override
	public String describe()
	{
		return String.format("Beer '%s', %.1fl, %.1f%% ABV",
			getName(), getVolume()/1000, getAbv());
	}
}
