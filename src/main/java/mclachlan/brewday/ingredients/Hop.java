/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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
		LEAF(1.00, 1.00, 1.00, 1.00, false),
		PLUG(1.02, 0.95, 0.95, 1.02, false),
		PELLET_T90(1.10, 0.70, 0.75, 1.08, false),
		CRYO(1.15, 0.35, 0.40, 1.15, false),
		CO2_EXTRACT(1.25, 0.00, 0.05, 1.25, false),
		ISOMERIZED_EXTRACT(1.00, 0.00, 0.00, 1.00, true);

		private final double utilisationMultiplier;
		private final double absorptionMultiplier;
		private final double particulateFraction;
		private final double alphaAvailability;
		private final boolean preIsomerized;

		Form(double utilisationMultiplier,
			double absorptionMultiplier,
			double particulateFraction,
			double alphaAvailability,
			boolean preIsomerized)
		{
			this.utilisationMultiplier = utilisationMultiplier;
			this.absorptionMultiplier = absorptionMultiplier;
			this.particulateFraction = particulateFraction;
			this.alphaAvailability = alphaAvailability;
			this.preIsomerized = preIsomerized;
		}

		public double getUtilisationMultiplier()
		{
			return utilisationMultiplier;
		}

		public double getAbsorptionMultiplier()
		{
			return absorptionMultiplier;
		}

		public double getParticulateFraction()
		{
			return particulateFraction;
		}

		public double getAlphaAvailability()
		{
			return alphaAvailability;
		}

		public boolean isPreIsomerized()
		{
			return preIsomerized;
		}

		/**
		 * Resolves a form name, supporting the legacy alias "PELLET" for
		 * backward-compatible deserialisation of existing data.
		 */
		public static Form fromString(String s)
		{
			if ("PELLET".equals(s))
			{
				return PELLET_T90;
			}
			return valueOf(s);
		}

		@Override
		public String toString()
		{
			return StringUtils.getUiString("hop.form." + name());
		}

		public Quantity.Unit getDefaultUnit()
		{
			switch (this)
			{
				case CO2_EXTRACT:
				case ISOMERIZED_EXTRACT:
					return Quantity.Unit.MILLILITRES;
				default:
					return Quantity.Unit.GRAMS;
			}
		}

		public Quantity.Type getQuantityType()
		{
			switch (this)
			{
				case CO2_EXTRACT:
				case ISOMERIZED_EXTRACT:
					return Quantity.Type.VOLUME;
				default:
					return Quantity.Type.WEIGHT;
			}
		}
	}
}
