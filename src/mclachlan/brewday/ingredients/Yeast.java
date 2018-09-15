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

/**
 *
 */
public class Yeast
{
	private String name;
	private String description;
	private Type type;
	private Form form;
	private String laboratory;
	private String productId;
	private double minTemp;
	private double maxTemp;
	private Flocculation flocculation;
	private double attenuation;
	private String recommendedStyles;

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

	public void setMinTemp(double minTemp)
	{
		this.minTemp = minTemp;
	}

	public double getMinTemp()
	{
		return minTemp;
	}

	public void setMaxTemp(double maxTemp)
	{
		this.maxTemp = maxTemp;
	}

	public double getMaxTemp()
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

	public void setAttenuation(double attenuation)
	{
		this.attenuation = attenuation;
	}

	public double getAttenuation()
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
		ALE, LAGER, WHEAT, WINE, CHAMPAGNE
	}

	public enum Form
	{
		LIQUID, DRY, SLANT, CULTURE
	}

	public enum Flocculation
	{
		LOW, MEDIUM, HIGH, VERY_HIGH
	}
}
