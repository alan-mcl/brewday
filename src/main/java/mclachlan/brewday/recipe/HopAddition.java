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

package mclachlan.brewday.recipe;

import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;

/**
 *
 */
public class HopAddition extends IngredientAddition
{
	private Hop hop;

	// used only for BeerXML support
	private Use use;

	// volatile data
	// time already boiled, for use
	private TimeUnit boiledTime = new TimeUnit(0);

	public enum Use
	{
		BOIL, DRY_HOP, MASH, FIRST_WORT, AROMA
	}

	/*-------------------------------------------------------------------------*/
	public HopAddition()
	{
	}

	public HopAddition(Hop hop, Quantity quantity, Quantity.Unit unit,
		TimeUnit time)
	{
		this.hop = hop;
		setQuantity(quantity);
		setUnit(unit);
		setTime(time);
	}

	public Hop getHop()
	{
		return hop;
	}

	public void setHop(Hop hop)
	{
		this.hop = hop;
	}

	@Override
	public String getName()
	{
		return hop.getName();
	}

	@Override
	public void setName(String newName)
	{
		// not possible
	}

	@Override
	public Type getType()
	{
		return Type.HOPS;
	}

	public Use getUse()
	{
		return use;
	}

	public void setUse(Use use)
	{
		this.use = use;
	}

	public TimeUnit getBoiledTime()
	{
		return boiledTime;
	}

	public void setBoiledTime(TimeUnit boiledTime)
	{
		this.boiledTime = boiledTime;
	}

	public void setForm(Hop.Form form)
	{
		// used for BeerXML support only
	}

	@Override
	public IngredientAddition clone()
	{
		HopAddition result = new HopAddition(
			this.hop,
			getQuantity(),
			getUnit(),
			getTime());

		result.setBoiledTime(new TimeUnit(this.getBoiledTime()));

		return result;
	}

	@Override
	public Quantity.Type getAdditionQuantityType()
	{
		return hop.getForm().getQuantityType();
	}

	public String describe()
	{
		double quantity = getQuantity().get(getUnit());
		String quantityS = StringUtils.format(quantity, getUnit());

		return StringUtils.getDocString("hop.addition.desc",
			quantityS, hop.getName());
	}

	@Override
	public String toString()
	{
		String qty = getQuantity().describe(getUnit());

		return StringUtils.getUiString("hop.addition.toString",
			getName(),
			qty,
			getTime().get(Quantity.Unit.MINUTES));
	}
}
