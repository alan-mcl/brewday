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
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;

/**
 *
 */
public class Water implements V2DataObject
{
	private String name;
	private String description;
	private PpmUnit calcium;
	private PpmUnit bicarbonate;
	private PpmUnit sulfate;
	private PpmUnit chloride;
	private PpmUnit sodium;
	private PpmUnit magnesium;
	private PhUnit ph;

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

	public Water(String name, PhUnit ph)
	{
		this.name = name;
		setPh(ph);
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

	public void setCalcium(PpmUnit calcium)
	{
		this.calcium = calcium;
	}

	public PpmUnit getCalcium()
	{
		return calcium;
	}

	public void setBicarbonate(PpmUnit bicarbonate)
	{
		this.bicarbonate = bicarbonate;
	}

	public PpmUnit getBicarbonate()
	{
		return bicarbonate;
	}

	public void setSulfate(PpmUnit sulfate)
	{
		this.sulfate = sulfate;
	}

	public PpmUnit getSulfate()
	{
		return sulfate;
	}

	public void setChloride(PpmUnit chloride)
	{
		this.chloride = chloride;
	}

	public PpmUnit getChloride()
	{
		return chloride;
	}

	public void setSodium(PpmUnit sodium)
	{
		this.sodium = sodium;
	}

	public PpmUnit getSodium()
	{
		return sodium;
	}

	public void setMagnesium(PpmUnit magnesium)
	{
		this.magnesium = magnesium;
	}

	public PpmUnit getMagnesium()
	{
		return magnesium;
	}

	public void setPh(PhUnit ph)
	{
		this.ph = ph;
	}

	public PhUnit getPh()
	{
		return ph;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return Alkalinity as ppm CaCO3
	 */
	public PpmUnit getAlkalinity()
	{
		return Equations.calcAlkalinitySimple(this);
	}

	/**
	 * Source: The Water Book p68
	 * @return The Kolbach RA as ppm CaCO3
	 */
	public PpmUnit getResidualAlkalinity()
	{
		return Equations.calcResidualAlkalinitySimple(this);
	}

	/*-------------------------------------------------------------------------*/
	public enum Component
	{
		CALCIUM, BICARBONATE, SULFATE, CHLORIDE, SODIUM, MAGNESIUM
	}
}
