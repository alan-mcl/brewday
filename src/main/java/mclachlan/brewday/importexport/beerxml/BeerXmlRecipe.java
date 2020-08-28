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

package mclachlan.brewday.importexport.beerxml;

import java.time.LocalDate;
import java.util.*;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.style.Style;

/**
 *
 */
public class BeerXmlRecipe implements V2DataObject
{
	private String name;
	private Type type;
	private Style style;
	private EquipmentProfile equipment;
	private String brewer;
	private String asstBrewer;
	private VolumeUnit batchSize;
	private VolumeUnit boilSize;
	private TimeUnit boilTime;
	private PercentageUnit efficiency;
	private List<HopAddition> hops = new ArrayList<>();
	private List<FermentableAddition> fermentables = new ArrayList<>();
	private List<MiscAddition> miscs = new ArrayList<>();
	private List<YeastAddition> yeasts = new ArrayList<>();
	private List<WaterAddition> waters = new ArrayList<>();
	private BeerXmlMashProfile mash;
	private String notes;
	private String tasteNotes;
	private double tasteRating;
	private DensityUnit og;
	private DensityUnit fg;
	private int fermentationStages;
	private TimeUnit primaryAge;
	private TemperatureUnit primaryTemp;
	private TimeUnit secondaryAge;
	private TemperatureUnit secondaryTemp;
	private TimeUnit tertiaryAge;
	private TemperatureUnit tertiaryTemp;
	private TimeUnit age;
	private TemperatureUnit ageTemp;
	private LocalDate date;
	private CarbonationUnit carbonation;
	private String carbonationUsed;
	private boolean forcedCarbonation;
	private String primingSugarName;
	private TemperatureUnit carbonationTemp;
	private double primingSugarEquiv;
	private double kegPrimingFactor;

	enum Type
	{
		EXTRACT, PARTIAL_MASH, ALL_GRAIN
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	public Type getType()
	{
		return type;
	}

	public void setType(
		Type type)
	{
		this.type = type;
	}

	public Style getStyle()
	{
		return style;
	}

	public void setStyle(Style style)
	{
		this.style = style;
	}

	public EquipmentProfile getEquipment()
	{
		return equipment;
	}

	public void setEquipment(EquipmentProfile equipment)
	{
		this.equipment = equipment;
	}

	public String getBrewer()
	{
		return brewer;
	}

	public void setBrewer(String brewer)
	{
		this.brewer = brewer;
	}

	public String getAsstBrewer()
	{
		return asstBrewer;
	}

	public void setAsstBrewer(String asstBrewer)
	{
		this.asstBrewer = asstBrewer;
	}

	public VolumeUnit getBatchSize()
	{
		return batchSize;
	}

	public void setBatchSize(VolumeUnit batchSize)
	{
		this.batchSize = batchSize;
	}

	public VolumeUnit getBoilSize()
	{
		return boilSize;
	}

	public void setBoilSize(VolumeUnit boilSize)
	{
		this.boilSize = boilSize;
	}

	public TimeUnit getBoilTime()
	{
		return boilTime;
	}

	public void setBoilTime(TimeUnit boilTime)
	{
		this.boilTime = boilTime;
	}

	public PercentageUnit getEfficiency()
	{
		return efficiency;
	}

	public void setEfficiency(PercentageUnit efficiency)
	{
		this.efficiency = efficiency;
	}

	public List<HopAddition> getHops()
	{
		return hops;
	}

	public void setHops(List<HopAddition> hops)
	{
		this.hops = hops;
	}

	public List<FermentableAddition> getFermentables()
	{
		return fermentables;
	}

	public void setFermentables(
		List<FermentableAddition> fermentables)
	{
		this.fermentables = fermentables;
	}

	public List<MiscAddition> getMiscs()
	{
		return miscs;
	}

	public void setMiscs(List<MiscAddition> miscs)
	{
		this.miscs = miscs;
	}

	public List<YeastAddition> getYeasts()
	{
		return yeasts;
	}

	public void setYeasts(List<YeastAddition> yeasts)
	{
		this.yeasts = yeasts;
	}

	public List<WaterAddition> getWaters()
	{
		return waters;
	}

	public void setWaters(List<WaterAddition> waters)
	{
		this.waters = waters;
	}

	public BeerXmlMashProfile getMash()
	{
		return mash;
	}

	public void setMash(BeerXmlMashProfile mash)
	{
		this.mash = mash;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public String getTasteNotes()
	{
		return tasteNotes;
	}

	public void setTasteNotes(String tasteNotes)
	{
		this.tasteNotes = tasteNotes;
	}

	public double getTasteRating()
	{
		return tasteRating;
	}

	public void setTasteRating(double tasteRating)
	{
		this.tasteRating = tasteRating;
	}

	public DensityUnit getOg()
	{
		return og;
	}

	public void setOg(DensityUnit og)
	{
		this.og = og;
	}

	public DensityUnit getFg()
	{
		return fg;
	}

	public void setFg(DensityUnit fg)
	{
		this.fg = fg;
	}

	public int getFermentationStages()
	{
		return fermentationStages;
	}

	public void setFermentationStages(int fermentationStages)
	{
		this.fermentationStages = fermentationStages;
	}

	public TimeUnit getPrimaryAge()
	{
		return primaryAge;
	}

	public void setPrimaryAge(TimeUnit primaryAge)
	{
		this.primaryAge = primaryAge;
	}

	public TemperatureUnit getPrimaryTemp()
	{
		return primaryTemp;
	}

	public void setPrimaryTemp(TemperatureUnit primaryTemp)
	{
		this.primaryTemp = primaryTemp;
	}

	public TimeUnit getSecondaryAge()
	{
		return secondaryAge;
	}

	public void setSecondaryAge(TimeUnit secondaryAge)
	{
		this.secondaryAge = secondaryAge;
	}

	public TemperatureUnit getSecondaryTemp()
	{
		return secondaryTemp;
	}

	public void setSecondaryTemp(TemperatureUnit secondaryTemp)
	{
		this.secondaryTemp = secondaryTemp;
	}

	public TimeUnit getTertiaryAge()
	{
		return tertiaryAge;
	}

	public void setTertiaryAge(TimeUnit tertiaryAge)
	{
		this.tertiaryAge = tertiaryAge;
	}

	public TemperatureUnit getTertiaryTemp()
	{
		return tertiaryTemp;
	}

	public void setTertiaryTemp(TemperatureUnit tertiaryTemp)
	{
		this.tertiaryTemp = tertiaryTemp;
	}

	public TimeUnit getAge()
	{
		return age;
	}

	public void setAge(TimeUnit age)
	{
		this.age = age;
	}

	public TemperatureUnit getAgeTemp()
	{
		return ageTemp;
	}

	public void setAgeTemp(TemperatureUnit ageTemp)
	{
		this.ageTemp = ageTemp;
	}

	public LocalDate getDate()
	{
		return date;
	}

	public void setDate(LocalDate date)
	{
		this.date = date;
	}

	public CarbonationUnit getCarbonation()
	{
		return carbonation;
	}

	public void setCarbonation(CarbonationUnit carbonation)
	{
		this.carbonation = carbonation;
	}

	public boolean isForcedCarbonation()
	{
		return forcedCarbonation;
	}

	public void setForcedCarbonation(boolean forcedCarbonation)
	{
		this.forcedCarbonation = forcedCarbonation;
	}

	public String getPrimingSugarName()
	{
		return primingSugarName;
	}

	public void setPrimingSugarName(String primingSugarName)
	{
		this.primingSugarName = primingSugarName;
	}

	public TemperatureUnit getCarbonationTemp()
	{
		return carbonationTemp;
	}

	public void setCarbonationTemp(TemperatureUnit carbonationTemp)
	{
		this.carbonationTemp = carbonationTemp;
	}

	public double getPrimingSugarEquiv()
	{
		return primingSugarEquiv;
	}

	public void setPrimingSugarEquiv(double primingSugarEquiv)
	{
		this.primingSugarEquiv = primingSugarEquiv;
	}

	public double getKegPrimingFactor()
	{
		return kegPrimingFactor;
	}

	public void setKegPrimingFactor(double kegPrimingFactor)
	{
		this.kegPrimingFactor = kegPrimingFactor;
	}

	public String getCarbonationUsed()
	{
		return carbonationUsed;
	}

	public void setCarbonationUsed(String carbonationUsed)
	{
		this.carbonationUsed = carbonationUsed;
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public String toString()
	{
		return "BeerXmlRecipe{" +
			"name='" + name + '\'' +
			", type=" + type +
			", style=" + style +
			", equipment=" + equipment +
			", brewer='" + brewer + '\'' +
			", asstBrewer='" + asstBrewer + '\'' +
			", batchSize=" + batchSize +
			", boilSize=" + boilSize +
			", boilTime=" + boilTime +
			", efficiency=" + efficiency +
			", hops=" + hops +
			", fermentables=" + fermentables +
			", miscs=" + miscs +
			", yeasts=" + yeasts +
			", waters=" + waters +
			", mash=" + mash +
			", notes='" + notes + '\'' +
			", tasteNotes='" + tasteNotes + '\'' +
			", tasteRating=" + tasteRating +
			", og=" + og +
			", fg=" + fg +
			", fermentationStages=" + fermentationStages +
			", primaryAge=" + primaryAge +
			", primaryTemp=" + primaryTemp +
			", secondaryAge=" + secondaryAge +
			", secondaryTemp=" + secondaryTemp +
			", tertiaryAge=" + tertiaryAge +
			", tertiaryTemp=" + tertiaryTemp +
			", age=" + age +
			", ageTemp=" + ageTemp +
			", date=" + date +
			", carbonation=" + carbonation +
			", forcedCarbonation=" + forcedCarbonation +
			", primingSugarName='" + primingSugarName + '\'' +
			", carbonationTemp=" + carbonationTemp +
			", primingSugarEquiv=" + primingSugarEquiv +
			", kegPrimingFactor=" + kegPrimingFactor +
			'}';
	}
}
