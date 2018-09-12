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

package mclachlan.brewday.ingredients;

/**
 *
 */
public class Hop
{
	/** AA in % */
	private double alphaAcid;

	/** weight in g */
	private double weight;

	public Hop(double alphaAcid, double weight)
	{
		this.alphaAcid = alphaAcid;
		this.weight = weight;
	}

	public double getAlphaAcid()
	{
		return alphaAcid;
	}

	public double getWeight()
	{
		return weight;
	}
}
