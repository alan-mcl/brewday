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
import mclachlan.brewday.util.StringUtils;

/**
 *
 */
public class YeastAddition extends IngredientAddition
{
	private Yeast yeast;

	// only used to support BeerXML
	private boolean addToSecondary;

	public YeastAddition()
	{
	}

	public YeastAddition(Yeast yeast, Quantity quantity, Quantity.Unit unit, TimeUnit time)
	{
		this.yeast = yeast;
		setQuantity(quantity);
		setUnit(unit);
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
	public Quantity.Type getAdditionQuantityType()
	{
		return yeast.getForm().getQuantityType();
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

	@Override
	public IngredientAddition clone()
	{
		return new YeastAddition(
			this.yeast,
			this.getQuantity(),
			getUnit(),
			this.getTime());
	}

	public String describe()
	{
		double quantity = getQuantity().get(getUnit());
		String quantityS = StringUtils.format(quantity, getUnit());
		return StringUtils.getDocString("yeast.addition.desc",
			quantityS, yeast.getName());
	}

	@Override
	public String toString()
	{
		String qty = getQuantity().describe(getUnit());

		return StringUtils.getUiString("yeast.addition.toString",
			getName(),
			qty,
			getTime().get(Quantity.Unit.DAYS));
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
