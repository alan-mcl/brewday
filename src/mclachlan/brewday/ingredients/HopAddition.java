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

import java.util.*;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class HopAddition implements Volume
{
	private String name;

	private List<Hop> hops;

	public HopAddition(List<Hop> hops)
	{
		this.hops = hops;
	}

	public List<Hop> getHops()
	{
		return hops;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	public void setHops(List<Hop> hops)
	{
		this.hops = hops;
	}

	@Override
	public String describe()
	{
		return String.format("Hops: %s", name);
	}
}
