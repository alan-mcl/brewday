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
	private Form form;

	// used only for BeerXML support
	private Use use;

	// volatile data
	// time already boiled, for use
	private TimeUnit boiledTime = new TimeUnit(0);

	/*-------------------------------------------------------------------------*/
	public enum Form
	{
		PELLET, PLUG, LEAF;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("hop.form." + name());
		}
	}

	public enum Use
	{
		BOIL, DRY_HOP, MASH, FIRST_WORT, AROMA
	}

	/*-------------------------------------------------------------------------*/
	public HopAddition()
	{
	}

	public HopAddition(Hop hop, Form form, Quantity quantity, Quantity.Unit unit,
		TimeUnit time)
	{
		this.hop = hop;
		setForm(form);
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
	public Quantity.Type getAdditionQuantityType()
	{
		return Quantity.Type.WEIGHT;
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

	public Form getForm()
	{
		return form;
	}

	public void setForm(Form form)
	{
		this.form = form;
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

	public String describe()
	{
		double quantity = getQuantity().get(getUnit());
		String quantityS = StringUtils.format(quantity, getUnit());

		return StringUtils.getDocString("hop.addition.desc",
			quantityS, hop.getName());
	}

	@Override
	public IngredientAddition clone()
	{
		HopAddition result = new HopAddition(
			this.hop,
			this.form,
			getQuantity(),
			getUnit(),
			getTime());

		result.setBoiledTime(new TimeUnit(this.getBoiledTime()));

		return result;
	}

	@Override
	public String toString()
	{
//		return getName();

		String qty;

		if (getQuantity().get(Quantity.Unit.KILOGRAMS) < 1)
		{
			qty = getQuantity().describe(Quantity.Unit.GRAMS);
		}
		else
		{
			qty = getQuantity().describe(Quantity.Unit.KILOGRAMS);
		}

		return StringUtils.getUiString("hop.addition.toString",
			getName(),
			qty,
			getTime().get(Quantity.Unit.MINUTES));
	}
}
