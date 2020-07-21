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

import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;

/**
 *
 */
public class FermentableAddition extends IngredientAddition
{
	private Fermentable fermentable;
	private WeightUnit weight;

	public FermentableAddition()
	{
	}

	public FermentableAddition(Fermentable fermentable, WeightUnit weight, TimeUnit time)
	{
		this.fermentable = fermentable;
		setQuantity(weight);
		setTime(time);
	}

	public Fermentable getFermentable()
	{
		return fermentable;
	}

	public void setFermentable(Fermentable fermentable)
	{
		this.fermentable = fermentable;
	}

	@Override
	public String getName()
	{
		return fermentable.getName();
	}

	@Override
	public void setName(String newName)
	{
		// not possible
	}

	@Override
	public Type getType()
	{
		return Type.FERMENTABLES;
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
		return new FermentableAddition(
			this.fermentable,
			new WeightUnit(this.weight.get()),
			this.getTime());
	}

	@Override
	public String toString()
	{
		return getName();
//		return StringUtils.getUiString("fermentable.addition.toString",
//			getName(),
//			getQuantity().get(Quantity.Unit.KILOGRAMS),
//			getTime().get(Quantity.Unit.MINUTES));
	}
}
