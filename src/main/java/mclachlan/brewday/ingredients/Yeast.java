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
import mclachlan.brewday.math.TemperatureUnit;

/**
 *
 */
public class Yeast implements V2DataObject
{
	private String name;
	private String description;

	private Type type;
	private Form form;
	private String laboratory;
	private String productId;
	private PercentageUnit attenuation;
	private Flocculation flocculation;
	private TemperatureUnit minTemp;
	private TemperatureUnit maxTemp;
	private String recommendedStyles;

	public Yeast(Yeast other)
	{
		this.name = other.name;
		this.description = other.description;
		this.type = other.type;
		this.form = other.form;
		this.laboratory = other.laboratory;
		this.productId = other.productId;
		this.minTemp = other.minTemp;
		this.maxTemp = other.maxTemp;
		this.flocculation = other.flocculation;
		this.attenuation = other.attenuation;
		this.recommendedStyles = other.recommendedStyles;
	}

	public Yeast()
	{

	}

	public Yeast(String name)
	{
		this.name = name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
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

	public void setForm(Form form)
	{
		this.form = form;
	}

	public Form getForm()
	{
		return form;
	}

	public void setLaboratory(String laboratory)
	{
		this.laboratory = laboratory;
	}

	public String getLaboratory()
	{
		return laboratory;
	}

	public void setProductId(String productId)
	{
		this.productId = productId;
	}

	public String getProductId()
	{
		return productId;
	}

	public void setMinTemp(TemperatureUnit minTemp)
	{
		this.minTemp = minTemp;
	}

	public TemperatureUnit getMinTemp()
	{
		return minTemp;
	}

	public void setMaxTemp(TemperatureUnit maxTemp)
	{
		this.maxTemp = maxTemp;
	}

	public TemperatureUnit getMaxTemp()
	{
		return maxTemp;
	}

	public void setFlocculation(Flocculation flocculation)
	{
		this.flocculation = flocculation;
	}

	public Flocculation getFlocculation()
	{
		return flocculation;
	}

	public void setAttenuation(PercentageUnit attenuation)
	{
		this.attenuation = attenuation;
	}

	public PercentageUnit getAttenuation()
	{
		return attenuation;
	}

	public void setRecommendedStyles(String recommendedStyles)
	{
		this.recommendedStyles = recommendedStyles;
	}

	public String getRecommendedStyles()
	{
		return recommendedStyles;
	}

	public enum Type
	{
		ALE, LAGER, WHEAT, WINE, CHAMPAGNE;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("yeast.type."+name());
		}
	}

	public enum Form
	{
		LIQUID, DRY, SLANT, CULTURE;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("yeast.form."+name());
		}
	}

	public enum Flocculation
	{
		LOW, MEDIUM, HIGH, VERY_HIGH;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("yeast.flocculation."+name());
		}
	}
}
