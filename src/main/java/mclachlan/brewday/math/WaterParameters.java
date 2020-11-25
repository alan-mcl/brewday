
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.math;

import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
public class WaterParameters implements V2DataObject
{
	private String name;
	private String description;

	// ion concentrations
	private PpmUnit minCalcium, maxCalcium;
	private PpmUnit minBicarbonate, maxBicarbonate;
	private PpmUnit minSulfate, maxSulfate;
	private PpmUnit minChloride, maxChloride;
	private PpmUnit minSodium, maxSodium;
	private PpmUnit minMagnesium, maxMagnesium;

	// alkalinity as ppm CaCO3
	private PpmUnit minAlkalinity, maxAlkalinity;
	private PpmUnit minResidualAlkalinity, maxResidualAlkalinity;

	public WaterParameters()
	{
	}

	public WaterParameters(
		String name, String description,
		PpmUnit minCalcium, PpmUnit maxCalcium,
		PpmUnit minBicarbonate, PpmUnit maxBicarbonate,
		PpmUnit minSulfate, PpmUnit maxSulfate,
		PpmUnit minChloride, PpmUnit maxChloride,
		PpmUnit minSodium, PpmUnit maxSodium,
		PpmUnit minMagnesium, PpmUnit maxMagnesium,
		PpmUnit minAlkalinity, PpmUnit maxAlkalinity,
		PpmUnit minResidualAlkalinity, PpmUnit maxResidualAlkalinity)
	{
		this.name = name;
		this.description = description;
		this.minCalcium = minCalcium;
		this.maxCalcium = maxCalcium;
		this.minBicarbonate = minBicarbonate;
		this.maxBicarbonate = maxBicarbonate;
		this.minSulfate = minSulfate;
		this.maxSulfate = maxSulfate;
		this.minChloride = minChloride;
		this.maxChloride = maxChloride;
		this.minSodium = minSodium;
		this.maxSodium = maxSodium;
		this.minMagnesium = minMagnesium;
		this.maxMagnesium = maxMagnesium;
		this.minAlkalinity = minAlkalinity;
		this.maxAlkalinity = maxAlkalinity;
		this.minResidualAlkalinity = minResidualAlkalinity;
		this.maxResidualAlkalinity = maxResidualAlkalinity;
	}

	public WaterParameters(WaterParameters other)
	{
		this.description = other.description;
		this.minCalcium = other.minCalcium;
		this.maxCalcium = other.maxCalcium;
		this.minBicarbonate = other.minBicarbonate;
		this.maxBicarbonate = other.maxBicarbonate;
		this.minSulfate = other.minSulfate;
		this.maxSulfate = other.maxSulfate;
		this.minChloride = other.minChloride;
		this.maxChloride = other.maxChloride;
		this.minSodium = other.minSodium;
		this.maxSodium = other.maxSodium;
		this.minMagnesium = other.minMagnesium;
		this.maxMagnesium = other.maxMagnesium;
		this.minAlkalinity = other.minAlkalinity;
		this.maxAlkalinity = other.maxAlkalinity;
		this.minResidualAlkalinity = other.minResidualAlkalinity;
		this.maxResidualAlkalinity = other.maxResidualAlkalinity;
	}

	public WaterParameters(String name)
	{
		this.name = name;
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

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public PpmUnit getMinCalcium()
	{
		return minCalcium;
	}

	public void setMinCalcium(PpmUnit minCalcium)
	{
		this.minCalcium = minCalcium;
	}

	public PpmUnit getMaxCalcium()
	{
		return maxCalcium;
	}

	public void setMaxCalcium(PpmUnit maxCalcium)
	{
		this.maxCalcium = maxCalcium;
	}

	public PpmUnit getMinBicarbonate()
	{
		return minBicarbonate;
	}

	public void setMinBicarbonate(PpmUnit minBicarbonate)
	{
		this.minBicarbonate = minBicarbonate;
	}

	public PpmUnit getMaxBicarbonate()
	{
		return maxBicarbonate;
	}

	public void setMaxBicarbonate(PpmUnit maxBicarbonate)
	{
		this.maxBicarbonate = maxBicarbonate;
	}

	public PpmUnit getMinSulfate()
	{
		return minSulfate;
	}

	public void setMinSulfate(PpmUnit minSulfate)
	{
		this.minSulfate = minSulfate;
	}

	public PpmUnit getMaxSulfate()
	{
		return maxSulfate;
	}

	public void setMaxSulfate(PpmUnit maxSulfate)
	{
		this.maxSulfate = maxSulfate;
	}

	public PpmUnit getMinChloride()
	{
		return minChloride;
	}

	public void setMinChloride(PpmUnit minChloride)
	{
		this.minChloride = minChloride;
	}

	public PpmUnit getMaxChloride()
	{
		return maxChloride;
	}

	public void setMaxChloride(PpmUnit maxChloride)
	{
		this.maxChloride = maxChloride;
	}

	public PpmUnit getMinSodium()
	{
		return minSodium;
	}

	public void setMinSodium(PpmUnit minSodium)
	{
		this.minSodium = minSodium;
	}

	public PpmUnit getMaxSodium()
	{
		return maxSodium;
	}

	public void setMaxSodium(PpmUnit maxSodium)
	{
		this.maxSodium = maxSodium;
	}

	public PpmUnit getMinMagnesium()
	{
		return minMagnesium;
	}

	public void setMinMagnesium(PpmUnit minMagnesium)
	{
		this.minMagnesium = minMagnesium;
	}

	public PpmUnit getMaxMagnesium()
	{
		return maxMagnesium;
	}

	public void setMaxMagnesium(PpmUnit maxMagnesium)
	{
		this.maxMagnesium = maxMagnesium;
	}

	public PpmUnit getMinAlkalinity()
	{
		return minAlkalinity;
	}

	public void setMinAlkalinity(PpmUnit minAlkalinity)
	{
		this.minAlkalinity = minAlkalinity;
	}

	public PpmUnit getMaxAlkalinity()
	{
		return maxAlkalinity;
	}

	public void setMaxAlkalinity(PpmUnit maxAlkalinity)
	{
		this.maxAlkalinity = maxAlkalinity;
	}

	public PpmUnit getMinResidualAlkalinity()
	{
		return minResidualAlkalinity;
	}

	public void setMinResidualAlkalinity(
		PpmUnit minResidualAlkalinity)
	{
		this.minResidualAlkalinity = minResidualAlkalinity;
	}

	public PpmUnit getMaxResidualAlkalinity()
	{
		return maxResidualAlkalinity;
	}

	public void setMaxResidualAlkalinity(
		PpmUnit maxResidualAlkalinity)
	{
		this.maxResidualAlkalinity = maxResidualAlkalinity;
	}
}
