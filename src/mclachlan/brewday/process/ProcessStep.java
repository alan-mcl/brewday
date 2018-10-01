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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.recipe.AdditionSchedule;

/**
 *
 */
public abstract class ProcessStep implements Comparable<ProcessStep>
{
	private String name;
	private String description;
	private Type type;
	private List<AdditionSchedule> ingredientAdditions;

	/*-------------------------------------------------------------------------*/
	public ProcessStep(String name, String description, Type type)
	{
		this.name = name;
		this.description = description;
		this.type = type;

		ingredientAdditions = new ArrayList<AdditionSchedule>();
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Apply this process step to the current recipe state.
	 */
	public abstract void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log);

	/*-------------------------------------------------------------------------*/
	public abstract String describe(Volumes v);

	/*-------------------------------------------------------------------------*/
	public String getName()
	{
		return name;
	}

	/*-------------------------------------------------------------------------*/
	public String getDescription()
	{
		return description;
	}

	/*-------------------------------------------------------------------------*/
	public Type getType()
	{
		return type;
	}

	public List<AdditionSchedule> getIngredientAdditions()
	{
		return ingredientAdditions;
	}

	public void setIngredientAdditions(
		List<AdditionSchedule> ingredientAdditions)
	{
		this.ingredientAdditions = ingredientAdditions;
	}

	/*-------------------------------------------------------------------------*/
	public abstract Collection<String> getInputVolumes();

	/*-------------------------------------------------------------------------*/
	public abstract Collection<String> getOutputVolumes();

	/*-------------------------------------------------------------------------*/
	@Override
	public int compareTo(ProcessStep other)
	{
		boolean outputToOther = !Collections.disjoint(this.getOutputVolumes(), other.getInputVolumes());
		boolean inputToOther = !Collections.disjoint(this.getInputVolumes(), other.getOutputVolumes());

		if (outputToOther && inputToOther)
		{
			// can't have this
			throw new BrewdayException("Pipeline error: steps ["+this.getName()+"] " +
				"and ["+other.getName()+"] have a circular volume dependency");
		}

		if (outputToOther)
		{
			// this step supplies an input of another step.
			return -1;
		}
		else if (inputToOther)
		{
			// this step requires an input from another step
			return 1;
		}
		else
		{
			return this.getType().getSortOrder() - other.getType().getSortOrder();
		}
	}

	/*-------------------------------------------------------------------------*/
	public List<Volume.Type> getSupportedIngredientAdditions()
	{
		return new ArrayList<Volume.Type>();
	}

	/*-------------------------------------------------------------------------*/
	public AdditionSchedule addIngredientAddition(Volume v)
	{
		AdditionSchedule schedule = new AdditionSchedule(v.getName(), 60);
		this.getIngredientAdditions().add(schedule);

		return schedule;
	}

	/*-------------------------------------------------------------------------*/
	public AdditionSchedule removeIngredientAddition(Volume v)
	{
		ListIterator<AdditionSchedule> iter = this.getIngredientAdditions().listIterator();

		while (iter.hasNext())
		{
			AdditionSchedule next = iter.next();
			if (v.getName().equals(next.getIngredientAddition()))
			{
				iter.remove();
				return next;
			}
		}

		throw new BrewdayException("Not found: "+v);
	}

	/*-------------------------------------------------------------------------*/
	public static enum Type
	{
		MASH("Mash", 1),
		MASH_INFUSION("Mash Infusion", 2),
		FIRST_RUNNING("First Running", 3),
		BATCH_SPARGE("Batch Sparge", 4),
		BOIL("Boil", 5),
		DILUTE("Dilute", 6),
		COOL("Cool", 7),
		FERMENT("Ferment", 8),
		STAND("Stand", 9),
		SPLIT_BY_PERCENT("Split (%)", 10),
		PACKAGE("Package", 11);

		private String name;
		private int sortOrder;

		Type(String name, int sortOrder)
		{
			this.name = name;
			this.sortOrder = sortOrder;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public int getSortOrder()
		{
			return sortOrder;
		}
	}
}
