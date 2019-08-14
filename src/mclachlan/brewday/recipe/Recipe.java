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

package mclachlan.brewday.recipe;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.process.*;

/**
 *
 */
public class Recipe implements V2DataObject
{
	/** Name of this recipe, is unique */
	private String name;

	/** Name of the equipment profile used for this recipe */
	private String equipmentProfile;

	/** List of process steps in this recipe */
	private List<ProcessStep> steps;

	//
	// dynamic fields:

	/** cache of the volumes created during processing*/
	private Volumes volumes;

	/** warnings created during processing */
	private List<String> warnings = new ArrayList<>();

	/** errors created during processing */
	private List<String> errors = new ArrayList<>();

	/*-------------------------------------------------------------------------*/
	public Recipe()
	{
		volumes = new Volumes();
	}

	/*-------------------------------------------------------------------------*/
	public Recipe(String name, String equipmentProfile, List<ProcessStep> steps)
	{
		this.name = name;
		this.equipmentProfile = equipmentProfile;
		this.steps = steps;
		volumes = new Volumes();
	}

	/*-------------------------------------------------------------------------*/
	public List<ProcessStep> getSteps()
	{
		return steps;
	}

	public void setSteps(List<ProcessStep> steps)
	{
		this.steps = steps;
	}

	/*-------------------------------------------------------------------------*/
	public String getEquipmentProfile()
	{
		return equipmentProfile;
	}

	/*-------------------------------------------------------------------------*/
	public void setEquipmentProfile(String equipmentProfile)
	{
		this.equipmentProfile = equipmentProfile;
	}

	/*-------------------------------------------------------------------------*/
	public Volumes getVolumes()
	{
		return volumes;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Runs the recipe end to end, populating created volumes and estimated data
	 * along the way. Clears computed volumes before running.
	 */
	public void run()
	{
		ErrorsAndWarnings log = new ErrorsAndWarnings();
		Volumes volumes = getVolumes();

		errors.clear();
		warnings.clear();
		clearComputedVolumes();

		sortSteps(log);

		EquipmentProfile equipment = Database.getInstance().getEquipmentProfiles().get(this.equipmentProfile);

		this.run(volumes, equipment, log);

		this.errors.addAll(log.getErrors());
		this.warnings.addAll(log.getWarnings());
	}

	/*-------------------------------------------------------------------------*/
	public void run(Volumes volumes, EquipmentProfile equipment, ErrorsAndWarnings log)
	{
		if (equipment == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", this.equipmentProfile));
			return;
		}

		for (ProcessStep s : getSteps())
		{
			try
			{
				s.apply(volumes, equipment, log);
			}
			catch (BrewdayException e)
			{
				errors.add(s.getName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void dryRun()
	{
		ErrorsAndWarnings log = new ErrorsAndWarnings();

		errors.clear();
		warnings.clear();
		clearComputedVolumes();

		sortSteps(log);

		for (ProcessStep s : getSteps())
		{
			try
			{
				s.dryRun(this, log);
			}
			catch (BrewdayException e)
			{
				errors.add(s.getName() + ": " + e.getMessage());
				e.printStackTrace();
				return;
			}
		}

		this.errors.addAll(log.getErrors());
		this.warnings.addAll(log.getWarnings());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Sorts the steps of this recipe in a sensible order. This method treats the
	 * process steps as a directed acyclic graph and performs a topological sort.
	 */
	public void sortSteps(ErrorsAndWarnings log)
	{
		// Steps should be an acyclic directed graph, and we want a topological sort.
		// Instead of proper graph topo sort algo we use this dirty hack instead.
		// Maybe if more graph-like behaviour emerges it'll be worth refactoring
		// the ProcessStep package into a proper graph representation, or wrapping it in one.

		ProcessStep[] wip = steps.toArray(new ProcessStep[0]);

		boolean swapping = true;

		while (swapping)
		{
			swapping = false;

			for (int i = wip.length-1; i >=0; i--)
			{
				ProcessStep p1 = wip[i];

				for (int j = i; j >=0; j--)
				{
					ProcessStep p2 = wip[j];

					boolean p1SuppliesP2 = !Collections.disjoint(p1.getOutputVolumes(), p2.getInputVolumes());
					boolean p2SuppliesP1 = !Collections.disjoint(p1.getInputVolumes(), p2.getOutputVolumes());

					if (p1SuppliesP2 && p2SuppliesP1)
					{
						// can't have this
						log.addError(
							StringUtils.getProcessString("recipe.error.circular.dependency",
								p1.getName(), p2.getName()));
						return;
					}

					if (p1SuppliesP2)
					{
						// swap the steps
						wip[i] = p2;
						wip[j] = p1;
						swapping = true;
					}
				}
			}
		}

		this.steps = new ArrayList<>(Arrays.asList(wip));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Clears computed volumes, leaving input volumes intact
	 */
	public void clearComputedVolumes()
	{
		this.volumes.clear();
	}

	/*-------------------------------------------------------------------------*/
	public String getName()
	{
		return name;
	}

	public void addError(String e)
	{
		this.errors.add(e);
	}

	public void addWarning(String w)
	{
		this.warnings.add(w);
	}

	public List<String> getErrors()
	{
		return this.errors;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}

	public String getUniqueStepName(ProcessStep.Type type)
	{
		int count = 0;
		for (ProcessStep step : getSteps())
		{
			if (step.getType() == type)
			{
				count++;
			}
		}

		return type.toString()+" #"+(count+1);
	}

	/*-------------------------------------------------------------------------*/
	public List<IngredientAddition> getIngredients()
	{
		List<IngredientAddition> result = new ArrayList<>();

		for (ProcessStep step : getSteps())
		{
			if (step.getSupportedIngredientAdditions().size() > 0)
			{
				result.addAll(step.getIngredients());
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public List<ProcessStep> getStepsForIngredient(IngredientAddition.Type ingredientType)
	{
		List<ProcessStep> result = new ArrayList<>();

		for (ProcessStep step : steps)
		{
			if (step.getSupportedIngredientAdditions().contains(ingredientType))
			{
				result.add(step);
			}
		}

		return result;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Apply the steps from the given recipe to this recipe, assigning
	 * ingredients as best we can.
	 */
	public void applyProcessTemplate(Recipe processTemplate)
	{
		List<ProcessStep> newSteps = new ArrayList<>();

		for (ProcessStep step : processTemplate.getSteps())
		{
			switch (step.getType())
			{
				case MASH:
					Mash mash = new Mash((Mash)step);
					mash.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(mash);
					break;

				case MASH_INFUSION:
					MashInfusion mashInfusion = new MashInfusion((MashInfusion)step);
					mashInfusion.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(mashInfusion);
					break;

				case BATCH_SPARGE:
					BatchSparge batchSparge = new BatchSparge((BatchSparge)step);
					batchSparge.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(batchSparge);
					break;

				case BOIL:
					Boil boil = new Boil((Boil)step);
					boil.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(boil);
					break;

				case DILUTE:
					Dilute dilute = new Dilute((Dilute)step);
					dilute.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(dilute);
					break;

				case COOL:
					Cool cool = new Cool((Cool)step);
					cool.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(cool);
					break;

				case FERMENT:
					Ferment ferment = new Ferment((Ferment)step);
					ferment.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(ferment);
					break;

				case STAND:
					Stand stand = new Stand((Stand)step);
					stand.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(stand);
					break;

				case SPLIT_BY_PERCENT:
					SplitByPercent split = new SplitByPercent((SplitByPercent)step);
					split.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(split);
					break;

				case PACKAGE:
					PackageStep packageStep = new PackageStep((PackageStep)step);
					packageStep.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(packageStep);
					break;
			}
		}

		this.steps.clear();
		this.steps.addAll(newSteps);
	}

	/*-------------------------------------------------------------------------*/

	private List<IngredientAddition> getIngredientsForStepType(ProcessStep.Type type)
	{
		List<IngredientAddition> result = new ArrayList<>();

		for (ProcessStep step : this.getSteps())
		{
			if (step.getType() == type && !step.getSupportedIngredientAdditions().isEmpty())
			{
				result.addAll(step.getIngredients());
			}
		}

		return result;
	}
}
