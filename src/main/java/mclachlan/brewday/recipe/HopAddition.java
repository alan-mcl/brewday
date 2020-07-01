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

import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;

/**
 *
 */
public class HopAddition extends IngredientAddition
{
	private Hop hop;
	private WeightUnit weight;

	public HopAddition()
	{
	}

	public HopAddition(Hop hop, WeightUnit weight, TimeUnit time)
	{
		this.hop = hop;
		setQuantity(weight);
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

	public Quantity getQuantity()
	{
		return weight;
	}

	public void setQuantity(Quantity weight)
	{
		this.weight = (WeightUnit)weight;
	}

	@Override
	public IngredientAddition clone()
	{
		return new HopAddition(
			this.hop,
			new WeightUnit(this.weight.get()),
			this.getTime());
	}

	@Override
	public String toString()
	{
		return StringUtils.getUiString("hop.addition.toString",
			getName(),
			getQuantity().get(Quantity.Unit.GRAMS),
			getTime().get(Quantity.Unit.MINUTES));
	}
}
