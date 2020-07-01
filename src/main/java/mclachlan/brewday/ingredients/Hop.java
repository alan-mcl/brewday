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
public class Hop implements V2DataObject
{
	private String name;
	private String description;
	/** AA in % */
	private double alphaAcid;
	private double hopStorageIndex;
	private Type type;
	private double betaAcid;
	private String substitutes;
	private String origin;
	private double humulene;
	private double caryophyllene;
	private double cohumulone;
	private double myrcene;

	public Hop()
	{

	}

	public double getAlphaAcid()
	{
		return alphaAcid;
	}

	public void setAlphaAcid(double alphaAcid)
	{
		this.alphaAcid = alphaAcid;
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

	public void setHopStorageIndex(double hopStorageIndex)
	{
		this.hopStorageIndex = hopStorageIndex;
	}

	public double getHopStorageIndex()
	{
		return hopStorageIndex;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public void setBetaAcid(double betaAcid)
	{
		this.betaAcid = betaAcid;
	}

	public double getBetaAcid()
	{
		return betaAcid;
	}

	public void setSubstitutes(String substitutes)
	{
		this.substitutes = substitutes;
	}

	public String getSubstitutes()
	{
		return substitutes;
	}

	public void setOrigin(String origin)
	{
		this.origin = origin;
	}

	public String getOrigin()
	{
		return origin;
	}

	public void setHumulene(double humulene)
	{
		this.humulene = humulene;
	}

	public double getHumulene()
	{
		return humulene;
	}

	public void setCaryophyllene(double caryophyllene)
	{
		this.caryophyllene = caryophyllene;
	}

	public double getCaryophyllene()
	{
		return caryophyllene;
	}

	public void setCohumulone(double cohumulone)
	{
		this.cohumulone = cohumulone;
	}

	public double getCohumulone()
	{
		return cohumulone;
	}

	public void setMyrcene(double myrcene)
	{
		this.myrcene = myrcene;
	}

	public double getMyrcene()
	{
		return myrcene;
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("Hop{");
		sb.append("name='").append(name).append('\'');
		sb.append('}');
		return sb.toString();
	}

	/*-------------------------------------------------------------------------*/
	public static enum Type
	{
		BITTERING, AROMA, BOTH
	}
}
