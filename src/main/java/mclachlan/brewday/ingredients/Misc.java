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
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;

/**
 *
 */
public class Misc implements V2DataObject
{
	private String name;
	private String description;
	private Type type;
	private Use use;
	private String usageRecommendation;
	private Quantity.Type measurementType;

	private WaterAdditionFormula waterAdditionFormula;
	/** only valid if the water addition formula is an ACID */
	private PercentageUnit acidContent;

	public Misc(Misc other)
	{
		this.name = other.name;
		this.description = other.description;
		this.type = other.type;
		this.use = other.use;
		this.usageRecommendation = other.usageRecommendation;
		this.measurementType = other.measurementType;
		this.waterAdditionFormula = other.waterAdditionFormula;
		this.acidContent = other.acidContent;
	}

	public Misc()
	{
	}

	public Misc(String name)
	{
		this.name = name;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public void setUse(Use use)
	{
		this.use = use;
	}

	public Use getUse()
	{
		return use;
	}

	public void setUsageRecommendation(String usageRecommendation)
	{
		this.usageRecommendation = usageRecommendation;
	}

	public String getUsageRecommendation()
	{
		return usageRecommendation;
	}

	public Quantity.Type getMeasurementType()
	{
		return measurementType;
	}

	public void setMeasurementType(Quantity.Type measurementType)
	{
		this.measurementType = measurementType;
	}

	public WaterAdditionFormula getWaterAdditionFormula()
	{
		return waterAdditionFormula;
	}

	public void setWaterAdditionFormula(
		WaterAdditionFormula waterAdditionFormula)
	{
		this.waterAdditionFormula = waterAdditionFormula;
	}

	public PercentageUnit getAcidContent()
	{
		return acidContent;
	}

	public void setAcidContent(PercentageUnit acidContent)
	{
		this.acidContent = acidContent;
	}

	public boolean isAcidAddition()
	{
		return this.getWaterAdditionFormula() == WaterAdditionFormula.LACTIC_ACID ||
			this.getWaterAdditionFormula() == WaterAdditionFormula.PHOSPHORIC_ACID;

	}

	/*-------------------------------------------------------------------------*/

	public enum Type
	{
		SPICE, FINING, WATER_AGENT, HERB, FLAVOUR, OTHER;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("misc.type."+name());
		}
	}

	public enum Use
	{
		BOIL, MASH, PRIMARY, SECONDARY, BOTTLING;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("misc.use."+name());
		}
	}

	public enum WaterAdditionFormula
	{
		CALCIUM_CARBONATE_UNDISSOLVED,
		CALCIUM_CARBONATE_DISSOLVED,
		CALCIUM_SULPHATE_DIHYDRATE,
		CALCIUM_CHLORIDE_DIHYDRATE,
		MAGNESIUM_SULFATE_HEPTAHYDRATE,
		SODIUM_BICARBONATE,
		SODIUM_CHLORIDE,
		CALCIUM_BICARBONATE,
		MAGNESIUM_CHLORIDE_HEXAHYDRATE,
		LACTIC_ACID,
		PHOSPHORIC_ACID
		;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("misc.water.addition.formula."+name());
		}
	}
}
