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

import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;

/**
 *
 */
class BeerXmlMashStep
{
	private String name;
	private MashStepType type;
	private VolumeUnit infuseAmount;
	private TemperatureUnit stepTemp;
	private TimeUnit stepTime;
	private TimeUnit rampTime;
	private TemperatureUnit endTemp;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public MashStepType getType()
	{
		return type;
	}

	public void setType(
		MashStepType type)
	{
		this.type = type;
	}

	public VolumeUnit getInfuseAmount()
	{
		return infuseAmount;
	}

	public void setInfuseAmount(VolumeUnit infuseAmount)
	{
		this.infuseAmount = infuseAmount;
	}

	public TemperatureUnit getStepTemp()
	{
		return stepTemp;
	}

	public void setStepTemp(TemperatureUnit stepTemp)
	{
		this.stepTemp = stepTemp;
	}

	public TimeUnit getStepTime()
	{
		return stepTime;
	}

	public void setStepTime(TimeUnit stepTime)
	{
		this.stepTime = stepTime;
	}

	public TimeUnit getRampTime()
	{
		return rampTime;
	}

	public void setRampTime(TimeUnit rampTime)
	{
		this.rampTime = rampTime;
	}

	public TemperatureUnit getEndTemp()
	{
		return endTemp;
	}

	public void setEndTemp(TemperatureUnit endTemp)
	{
		this.endTemp = endTemp;
	}

	enum MashStepType
	{
		INFUSION, TEMPERATURE, DECOCTION
	}

	@Override
	public String toString()
	{
		return "BeerXmlMashStep{" +
			"name='" + name + '\'' +
			", type=" + type +
			", infuseAmount=" + infuseAmount +
			", stepTemp=" + stepTemp +
			", stepTime=" + stepTime +
			", rampTime=" + rampTime +
			", endTemp=" + endTemp +
			'}';
	}
}
