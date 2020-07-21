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

import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;

/**
 *
 */
public class YeastAddition extends IngredientAddition
{
	private Yeast yeast;
	private Quantity amount;

	// only used to support BeerXML
	private boolean addToSecondary;

	public YeastAddition()
	{
	}

	public YeastAddition(Yeast yeast, WeightUnit amount, TimeUnit time)
	{
		this.yeast = yeast;
		setQuantity(amount);
		setTime(time);
	}

	public Yeast getYeast()
	{
		return yeast;
	}

	public void setYeast(Yeast yeast)
	{
		this.yeast = yeast;
	}

	@Override
	public String getName()
	{
		return yeast.getName();
	}

	@Override
	public void setName(String newName)
	{
		// not possible
	}

	@Override
	public Type getType()
	{
		return Type.YEAST;
	}

	public Quantity getQuantity()
	{
		return amount;
	}

	public void setQuantity(Quantity weight)
	{
		this.amount = weight;
	}

	@Override
	public IngredientAddition clone()
	{
		return new YeastAddition(
			this.yeast,
			new WeightUnit(this.amount.get()),
			this.getTime());
	}

	@Override
	public String toString()
	{
		return getName();
//		return StringUtils.getUiString("yeast.addition.toString",
//			getName(),
//			getQuantity().get(Quantity.Unit.GRAMS),
//			getTime().get(Quantity.Unit.DAYS));
	}

	public void setAddToSecondary(boolean addToSecondary)
	{
		this.addToSecondary = addToSecondary;
	}

	public boolean getAddToSecondary()
	{
		return addToSecondary;
	}
}
