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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.BitternessUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.util.StringUtils;

/**
 *
 */
public abstract class ProcessStep
	implements Comparable<ProcessStep>, IProcessStep, V2DataObject
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
	protected IngredientAddition getIngredientAddition(
		IngredientAddition.Type type)
	{
		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == type)
			{
				return ia;
			}
		}

		return null;
	}

	/*-------------------------------------------------------------------------*/
	public List<WaterAddition> getWaterAdditions()
	{
		return (List<WaterAddition>)getIngredientAdditions(IngredientAddition.Type.WATER);
	}

	/*-------------------------------------------------------------------------*/
	public List<FermentableAddition> getFermentableAdditions()
	{
		return (List<FermentableAddition>)getIngredientAdditions(IngredientAddition.Type.FERMENTABLES);
	}

	/*-------------------------------------------------------------------------*/
	public List<HopAddition> getHopAdditions()
	{
		return (List<HopAddition>)getIngredientAdditions(IngredientAddition.Type.HOPS);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return A display string for a hop addition: "Name [Form], qty @ time",
	 * with the timing expressed in the given unit (minutes for kettle-side
	 * steps, days for dry/late hopping).
	 */
	protected static String describeHopAddition(HopAddition hop, Quantity.Unit timeUnit)
	{
		StringBuilder desc = new StringBuilder(hop.getName());
		if (hop.getForm() != null)
		{
			desc.append(" [").append(hop.getForm()).append("]");
		}
		desc.append(", ").append(hop.getQuantity().describe(hop.getUnit()));
		desc.append(" @ ").append(hop.getTime().describe(timeUnit));
		return desc.toString();
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return A "model: x.x IBU; ..." string covering every reported bitterness
	 * formula, for steps that isomerise hops (boil, whirlpool/hop-stand).
	 */
	protected static String formatPerFormulaBitterness(
		List<HopBitternessFormula> formulas,
		Map<HopBitternessFormula, BitternessUnit> contributions)
	{
		StringBuilder sb = new StringBuilder();
		for (HopBitternessFormula formula : formulas)
		{
			BitternessUnit ibu = contributions.get(formula);
			if (ibu == null)
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append("; ");
			}
			sb.append(formula.toString());
			sb.append(": ");
			sb.append(String.format("%.2f IBU", ibu.get(Quantity.Unit.IBU)));
		}
		return sb.toString();
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return A description of a dry/late hop addition's contribution to hop-acid
	 * inventory, for steps that do not isomerise hops (ferment, package). Such
	 * additions add raw alpha acids without measurable bitterness, except for
	 * pre-isomerized forms which contribute iso-alpha directly.
	 */
	protected static String formatDryHopAlpha(WeightUnit alphaMg, boolean preIsomerized)
	{
		double mg = alphaMg == null ? 0 : alphaMg.get(Quantity.Unit.MILLIGRAMS);
		if (preIsomerized)
		{
			return String.format("iso-alpha acids +%.0f mg (pre-isomerized)", mg);
		}
		return String.format(
			"alpha acids +%.0f mg (dry/late hop, no boil isomerisation)", mg);
	}

	/*-------------------------------------------------------------------------*/
	public List<YeastAddition> getYeastAdditions()
	{
		return (List<YeastAddition>)getIngredientAdditions(IngredientAddition.Type.YEAST);
	}

	/*-------------------------------------------------------------------------*/
	public List<MiscAddition> getMiscAdditions()
	{
		return (List<MiscAddition>)getIngredientAdditions(IngredientAddition.Type.MISC);
	}

	/*-------------------------------------------------------------------------*/
	private List<? extends IngredientAddition> getIngredientAdditions(
		IngredientAddition.Type type)
	{
		List<IngredientAddition> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == type)
			{
				result.add(ia);
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return The joint water profile addition of this step at the given time.
	 * This sums up all of the water additions, and all relevant misc additions
	 * with the same timestamp.
	 */
	public WaterAddition getCombinedWaterProfile(TimeUnit timeUnit)
	{
		Water resultWater = null;
		WaterAddition result = null;

		// blend the raw water additions
		for (IngredientAddition w : getIngredientAdditions(IngredientAddition.Type.WATER))
		{
			if (timeUnit == null || w.getTime().get(Quantity.Unit.MINUTES) == timeUnit.get(Quantity.Unit.MINUTES))
			{
				if (result == null)
				{
					resultWater = new Water(((WaterAddition)w).getWater());
					result = new WaterAddition(
						resultWater,
						(VolumeUnit)w.getQuantity(),
						w.getUnit(),
						((WaterAddition)w).getTemperature(),
						timeUnit);
				}
				else
				{
					// we already found a water, blend this one in

					result = result.getCombination((WaterAddition)w);
				}
			}
		}

		if (result != null)
		{
			// add the impact of any misc additions at the same time
			for (IngredientAddition m : getIngredientAdditions(IngredientAddition.Type.MISC))
			{
				if (timeUnit == null || m.getTime().get(Quantity.Unit.MINUTES) == timeUnit.get(Quantity.Unit.MINUTES))
				{
					Misc misc = ((MiscAddition)m).getMisc();
					if (misc.getType() == Misc.Type.WATER_AGENT &&
						misc.getWaterAdditionFormula() != null)
					{
						Water w = Equations.calcBrewingSaltAddition(result, (MiscAddition)m);
						result.setWater(w);
					}
				}
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
	public List<IngredientAddition> getIngredientAdditions()
	{
		return ingredients;
	}

	@Override
	public void setIngredients(
		List<IngredientAddition> ingredients)
	{
		this.ingredients.clear();
		if (ingredients != null)
		{
			this.ingredients.addAll(ingredients);
			sortIngredients();
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void sortIngredients()
	{
		// sort ascending by time
//		ingredients.sort((o1, o2) -> (int)(o1.getTime().get() - o2.getTime().get()));
		ingredients.sort(UiUtils.getIngredientAdditionComparator());
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
			throw new BrewdayException("Pipeline error: steps [" + this.getName() + "] " +
				"and [" + other.getName() + "] have a circular volume dependency");
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
		this.getIngredientAdditions().add(item);
		sortIngredients();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void addIngredientAdditions(List<IngredientAddition> additions)
	{
		for (IngredientAddition addition : additions)
		{
			this.getIngredientAdditions().add(addition);
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
		return this.type + ": " + this.name;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return a map of this step's configuration properties as human-readable
	 * key-value pairs, for verbose diagnostic logging
	 */
	public abstract Map<String, String> describeProperties();

	/*-------------------------------------------------------------------------*/

	/**
	 * @return a deep clone of this process step
	 */
	public abstract ProcessStep clone(String newName);

	/*-------------------------------------------------------------------------*/
	protected List<IngredientAddition> cloneIngredients(
		List<IngredientAddition> other)
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
		STEEP("Steep", "steep.desc", 3),
		LAUTER("Lauter", "lauter.desc", 4),
		BATCH_SPARGE("Batch Sparge", "batch.sparge.desc", 5),
		FLY_SPARGE("Fly Sparge", "fly.sparge.desc", 5),
		BOIL("Boil", "boil.desc", 6),
		DILUTE("Dilute", "dilute.desc", 7),
		HEAT("Heat", "heat.desc", 8),
		COOL("Cool", "cool.desc", 9),
		FERMENT("Ferment", "ferment.desc", 10),
		STAND("Stand", "stand.desc", 11),
		SPLIT("Split", "split.desc", 12),
		COMBINE("Combine", "combine.desc", 12),
		FREEZE_CONCENTRATE("Freeze Concentrate", "freeze.concentrate.desc", 14),
		PACKAGE("Package", "package.desc", 15);

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
