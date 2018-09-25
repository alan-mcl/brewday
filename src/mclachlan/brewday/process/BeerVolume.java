/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.process;

/**
 *
 */
public class BeerVolume extends FluidVolume
{
	public BeerVolume(double volume, double temperature, double gravity,
		double abv, double colour, double bitterness)
	{
		super(Type.BEER, temperature, colour, bitterness, gravity, volume, abv);
	}

	@Override
	public String describe()
	{
		return String.format(
			"Type: '%s'\n" +
				"Volume: %.1fl\n" +
				"Temperature: %.1fC\n" +
				"Gravity: %.1f\n" +
				"Colour: %.1f SRM\n" +
				"Bitterness: %.1f IBU\n" +
				"ABV: %.1f%%",
			getType().toString(),
			getVolume()/1000,
			getTemperature(),
			1000+getGravity(),
			getColour(),
			getBitterness(),
			getAbv());
	}
}
