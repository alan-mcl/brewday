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

package mclachlan.brewday.recipe;

import mclachlan.brewday.ingredients.Hop;

/**
 *
 */
public class HopAddition
{
	private Hop hop;

	/** weight in g */
	private double weight;

	public HopAddition(Hop hop, double weight)
	{
		this.hop = hop;
		this.weight = weight;
	}

	public Hop getHop()
	{
		return hop;
	}

	public double getWeight()
	{
		return weight;
	}

	public void setHop(Hop hop)
	{
		this.hop = hop;
	}

	public void setWeight(double weight)
	{
		this.weight = weight;
	}
}
