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
import mclachlan.brewday.style.Style;

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

	private ProcessLog log;

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
	public Recipe(Recipe other)
	{
		this.name = other.getName();
		this.equipmentProfile = other.equipmentProfile;
		volumes = new Volumes();
		this.steps = new ArrayList<>();
		for (ProcessStep ps : other.steps)
		{
			this.steps.add(ps.clone());
		}
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
		log = new ProcessLog();
		Volumes volumes = getVolumes();

		clearComputedVolumes();

		sortSteps(log);

		EquipmentProfile equipment = Database.getInstance().getEquipmentProfiles().get(this.equipmentProfile);

		this.run(volumes, equipment, log);
	}

	/*-------------------------------------------------------------------------*/
	public void run(Volumes volumes, EquipmentProfile equipment, ProcessLog log)
	{
		if (equipment == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", this.equipmentProfile));
			return;
		}

		for (ProcessStep s : getSteps())
		{
			s.setRecipe(this);

			try
			{
				log.addMessage(StringUtils.getProcessString("log.step", s.getName()));

				for (String inputVolume : s.getInputVolumes())
				{
					Volume v = volumes.getVolume(inputVolume);
					log.addMessage(StringUtils.getProcessString("log.volume.in", v.describe()));
				}

				s.apply(volumes, equipment, log);

				for (String outputVolume : s.getOutputVolumes())
				{
					Volume v = volumes.getVolume(outputVolume);
					log.addMessage(StringUtils.getProcessString("log.volume.out", v.describe()));
				}
			}
			catch (BrewdayException e)
			{
				log.addError(s.getName() + ": " + e.getMessage());
//				e.printStackTrace();
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void dryRun()
	{
		ProcessLog log = new ProcessLog();

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
				log.addError(s.getName() + ": " + e.getMessage());
				e.printStackTrace();
				return;
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Sorts the steps of this recipe in a sensible order. This method treats the
	 * process steps as a directed acyclic graph and performs a topological sort.
	 */
	public void sortSteps(ProcessLog log)
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

	public ProcessLog getLog()
	{
		return this.log;
	}

	public List<String> getErrors()
	{
		return this.log.getErrors();
	}

	public List<String> getWarnings()
	{
		return this.log.getWarnings();
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

	/*-------------------------------------------------------------------------*/
	public ProcessStep getStepOfAddition(IngredientAddition ingredient)
	{
		for (ProcessStep ps : getSteps())
		{
			if (ps.getIngredients() != null && ps.getIngredients().contains(ingredient))
			{
				return ps;
			}
		}
		return null;
	}

	/*-------------------------------------------------------------------------*/
	public void removeIngredient(IngredientAddition ia)
	{
		ProcessStep ps = getStepOfAddition(ia);
		ps.removeIngredientAddition(ia);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	The beers out out from this recipe, empty list if none
	 */
	public List<Volume> getBeers()
	{
		List<Volume> result = new ArrayList<>();

		for (ProcessStep step : getSteps())
		{
			if (step instanceof PackageStep)
			{
				for (String s : step.getOutputVolumes())
				{
					Volume volume = getVolumes().getVolume(s);

					if (volume.getType() == Volume.Type.BEER)
					{
						Volume vol = new Volume(volume.getName(), volume);
						if (vol.getStyle() == null)
						{
							Style errorStyle = new Style();
							errorStyle.setName("ERROR NO STYLE");
							errorStyle.setStyleGuide("ERROR NO STYLE");
							errorStyle.setStyleGuideName("ERROR NO STYLE");
							errorStyle.setStyleLetter("ERROR NO STYLE");
							errorStyle.setCategoryNumber("ERROR NO STYLE");
							errorStyle.setCategory("ERROR NO STYLE");
							vol.setStyle(errorStyle);
						}

						result.add(vol);
					}
				}
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	The total wort output from the given mash.
	 */
	public Volume getTotalMashOutput(Mash mash)
	{
		String outputVolume = mash.getOutputFirstRunnings();

		// we rely on the steps being sorted to encounter batch sparges in
		// order from first to last
		for (ProcessStep step : steps)
		{
			if (step instanceof BatchSparge)
			{
				// if this is a batch sparge with this mash as an input, gather the
				// combined runnings
				if (mash.getOutputMashVolume().equals(((BatchSparge)step).getMashVolume()))
				{
					outputVolume = ((BatchSparge)step).getOutputCombinedWortVolume();
				}
			}
		}

		return volumes.getVolume(outputVolume);
	}
}
