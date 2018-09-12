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
public class Grain
{
	/**
	 * Extract potential in USA units:
	 * GU that can be achieved with 1.00 pound (455 g) of malt mashed in 1.00 gallon (3.78 L) of water.
	 * source: https://byo.com/article/understanding-malt-spec-sheets-advanced-brewing/
	 */
	private double extractPotential;

	/** colour in SRM */
	private double colour;

	/** weight in g */
	private double weight;

	public Grain(double extractPotential, double colour, double weight)
	{
		this.extractPotential = extractPotential;
		this.colour = colour;
		this.weight = weight;
	}

	public double getExtractPotential()
	{
		return extractPotential;
	}

	public double getColour()
	{
		return colour;
	}

	public double getWeight()
	{
		return weight;
	}

	public void setWeight(double weight)
	{
		this.weight = weight;
	}
}
