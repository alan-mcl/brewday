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

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.DensityUnit;

/**
 *
 */
public class WortVolume extends FluidVolume
{
	private Fermentability fermentability;

	/*-------------------------------------------------------------------------*/
	public WortVolume()
	{
		super(Type.WORT);
	}

	/*-------------------------------------------------------------------------*/
	public WortVolume(
		double volume,
		double temperature,
		Fermentability fermentability,
		DensityUnit gravity,
		double abv,
		double colour,
		double bitterness)
	{
		super(Type.WORT, temperature, colour, bitterness, gravity, volume, abv);
		this.fermentability = fermentability;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe()
	{
		return
			StringUtils.getProcessString("volumes.wort.format",
				getType().toString(),
				getName(),
				getVolume()/1000,
				getTemperature(),
				getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY),
				getColour());
	}

	/*-------------------------------------------------------------------------*/

	public Fermentability getFermentability()
	{
		return fermentability;
	}

	public void setFermentability(Fermentability fermentability)
	{
		this.fermentability = fermentability;
	}

	/*-------------------------------------------------------------------------*/
	public static enum Fermentability
	{
		LOW, MEDIUM, HIGH
	}
}
