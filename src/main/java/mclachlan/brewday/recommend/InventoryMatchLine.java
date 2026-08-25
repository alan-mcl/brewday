/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.recommend;

import java.util.List;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 * Inventory coverage for one recipe bill-of-materials line.
 */
public final class InventoryMatchLine
{
	private final String ingredientName;
	private final IngredientAddition.Type type;
	private final double required;
	private final double inStock;
	private final double coverage;
	private final double lineWeight;
	private final Quantity.Unit unit;
	private final boolean criticalMiss;

	public InventoryMatchLine(
		String ingredientName,
		IngredientAddition.Type type,
		double required,
		double inStock,
		double coverage,
		double lineWeight,
		Quantity.Unit unit,
		boolean criticalMiss)
	{
		this.ingredientName = ingredientName;
		this.type = type;
		this.required = required;
		this.inStock = inStock;
		this.coverage = coverage;
		this.lineWeight = lineWeight;
		this.unit = unit;
		this.criticalMiss = criticalMiss;
	}

	public String getIngredientName()
	{
		return ingredientName;
	}

	public IngredientAddition.Type getType()
	{
		return type;
	}

	public double getRequired()
	{
		return required;
	}

	public double getInStock()
	{
		return inStock;
	}

	public double getCoverage()
	{
		return coverage;
	}

	public double getLineWeight()
	{
		return lineWeight;
	}

	public Quantity.Unit getUnit()
	{
		return unit;
	}

	public boolean isCriticalMiss()
	{
		return criticalMiss;
	}

	public boolean isFullyCovered()
	{
		return coverage >= 0.999D;
	}

	public boolean isMissing()
	{
		return coverage <= 0.001D;
	}

	public double getShortfall()
	{
		return Math.max(0D, required - inStock);
	}

	public double getSurplusAfterBrew()
	{
		return Math.max(0D, inStock - required);
	}
}
