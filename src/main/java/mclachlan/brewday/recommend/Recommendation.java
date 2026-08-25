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
import java.util.Collections;
import java.util.List;

/**
 * A single recipe suggestion within a recommendation group.
 */
public class Recommendation
{
	private final String recipeName;
	private final String styleDisplay;
	private final int inventoryMatchPercent;
	private final String explanation;
	private final List<RecommendationTag> tags;
	private final List<String> detailLines;

	public Recommendation(
		String recipeName,
		String styleDisplay,
		int inventoryMatchPercent,
		String explanation,
		List<RecommendationTag> tags,
		List<String> detailLines)
	{
		this.recipeName = recipeName;
		this.styleDisplay = styleDisplay;
		this.inventoryMatchPercent = inventoryMatchPercent;
		this.explanation = explanation;
		this.tags = tags == null ? List.of() : List.copyOf(tags);
		this.detailLines = detailLines == null ? List.of() : List.copyOf(detailLines);
	}

	public String getRecipeName()
	{
		return recipeName;
	}

	public String getStyleDisplay()
	{
		return styleDisplay;
	}

	public int getInventoryMatchPercent()
	{
		return inventoryMatchPercent;
	}

	public String getExplanation()
	{
		return explanation;
	}

	public List<RecommendationTag> getTags()
	{
		return tags;
	}

	public List<String> getDetailLines()
	{
		return detailLines;
	}

	public static Builder builder(String recipeName)
	{
		return new Builder(recipeName);
	}

	public static final class Builder
	{
		private final String recipeName;
		private String styleDisplay = "";
		private int inventoryMatchPercent;
		private String explanation = "";
		private final List<RecommendationTag> tags = new ArrayList<>();
		private final List<String> detailLines = new ArrayList<>();

		private Builder(String recipeName)
		{
			this.recipeName = recipeName;
		}

		public Builder styleDisplay(String styleDisplay)
		{
			this.styleDisplay = styleDisplay;
			return this;
		}

		public Builder inventoryMatchPercent(int inventoryMatchPercent)
		{
			this.inventoryMatchPercent = inventoryMatchPercent;
			return this;
		}

		public Builder explanation(String explanation)
		{
			this.explanation = explanation;
			return this;
		}

		public Builder tag(RecommendationTag tag)
		{
			if (tag != null && !tags.contains(tag))
			{
				tags.add(tag);
			}
			return this;
		}

		public Builder detailLine(String line)
		{
			if (line != null && !line.isBlank())
			{
				detailLines.add(line);
			}
			return this;
		}

		public Recommendation build()
		{
			return new Recommendation(
				recipeName,
				styleDisplay,
				inventoryMatchPercent,
				explanation,
				Collections.unmodifiableList(new ArrayList<>(tags)),
				Collections.unmodifiableList(new ArrayList<>(detailLines)));
		}
	}
}
