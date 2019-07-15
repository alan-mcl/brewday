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

import mclachlan.brewday.math.DensityUnit;

/**
 *
 */
public class BeerVolume extends FluidVolume
{
	private DensityUnit originalGravity = new DensityUnit();

	/*-------------------------------------------------------------------------*/
	public BeerVolume()
	{
		super(Type.BEER);
	}

	/*-------------------------------------------------------------------------*/
	public BeerVolume(
		double volume,
		double temperature,
		DensityUnit originalGravity,
		DensityUnit gravity,
		double abv,
		double colour,
		double bitterness)
	{
		super(Type.BEER, temperature, colour, bitterness, gravity, volume, abv);
		this.originalGravity = originalGravity;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe()
	{
		return String.format(
			"%s: '%s'\n" +
				"Volume: %.1fl\n" +
				"OG: %.3f\n" +
				"FG: %.3f\n" +
				"Colour: %.1f SRM\n" +
				"Bitterness: %.1f IBU\n" +
				"ABV: %.1f%%",
			getType().toString(),
			getName(),
			getVolume()/1000,
			getOriginalGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY),
			getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY),
			getColour(),
			getBitterness(),
			getAbv()*100);
	}

	/*-------------------------------------------------------------------------*/

	public DensityUnit getOriginalGravity()
	{
		return originalGravity;
	}

	public void setOriginalGravity(DensityUnit originalGravity)
	{
		this.originalGravity = originalGravity;
	}
}
