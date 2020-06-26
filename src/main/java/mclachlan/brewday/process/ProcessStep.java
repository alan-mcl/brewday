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
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public abstract class ProcessStep implements Comparable<ProcessStep>, IProcessStep, V2DataObject
{
	private String name;
	private String description;
	private Type type;
	private final List<IngredientAddition> ingredients = new ArrayList<>();
	private Recipe recipe;

	/*-------------------------------------------------------------------------*/
	protected ProcessStep()
	{
	}

	/*-------------------------------------------------------------------------*/
	public ProcessStep(String name, String description, Type type)
	{
		this.name = name;
		this.description = description;
		this.type = type;
	}

	/*-------------------------------------------------------------------------*/
	protected IngredientAddition getIngredientAddition(IngredientAddition.Type type)
	{
		for (IngredientAddition ia : getIngredients())
		{
			if (ia.getType() == type)
			{
				return ia;
			}
		}

		return null;
	}

	/*-------------------------------------------------------------------------*/
	protected List<IngredientAddition> getIngredientAdditions(IngredientAddition.Type type)
	{
		List<IngredientAddition> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredients())
		{
			if (ia.getType() == type)
			{
				result.add(ia);
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String getDescription()
	{
		return description;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Type getType()
	{
		return type;
	}

	@Override
	public List<IngredientAddition> getIngredients()
	{
		return ingredients;
	}

	@Override
	public void setIngredients(
		List<IngredientAddition> ingredients)
	{
		this.ingredients.clear();
		this.ingredients.addAll(ingredients);
		sortIngredients();
	}

	/*-------------------------------------------------------------------------*/
	protected void sortIngredients()
	{
		// sort ascending by time
		ingredients.sort((o1, o2) -> (int)(o1.getTime().get() - o2.getTime().get()));
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
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return new ArrayList<>();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void addIngredientAddition(IngredientAddition item)
	{
		this.getIngredients().add(item);
		sortIngredients();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void addIngredientAdditions(List<IngredientAddition> additions)
	{
		for (IngredientAddition addition : additions)
		{
			this.getIngredients().add(addition);
		}
		sortIngredients();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void removeIngredientAddition(IngredientAddition item)
	{
		this.ingredients.remove(item);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return a list of localised instructions for this step, empty list if none
	 */
	public List<String> getInstructions()
	{
		return Collections.emptyList();
	}

	/*-------------------------------------------------------------------------*/

	public Recipe getRecipe()
	{
		return recipe;
	}

	public void setRecipe(Recipe recipe)
	{
		this.recipe = recipe;
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public String toString()
	{
		return this.type+": "+this.name;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return a deep clone of this process step
	 */
	@Override
	public abstract ProcessStep clone();

	/*-------------------------------------------------------------------------*/
	protected List<IngredientAddition> cloneIngredients(List<IngredientAddition> other)
	{
		if (other == null)
		{
			return null;
		}

		List<IngredientAddition> result = new ArrayList<>();

		for (IngredientAddition ia : other)
		{
			result.add(ia.clone());
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static enum Type
	{
		MASH("Mash", "mash.desc", 1),
		MASH_INFUSION("Mash Infusion", "mash.infusion.desc", 2),
		BATCH_SPARGE("Batch Sparge", "batch.sparge.desc", 4),
		BOIL("Boil", "boil.desc", 5),
		DILUTE("Dilute", "dilute.desc", 6),
		COOL("Cool", "cool.desc", 7),
		FERMENT("Ferment", "ferment.desc", 8),
		STAND("Stand", "stand.desc", 9),
		SPLIT_BY_PERCENT("Split (%)", "split%.desc", 10),
		PACKAGE("Package", "package.desc", 11);

		private String name, descKey;
		private int sortOrder;

		Type(String name, String descKey, int sortOrder)
		{
			this.name = name;
			this.descKey = descKey;
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

		public String getDescKey()
		{
			return descKey;
		}
	}
}
