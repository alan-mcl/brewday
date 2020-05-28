package mclachlan.brewday.style;

import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.DensityUnit;

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
	private int ibuMin;
	/** Max bitterness in IBU*/
	private int ibuMax;
	/** Min colour in SRM*/
	private int colourMin;
	/** Max colour in SRM*/
	private int colourMax;
	/** Min carbonation, in vol CO2*/
	private double carbMin;
	/** Max carbonation, in vol CO2*/
	private double carbMax;
	/** Min ABV*/
	private double abvMin;
	/** Max ABV*/
	private double abvMax;
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

	public Style(
		String name,
		String styleGuideName,
		String category,
		String categoryNumber,
		String styleLetter,
		String styleGuide,
		Type type,
		DensityUnit ogMin,
		DensityUnit ogMax,
		DensityUnit fgMin,
		DensityUnit fgMax,
		int ibuMin,
		int ibuMax,
		int colourMin,
		int colourMax,
		double carbMin,
		double carbMax,
		double abvMin,
		double abvMax,
		String notes,
		String profile,
		String ingredients,
		String examples)
	{
		this.name = name;
		this.styleGuideName = styleGuideName;
		this.category = category;
		this.categoryNumber = categoryNumber;
		this.styleLetter = styleLetter;
		this.styleGuide = styleGuide;
		this.type = type;
		this.ogMin = ogMin;
		this.ogMax = ogMax;
		this.fgMin = fgMin;
		this.fgMax = fgMax;
		this.ibuMin = ibuMin;
		this.ibuMax = ibuMax;
		this.colourMin = colourMin;
		this.colourMax = colourMax;
		this.carbMin = carbMin;
		this.carbMax = carbMax;
		this.abvMin = abvMin;
		this.abvMax = abvMax;
		this.notes = notes;
		this.profile = profile;
		this.ingredients = ingredients;
		this.examples = examples;
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

	public int getIbuMin()
	{
		return ibuMin;
	}

	public void setIbuMin(int ibuMin)
	{
		this.ibuMin = ibuMin;
	}

	public int getIbuMax()
	{
		return ibuMax;
	}

	public void setIbuMax(int ibuMax)
	{
		this.ibuMax = ibuMax;
	}

	public int getColourMin()
	{
		return colourMin;
	}

	public void setColourMin(int colourMin)
	{
		this.colourMin = colourMin;
	}

	public int getColourMax()
	{
		return colourMax;
	}

	public void setColourMax(int colourMax)
	{
		this.colourMax = colourMax;
	}

	public double getCarbMin()
	{
		return carbMin;
	}

	public void setCarbMin(double carbMin)
	{
		this.carbMin = carbMin;
	}

	public double getCarbMax()
	{
		return carbMax;
	}

	public void setCarbMax(double carbMax)
	{
		this.carbMax = carbMax;
	}

	public double getAbvMin()
	{
		return abvMin;
	}

	public void setAbvMin(double abvMin)
	{
		this.abvMin = abvMin;
	}

	public double getAbvMax()
	{
		return abvMax;
	}

	public void setAbvMax(double abvMax)
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

	/** The type of style, as per the BeerXML definitions */
	public static enum Type
	{
		LAGER, ALE, MEAD, WHEAT, MIXED, CIDER
	}
}
