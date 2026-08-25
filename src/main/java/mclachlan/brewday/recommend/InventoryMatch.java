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

import java.util.ArrayList;
import java.util.List;

/**
 * Weighted inventory coverage for a recipe.
 */
public final class InventoryMatch
{
	private final int matchPercent;
	private final boolean fullyBrewable;
	private final List<InventoryMatchLine> lines;
	private final List<InventoryMatchLine> missingLines;
	private final int missingLineCount;

	public InventoryMatch(
		int matchPercent,
		boolean fullyBrewable,
		List<InventoryMatchLine> lines,
		List<InventoryMatchLine> missingLines)
	{
		this.matchPercent = matchPercent;
		this.fullyBrewable = fullyBrewable;
		this.lines = List.copyOf(lines);
		this.missingLines = List.copyOf(missingLines);
		this.missingLineCount = missingLines.size();
	}

	public int getMatchPercent()
	{
		return matchPercent;
	}

	public boolean isFullyBrewable()
	{
		return fullyBrewable;
	}

	public List<InventoryMatchLine> getLines()
	{
		return lines;
	}

	public List<InventoryMatchLine> getMissingLines()
	{
		return missingLines;
	}

	public int getMissingLineCount()
	{
		return missingLineCount;
	}

	public boolean hasCriticalMiss()
	{
		for (InventoryMatchLine line : lines)
		{
			if (line.isCriticalMiss())
			{
				return true;
			}
		}
		return false;
	}

	public List<InventoryMatchLine> getSmallPurchaseCandidates()
	{
		List<InventoryMatchLine> result = new ArrayList<>();
		for (InventoryMatchLine line : missingLines)
		{
			if (!line.isCriticalMiss() && line.getShortfall() > 0D)
			{
				result.add(line);
			}
		}
		return result;
	}
}
