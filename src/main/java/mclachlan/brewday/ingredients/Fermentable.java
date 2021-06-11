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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ingredients;

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.*;

/**
 *
 */
public class Fermentable implements V2DataObject
{
	private String name;
	private String description;

	private Type type;
	private String origin;
	private String supplier;
	private PercentageUnit yield;

	private ColourUnit colour;

	private boolean addAfterBoil;
	private PercentageUnit coarseFineDiff;
	private PercentageUnit moisture;
	private DiastaticPowerUnit diastaticPower;
	private PercentageUnit protein;
	private PercentageUnit maxInBatch;
	private boolean recommendMash;
	private double ibuGalPerLb;

	/** the pH that results when the grain is mashed in distilled water. */
	private PhUnit distilledWaterPh;

	/** the pH buffering capacity of this grain */
	private ArbitraryPhysicalQuantity bufferingCapacity;

	/** any lactic acid content of this fermentable i.e. acid malt*/
	private PercentageUnit lacticAcidContent;

	public Fermentable()
	{
	}

	public Fermentable(String name)
	{
		this.name = name;
	}

	/**
	 * Deep clone the other
	 */
	public Fermentable(Fermentable other)
	{
		this(other.name);

		this.description = other.description;
		this.colour = other.colour;
		this.type = other.type;
		this.origin = other.origin;
		this.supplier = other.supplier;
		this.yield = other.yield;
		this.addAfterBoil = other.addAfterBoil;
		this.coarseFineDiff = other.coarseFineDiff;
		this.moisture = other.moisture;
		this.diastaticPower = other.diastaticPower;
		this.bufferingCapacity = other.bufferingCapacity;
		this.protein = other.protein;
		this.maxInBatch = other.maxInBatch;
		this.recommendMash = other.recommendMash;
		this.ibuGalPerLb = other.ibuGalPerLb;
		this.lacticAcidContent = other.lacticAcidContent;
		this.distilledWaterPh = other.distilledWaterPh;
	}

	public ColourUnit getColour()
	{
		return colour;
	}

	public void setColour(ColourUnit colour)
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

	public void setYield(PercentageUnit yield)
	{
		this.yield = yield;
	}

	public PercentageUnit getYield()
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

	public void setCoarseFineDiff(PercentageUnit coarseFineDiff)
	{
		this.coarseFineDiff = coarseFineDiff;
	}

	public PercentageUnit getCoarseFineDiff()
	{
		return coarseFineDiff;
	}

	public void setMoisture(PercentageUnit moisture)
	{
		this.moisture = moisture;
	}

	public PercentageUnit getMoisture()
	{
		return moisture;
	}

	public void setDiastaticPower(DiastaticPowerUnit diastaticPower)
	{
		this.diastaticPower = diastaticPower;
	}

	public DiastaticPowerUnit getDiastaticPower()
	{
		return diastaticPower;
	}

	public ArbitraryPhysicalQuantity getBufferingCapacity()
	{
		return bufferingCapacity;
	}

	public void setBufferingCapacity(
		ArbitraryPhysicalQuantity bufferingCapacity)
	{
		this.bufferingCapacity = bufferingCapacity;
	}

	public void setProtein(PercentageUnit protein)
	{
		this.protein = protein;
	}

	public PercentageUnit getProtein()
	{
		return protein;
	}

	public void setMaxInBatch(PercentageUnit maxInBatch)
	{
		this.maxInBatch = maxInBatch;
	}

	public PercentageUnit getMaxInBatch()
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

	public PhUnit getDistilledWaterPh()
	{
		return distilledWaterPh;
	}

	public void setDistilledWaterPh(PhUnit distilledWaterPh)
	{
		this.distilledWaterPh = distilledWaterPh;
	}

	public PercentageUnit getLacticAcidContent()
	{
		return lacticAcidContent;
	}

	public void setLacticAcidContent(PercentageUnit acidContent)
	{
		this.lacticAcidContent = acidContent;
	}

	public enum Type
	{
		GRAIN (1),
		SUGAR (5),
		LIQUID_EXTRACT (2),
		DRY_EXTRACT (3),
		ADJUNCT (4),
		JUICE (5); // not in the BeerXML spec but exported by BeerSmith anyway!

		private final int sortOrder;

		Type(int sortOrder)
		{
			this.sortOrder = sortOrder;
		}

		public int getSortOrder()
		{
			return sortOrder;
		}

		@Override
		public String toString()
		{
			return StringUtils.getUiString("fermentable."+this.name());
		}
	}
}
