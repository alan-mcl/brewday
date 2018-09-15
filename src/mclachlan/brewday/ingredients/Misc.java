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
public class Misc
{
	private String description;
	private String name;
	private Type type;
	private Use use;
	private String usageRecommendation;

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

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public void setUse(Use use)
	{
		this.use = use;
	}

	public Use getUse()
	{
		return use;
	}

	public void setUsageRecommendation(String usageRecommendation)
	{
		this.usageRecommendation = usageRecommendation;
	}

	public String getUsageRecommendation()
	{
		return usageRecommendation;
	}

	public enum Type
	{
		SPICE, FINING, WATER_AGENT, HERB, FLAVOUR, OTHER;
	}

	public enum Use
	{
		BOIL, MASH, PRIMARY, SECONDARY, BOTTLING
	}
}
