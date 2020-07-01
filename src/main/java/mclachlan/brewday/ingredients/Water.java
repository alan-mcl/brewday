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

import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
public class Water implements V2DataObject
{
	private String name;
	private String description;
	private double calcium;
	private double bicarbonate;
	private double sulfate;
	private double chloride;
	private double sodium;
	private double magnesium;
	private double ph;

	public Water()
	{
	}

	public Water(String name)
	{
		this.name = name;
	}

	/**
	 * Creates a deep clone.
	 */
	public Water(Water other)
	{
		this.name = other.name;
		this.description = other.description;
		this.calcium = other.calcium;
		this.bicarbonate = other.bicarbonate;
		this.sulfate = other.sulfate;
		this.chloride = other.chloride;
		this.sodium = other.sodium;
		this.magnesium = other.magnesium;
		this.ph = other.ph;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
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

	public void setCalcium(double calcium)
	{
		this.calcium = calcium;
	}

	public double getCalcium()
	{
		return calcium;
	}

	public void setBicarbonate(double bicarbonate)
	{
		this.bicarbonate = bicarbonate;
	}

	public double getBicarbonate()
	{
		return bicarbonate;
	}

	public void setSulfate(double sulfate)
	{
		this.sulfate = sulfate;
	}

	public double getSulfate()
	{
		return sulfate;
	}

	public void setChloride(double chloride)
	{
		this.chloride = chloride;
	}

	public double getChloride()
	{
		return chloride;
	}

	public void setSodium(double sodium)
	{
		this.sodium = sodium;
	}

	public double getSodium()
	{
		return sodium;
	}

	public void setMagnesium(double magnesium)
	{
		this.magnesium = magnesium;
	}

	public double getMagnesium()
	{
		return magnesium;
	}

	public void setPh(double ph)
	{
		this.ph = ph;
	}

	public double getPh()
	{
		return ph;
	}
}
