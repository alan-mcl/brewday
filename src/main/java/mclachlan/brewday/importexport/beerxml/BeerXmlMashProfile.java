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

import java.util.*;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.WeightUnit;

/**
 *
 */
class BeerXmlMashProfile
{
	String name;
	TemperatureUnit grainTemp;
	List<BeerXmlMashStep> mashSteps;
	String notes;
	TemperatureUnit tunTemp;
	TemperatureUnit spargeTemp;
	PhUnit ph;
	WeightUnit tunWeight;
	ArbitraryPhysicalQuantity tunSpecificHeat;
	boolean equipAdjust;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public TemperatureUnit getGrainTemp()
	{
		return grainTemp;
	}

	public void setGrainTemp(TemperatureUnit grainTemp)
	{
		this.grainTemp = grainTemp;
	}

	public List<BeerXmlMashStep> getMashSteps()
	{
		return mashSteps;
	}

	public void setMashSteps(
		List<BeerXmlMashStep> mashSteps)
	{
		this.mashSteps = mashSteps;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public TemperatureUnit getTunTemp()
	{
		return tunTemp;
	}

	public void setTunTemp(TemperatureUnit tunTemp)
	{
		this.tunTemp = tunTemp;
	}

	public TemperatureUnit getSpargeTemp()
	{
		return spargeTemp;
	}

	public void setSpargeTemp(TemperatureUnit spargeTemp)
	{
		this.spargeTemp = spargeTemp;
	}

	public PhUnit getPh()
	{
		return ph;
	}

	public void setPh(PhUnit ph)
	{
		this.ph = ph;
	}

	public WeightUnit getTunWeight()
	{
		return tunWeight;
	}

	public void setTunWeight(WeightUnit tunWeight)
	{
		this.tunWeight = tunWeight;
	}

	public ArbitraryPhysicalQuantity getTunSpecificHeat()
	{
		return tunSpecificHeat;
	}

	public void setTunSpecificHeat(
		ArbitraryPhysicalQuantity tunSpecificHeat)
	{
		this.tunSpecificHeat = tunSpecificHeat;
	}

	public boolean isEquipAdjust()
	{
		return equipAdjust;
	}

	public void setEquipAdjust(boolean equipAdjust)
	{
		this.equipAdjust = equipAdjust;
	}

	@Override
	public String toString()
	{
		return "BeerXmlMashProfile{" +
			"name='" + name + '\'' +
			", grainTemp=" + grainTemp +
			", mashSteps=" + mashSteps +
			", notes='" + notes + '\'' +
			", tunTemp=" + tunTemp +
			", spargeTemp=" + spargeTemp +
			", ph=" + ph +
			", tunWeight=" + tunWeight +
			", tunSpecificHeat=" + tunSpecificHeat +
			", equipAdjust=" + equipAdjust +
			'}';
	}
}
