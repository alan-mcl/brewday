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
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.StringUtils;

/**
 *
 */
public class Hop implements V2DataObject
{
	private String name;
	private String description;
	private Type type;
	private Form form;
	private String origin;
	private String substitutes;
	private PercentageUnit alphaAcid;
	private PercentageUnit betaAcid;
	private PercentageUnit humulene;
	private PercentageUnit caryophyllene;
	private PercentageUnit cohumulone;
	private PercentageUnit myrcene;
	private PercentageUnit hopStorageIndex;

	public Hop()
	{
	}

	public Hop(Hop other)
	{
		this.name = other.name;
		this.description = other.description;
		this.type = other.type;
		this.form = other.form;
		this.origin = other.origin;
		this.substitutes = other.substitutes;
		this.alphaAcid = other.alphaAcid;
		this.betaAcid = other.betaAcid;
		this.humulene = other.humulene;
		this.caryophyllene = other.caryophyllene;
		this.myrcene = other.myrcene;
		this.cohumulone = other.cohumulone;
		this.hopStorageIndex = other.hopStorageIndex;
	}

	public Hop(String name)
	{
		this.name = name;
	}

	public PercentageUnit getAlphaAcid()
	{
		return alphaAcid;
	}

	public void setAlphaAcid(PercentageUnit alphaAcid)
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

	public void setHopStorageIndex(PercentageUnit hopStorageIndex)
	{
		this.hopStorageIndex = hopStorageIndex;
	}

	public PercentageUnit getHopStorageIndex()
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

	public Form getForm()
	{
		return form;
	}

	public void setForm(Form form)
	{
		this.form = form;
	}

	public void setBetaAcid(PercentageUnit betaAcid)
	{
		this.betaAcid = betaAcid;
	}

	public PercentageUnit getBetaAcid()
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

	public void setHumulene(PercentageUnit humulene)
	{
		this.humulene = humulene;
	}

	public PercentageUnit getHumulene()
	{
		return humulene;
	}

	public void setCaryophyllene(PercentageUnit caryophyllene)
	{
		this.caryophyllene = caryophyllene;
	}

	public PercentageUnit getCaryophyllene()
	{
		return caryophyllene;
	}

	public void setCohumulone(PercentageUnit cohumulone)
	{
		this.cohumulone = cohumulone;
	}

	public PercentageUnit getCohumulone()
	{
		return cohumulone;
	}

	public void setMyrcene(PercentageUnit myrcene)
	{
		this.myrcene = myrcene;
	}

	public PercentageUnit getMyrcene()
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
		BITTERING(1), AROMA(3), BOTH(2);

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
			return StringUtils.getUiString("hop."+name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum Form
	{
		PELLET, PLUG, LEAF;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("hop.form." + name());
		}

		public Quantity.Unit getDefaultUnit()
		{
			return Quantity.Unit.GRAMS;
		}

		public Quantity.Type getQuantityType()
		{
			return Quantity.Type.WEIGHT;
		}
	}
}
