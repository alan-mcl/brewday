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

package mclachlan.brewday.style;

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.*;

/**
 *
 */
public class Style implements V2DataObject
{
	/** Unique name for Brewday, eg "19A American Amber Ale"*/
	private String name;

	/** Name as per the style guide, eg "American Amber Ale" */
	private String styleGuideName;

	/** Category name as per the style guide, eg "Amber and Brown American Beer" */
	private String category;

	/** Category number as per the style guide, eg "19" */
	private String categoryNumber;

	/** Category letter as per the style guide, eg "A"*/
	private String styleLetter;

	/** The style guide that contains this style, eg "BJCP 2015"*/
	private String styleGuide;

	/** The type of style, as per the BeerXML definitions */
	private Type type;

	/** Min OG in SG */
	private DensityUnit ogMin;
	/** Max OG in SG */
	private DensityUnit ogMax;
	/** Min FG in SG */
	private DensityUnit fgMin;
	/** Max FG in SG */
	private DensityUnit fgMax;
	/** Min bitterness in IBU*/
	private BitternessUnit ibuMin;
	/** Max bitterness in IBU*/
	private BitternessUnit ibuMax;
	/** Min colour in SRM*/
	private ColourUnit colourMin;
	/** Max colour in SRM*/
	private ColourUnit colourMax;
	/** Min carbonation, in vol CO2*/
	private CarbonationUnit carbMin;
	/** Max carbonation, in vol CO2*/
	private CarbonationUnit carbMax;
	/** Min ABV*/
	private PercentageUnit abvMin;
	/** Max ABV*/
	private PercentageUnit abvMax;

	/** Descriptive style notes*/
	private String notes;
	/** Detailed style profile*/
	private String profile;
	/** Ingredient guidelines*/
	private String ingredients;
	/** Commercial or well known style examples*/
	private String examples;

	public Style()
	{
	}

	public Style(Style other)
	{
		this.name = other.name;
		this.styleGuideName = other.styleGuideName;
		this.category = other.category;
		this.categoryNumber = other.categoryNumber;
		this.styleLetter = other.styleLetter;
		this.styleGuide = other.styleGuide;
		this.type = other.type;
		this.ogMin = other.ogMin;
		this.ogMax = other.ogMax;
		this.fgMin = other.fgMin;
		this.fgMax = other.fgMax;
		this.ibuMin = other.ibuMin;
		this.ibuMax = other.ibuMax;
		this.colourMin = other.colourMin;
		this.colourMax = other.colourMax;
		this.carbMin = other.carbMin;
		this.carbMax = other.carbMax;
		this.abvMin = other.abvMin;
		this.abvMax = other.abvMax;
		this.notes = other.notes;
		this.profile = other.profile;
		this.ingredients = other.ingredients;
		this.examples = other.examples;
	}

	public Style(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getStyleGuideName()
	{
		return styleGuideName;
	}

	public void setStyleGuideName(String styleGuideName)
	{
		this.styleGuideName = styleGuideName;
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(String category)
	{
		this.category = category;
	}

	public String getCategoryNumber()
	{
		return categoryNumber;
	}

	public void setCategoryNumber(String categoryNumber)
	{
		this.categoryNumber = categoryNumber;
	}

	public String getStyleLetter()
	{
		return styleLetter;
	}

	public void setStyleLetter(String styleLetter)
	{
		this.styleLetter = styleLetter;
	}

	public String getStyleGuide()
	{
		return styleGuide;
	}

	public void setStyleGuide(String styleGuide)
	{
		this.styleGuide = styleGuide;
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public DensityUnit getOgMin()
	{
		return ogMin;
	}

	public void setOgMin(DensityUnit ogMin)
	{
		this.ogMin = ogMin;
	}

	public DensityUnit getOgMax()
	{
		return ogMax;
	}

	public void setOgMax(DensityUnit ogMax)
	{
		this.ogMax = ogMax;
	}

	public DensityUnit getFgMin()
	{
		return fgMin;
	}

	public void setFgMin(DensityUnit fgMin)
	{
		this.fgMin = fgMin;
	}

	public DensityUnit getFgMax()
	{
		return fgMax;
	}

	public void setFgMax(DensityUnit fgMax)
	{
		this.fgMax = fgMax;
	}

	public BitternessUnit getIbuMin()
	{
		return ibuMin;
	}

	public void setIbuMin(BitternessUnit ibuMin)
	{
		this.ibuMin = ibuMin;
	}

	public BitternessUnit getIbuMax()
	{
		return ibuMax;
	}

	public void setIbuMax(BitternessUnit ibuMax)
	{
		this.ibuMax = ibuMax;
	}

	public ColourUnit getColourMin()
	{
		return colourMin;
	}

	public void setColourMin(ColourUnit colourMin)
	{
		this.colourMin = colourMin;
	}

	public ColourUnit getColourMax()
	{
		return colourMax;
	}

	public void setColourMax(ColourUnit colourMax)
	{
		this.colourMax = colourMax;
	}

	public CarbonationUnit getCarbMin()
	{
		return carbMin;
	}

	public void setCarbMin(CarbonationUnit carbMin)
	{
		this.carbMin = carbMin;
	}

	public CarbonationUnit getCarbMax()
	{
		return carbMax;
	}

	public void setCarbMax(CarbonationUnit carbMax)
	{
		this.carbMax = carbMax;
	}

	public PercentageUnit getAbvMin()
	{
		return abvMin;
	}

	public void setAbvMin(PercentageUnit abvMin)
	{
		this.abvMin = abvMin;
	}

	public PercentageUnit getAbvMax()
	{
		return abvMax;
	}

	public void setAbvMax(PercentageUnit abvMax)
	{
		this.abvMax = abvMax;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public String getProfile()
	{
		return profile;
	}

	public void setProfile(String profile)
	{
		this.profile = profile;
	}

	public String getIngredients()
	{
		return ingredients;
	}

	public void setIngredients(String ingredients)
	{
		this.ingredients = ingredients;
	}

	public String getExamples()
	{
		return examples;
	}

	public void setExamples(String examples)
	{
		this.examples = examples;
	}

	public String toString()
	{
		return getName();
	}

	/*-------------------------------------------------------------------------*/
	// display fields

	/**
	 * @return
	 * 	Category Number+Style Letter, e.g. "5B"
	 */
	public String getStyleNumber()
	{
		return categoryNumber + styleLetter;
	}

	/*-------------------------------------------------------------------------*/

	/** The type of style, as per the BeerXML definitions */
	public static enum Type
	{
		LAGER, ALE, MEAD, WHEAT, MIXED, CIDER;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("style.type."+name());
		}
	}
}
