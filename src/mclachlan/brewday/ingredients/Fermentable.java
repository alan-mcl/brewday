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

import mclachlan.brewday.math.Equations;

/**
 *
 */
public class Fermentable
{
	private String name;

	/**
	 * Extract potential in USA units:
	 * GU that can be achieved with 1.00 pound (455 g) of malt mashed in 1.00 gallon (3.78 L) of water.
	 * source: https://byo.com/article/understanding-malt-spec-sheets-advanced-brewing/
	 */
	private double extractPotential;

	/** colour in SRM */
	private double colour;

	private String description;
	private Type type;
	private String origin;
	private String supplier;
	private double yield;
	private boolean addAfterBoil;
	private double coarseFineDiff;
	private double moisture;
	private double diastaticPower;
	private double protein;
	private double maxInBatch;
	private boolean recommendMash;
	private double ibuGalPerLb;

	public Fermentable()
	{
	}

	public double getExtractPotential()
	{
		return extractPotential;
	}

	public double getColour()
	{
		return colour;
	}

	public void setColour(double colour)
	{
		this.colour = colour;
	}

	@Override
	public String toString()
	{
		return String.format("%s", name);
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public void setOrigin(String origin)
	{
		this.origin = origin;
	}

	public String getOrigin()
	{
		return origin;
	}

	public void setSupplier(String supplier)
	{
		this.supplier = supplier;
	}

	public String getSupplier()
	{
		return supplier;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setYield(double yield)
	{
		this.yield = yield;
		this.extractPotential = Equations.calcExtractPotentialFromYield(yield);
	}

	public double getYield()
	{
		return yield;
	}

	public void setAddAfterBoil(boolean addAfterBoil)
	{
		this.addAfterBoil = addAfterBoil;
	}

	public boolean isAddAfterBoil()
	{
		return addAfterBoil;
	}

	public void setCoarseFineDiff(double coarseFineDiff)
	{
		this.coarseFineDiff = coarseFineDiff;
	}

	public double getCoarseFineDiff()
	{
		return coarseFineDiff;
	}

	public void setMoisture(double moisture)
	{
		this.moisture = moisture;
	}

	public double getMoisture()
	{
		return moisture;
	}

	public void setDiastaticPower(double diastaticPower)
	{
		this.diastaticPower = diastaticPower;
	}

	public double getDiastaticPower()
	{
		return diastaticPower;
	}

	public void setProtein(double protein)
	{
		this.protein = protein;
	}

	public double getProtein()
	{
		return protein;
	}

	public void setMaxInBatch(double maxInBatch)
	{
		this.maxInBatch = maxInBatch;
	}

	public double getMaxInBatch()
	{
		return maxInBatch;
	}

	public void setRecommendMash(boolean recommendMash)
	{
		this.recommendMash = recommendMash;
	}

	public boolean isRecommendMash()
	{
		return recommendMash;
	}

	public void setIbuGalPerLb(double ibuGalPerLb)
	{
		this.ibuGalPerLb = ibuGalPerLb;
	}

	public double getIbuGalPerLb()
	{
		return ibuGalPerLb;
	}

	public enum Type
	{
		GRAIN, SUGAR, LIQUID_EXTRACT, DRY_EXTRACT, ADJUNCT,
		JUICE, // not in the BeerXML spec but exported by BeerSmith anyway!
	}
}
