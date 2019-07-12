package mclachlan.brewday.style;

import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
public class Style implements V2DataObject
{
	private String name;
	private String styleGuideName;
	private String category;
	private String categoryNumber;
	private String styleLetter;
	private String styleGuide;
	private Type type;
	private double ogMin;
	private double ogMax;
	private double fgMin;
	private double fgMax;
	private int ibuMin;
	private int ibuMax;
	private int colourMin;
	private int colourMax;
	private double carbMin;
	private double carbMax;
	private double abvMin;
	private double abvMax;
	private String notes;
	private String profile;
	private String ingredients;
	private String examples;

	public Style()
	{
	}

	public Style(String name, String styleGuideName, String category, String categoryNumber,
		String styleLetter, String styleGuide,
		Type type, double ogMin, double ogMax, double fgMin, double fgMax,
		int ibuMin, int ibuMax, int colourMin, int colourMax, double carbMin,
		double carbMax, double abvMin, double abvMax, String notes,
		String profile, String ingredients, String examples)
	{
		this.name = name;
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

	public double getOgMin()
	{
		return ogMin;
	}

	public void setOgMin(double ogMin)
	{
		this.ogMin = ogMin;
	}

	public double getOgMax()
	{
		return ogMax;
	}

	public void setOgMax(double ogMax)
	{
		this.ogMax = ogMax;
	}

	public double getFgMin()
	{
		return fgMin;
	}

	public void setFgMin(double fgMin)
	{
		this.fgMin = fgMin;
	}

	public double getFgMax()
	{
		return fgMax;
	}

	public void setFgMax(double fgMax)
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

	public static enum Type
	{
		LAGER, ALE, MEAD, WHEAT, MIXED, CIDER
	}
}
