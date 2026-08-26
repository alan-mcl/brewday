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

package mclachlan.brewday.recipe;

import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.ui.UiQuantityDisplay;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.SECONDS;

/**
 * Living yeast population state carried on a {@link mclachlan.brewday.process.Volume}.
 */
public class YeastCulture extends IngredientAddition
{
	private Yeast yeast;
	private long cellCount;
	private PercentageUnit viability;
	private int generation;
	private YeastActivityState activityState = YeastActivityState.ACTIVE;
	private YeastSourceType sourceType = YeastSourceType.DIRECT_PITCH;

	/*-------------------------------------------------------------------------*/
	public YeastCulture()
	{
	}

	/*-------------------------------------------------------------------------*/
	public YeastCulture(
		Yeast yeast,
		Quantity quantity,
		Quantity.Unit unit,
		long cellCount,
		PercentageUnit viability,
		int generation,
		YeastActivityState activityState,
		YeastSourceType sourceType)
	{
		this.yeast = yeast;
		setQuantity(quantity);
		setUnit(unit);
		setTime(new TimeUnit(0, SECONDS, false));
		this.cellCount = cellCount;
		this.viability = viability;
		this.generation = generation;
		if (activityState != null)
		{
			this.activityState = activityState;
		}
		if (sourceType != null)
		{
			this.sourceType = sourceType;
		}
	}

	/*-------------------------------------------------------------------------*/
	public static YeastCulture fromPitch(YeastAddition pitch)
	{
		YeastCulture culture = new YeastCulture(
			pitch.getYeast(),
			pitch.getQuantity(),
			pitch.getUnit(),
			0L,
			null,
			0,
			YeastActivityState.ACTIVE,
			YeastSourceType.DIRECT_PITCH);
		return culture;
	}

	/*-------------------------------------------------------------------------*/
	public Yeast getYeast()
	{
		return yeast;
	}

	public void setYeast(Yeast yeast)
	{
		this.yeast = yeast;
	}

	public long getCellCount()
	{
		return cellCount;
	}

	public void setCellCount(long cellCount)
	{
		this.cellCount = cellCount;
	}

	public PercentageUnit getViability()
	{
		return viability;
	}

	public void setViability(PercentageUnit viability)
	{
		this.viability = viability;
	}

	public int getGeneration()
	{
		return generation;
	}

	public void setGeneration(int generation)
	{
		this.generation = generation;
	}

	public YeastActivityState getActivityState()
	{
		return activityState;
	}

	public void setActivityState(YeastActivityState activityState)
	{
		this.activityState = activityState;
	}

	public YeastSourceType getSourceType()
	{
		return sourceType;
	}

	public void setSourceType(YeastSourceType sourceType)
	{
		this.sourceType = sourceType;
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
		return Type.YEAST_CULTURE;
	}

	@Override
	public IngredientAddition clone()
	{
		YeastCulture copy = new YeastCulture(
			this.yeast,
			getQuantity(),
			getUnit(),
			this.cellCount,
			viability == null ? null : new PercentageUnit(viability),
			this.generation,
			this.activityState,
			this.sourceType);
		copy.setTime(new TimeUnit(0, SECONDS, false));
		return copy;
	}

	@Override
	public String describe()
	{
		return StringUtils.getDocString("yeast.culture.desc",
			UiQuantityDisplay.describeAdditionQuantity(this),
			yeast.getName());
	}
}
