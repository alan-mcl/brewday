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
public class HopAddition extends IngredientAddition
{
	private Hop hop;

	public HopAddition()
	{
	}

	public HopAddition(Hop hop, double weight, double time)
	{
		this.hop = hop;
		setWeight(weight);
		setTime(time);
	}

	public Hop getHop()
	{
		return hop;
	}

	public void setHop(Hop hop)
	{
		this.hop = hop;
	}

	@Override
	public String getName()
	{
		return hop.getName();
	}

	@Override
	public Type getType()
	{
		return Type.HOPS;
	}
}
