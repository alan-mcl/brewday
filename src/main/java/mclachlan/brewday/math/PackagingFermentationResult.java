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

package mclachlan.brewday.math;

/**
 * CO₂ and ABV contributions from packaging fermentation of a fermentable extract mass.
 */
public class PackagingFermentationResult
{
	public final CarbonationUnit carbonation;
	public final PercentageUnit abvIncrease;

	public PackagingFermentationResult(
		CarbonationUnit carbonation,
		PercentageUnit abvIncrease)
	{
		this.carbonation = carbonation;
		this.abvIncrease = abvIncrease;
	}
}
