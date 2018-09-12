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
		super(temperature, colour, bitterness, gravity, volume, abv);
	}

	@Override
	public String describe()
	{
		return String.format("Beer '%s', %.1fl, %.1f%% ABV",
			getName(), getVolume()/1000, getAbv());
	}
}
