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
public class GrainBill implements Volume
{
	private String name;

	private List<Fermentable> fermentables;

	public GrainBill(List<Fermentable> fermentables)
	{
		this.fermentables = fermentables;
	}

	public double getGrainWeight()
	{
		double result = 0D;

		for (Fermentable g : fermentables)
		{
			result += g.getWeight();
		}
		return result;
	}

	public List<Fermentable> getFermentables()
	{
		return fermentables;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("GrainBill{");
		sb.append("grains=").append(getGrainWeight());
		sb.append('}');
		return sb.toString();
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

	public void setFermentables(List<Fermentable> fermentables)
	{
		this.fermentables = fermentables;
	}

	@Override
	public String describe()
	{
		return String.format("Grains: %s, %.1fkg", name, getGrainWeight()/1000D);
	}
}
