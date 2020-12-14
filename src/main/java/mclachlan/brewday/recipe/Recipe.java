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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
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
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 *
 */
public class Recipe implements V2DataObject
{
	/**
	 * Name of this recipe, is unique
	 */
	private String name;

	/**
	 * Free text notes about this recipe.
	 */
	private String description;

	/**
	 * Tags for this recipe.
	 */
	private List<String> tags = new ArrayList<>();

	/**
	 * Name of the equipment profile used for this recipe
	 */
	private String equipmentProfile;

	/**
	 * List of process steps in this recipe
	 */
	private List<ProcessStep> steps = new ArrayList<>();

	// dynamic fields:

	/**
	 * cache of the volumes created during processing
	 */
	private Volumes volumes;

	/**
	 * Log of recipe steps, warnings and errors.
	 */
	private ProcessLog log;

	/*-------------------------------------------------------------------------*/
	public Recipe()
	{
		volumes = new Volumes();
	}

	/*-------------------------------------------------------------------------*/
	public Recipe(
		String name,
		String description,
		String equipmentProfile,
		List<String> tags,
		List<ProcessStep> steps)
	{
		this.name = name;
		this.description = description;
		this.equipmentProfile = equipmentProfile;
		this.tags = tags;
		this.steps = steps;
		this.volumes = new Volumes();
		this.log = new ProcessLog();
	}

	/*-------------------------------------------------------------------------*/
	public Recipe(Recipe other)
	{
		this.name = other.getName();
		this.description = other.getDescription();
		this.equipmentProfile = other.equipmentProfile;
		this.volumes = new Volumes();
		this.log = new ProcessLog();
		this.steps = new ArrayList<>();

		for (ProcessStep ps : other.steps)
		{
			this.steps.add(ps.clone());
		}
	}

	/*-------------------------------------------------------------------------*/
	public Recipe(String name)
	{
		this.name = name;
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
	public String getDescription()
	{
		return description;
	}

	/*-------------------------------------------------------------------------*/
	public void setDescription(String description)
	{
		this.description = description;
	}

	/*-------------------------------------------------------------------------*/
	public List<String> getTags()
	{
		return tags;
	}

	/*-------------------------------------------------------------------------*/
	public void setTags(List<String> tags)
	{
		this.tags = tags;
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
		this.volumes = new Volumes();

		sortSteps(log);

		EquipmentProfile equipment = Database.getInstance().getEquipmentProfiles().get(this.equipmentProfile);

		this.run(volumes, equipment, log);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Runs the recipe end to end, populating the given volumes, equipment and log.
	 */
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
					if (volumes.contains(inputVolume))
					{
						Volume v = volumes.getVolume(inputVolume);
						log.addMessage(StringUtils.getProcessString("log.volume.in", v.describe(), v.getIngredientAdditions().size()));
					}
					else
					{
						log.addMessage(StringUtils.getProcessString("log.volume.missing", inputVolume));
					}
				}

				s.apply(volumes, equipment, log);

				for (String outputVolume : s.getOutputVolumes())
				{
					if (volumes.contains(outputVolume))
					{
						Volume v = volumes.getVolume(outputVolume);
						log.addMessage(StringUtils.getProcessString("log.volume.out", v.describe(), v.getIngredientAdditions().size()));
					}
					else
					{
						log.addMessage(StringUtils.getProcessString("log.volume.missing", outputVolume));
					}
				}
			}
			catch (BrewdayException e)
			{
				log.addError(s.getName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Performs a dry run of this recipe: steps validate their input volumes
	 * and produce their output volumes but do no other processing.
	 */
	public void dryRun()
	{
		this.log = new ProcessLog();
		this.volumes = new Volumes();

		dryRun(this.volumes, this.log);
	}

	/*-------------------------------------------------------------------------*/
	public void dryRun(Volumes volumes, ProcessLog log)
	{
		sortSteps(log);

		for (ProcessStep s : getSteps())
		{
			s.setRecipe(this);

			try
			{
				log.addMessage(StringUtils.getProcessString("log.step", s.getName()));

				for (String inputVolume : s.getInputVolumes())
				{
					Volume v = volumes.getVolume(inputVolume);
					log.addMessage(StringUtils.getProcessString("log.volume.in", v.describe(), "?"));
				}

				s.dryRun(this, log);

				for (String outputVolume : s.getOutputVolumes())
				{
					Volume v = volumes.getVolume(outputVolume);
					log.addMessage(StringUtils.getProcessString("log.volume.out", v.describe(), "?"));
				}
			}
			catch (BrewdayException e)
			{
				log.addError(s.getName() + ": " + e.getMessage());
				return;
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Sorts the steps of this recipe in a sensible order. This method treats the
	 * process steps as a directed acyclic graph and performs a topological
	 * sort.
	 */
	public void sortSteps(ProcessLog log)
	{
		DirectedAcyclicGraph<ProcessStep, String> graph =
			new DirectedAcyclicGraph<>(String.class);

		for (ProcessStep step : this.getSteps())
		{
			graph.addVertex(step);
		}

		for (ProcessStep step1 : this.getSteps())
		{
			for (String output : step1.getOutputVolumes())
			{
				for (ProcessStep step2 : this.getSteps())
				{
					for (String input : step2.getInputVolumes())
					{
						if (output.equals(input))
						{
							try
							{
								graph.addEdge(step1, step2, output);
							}
							catch (IllegalArgumentException e)
							{
								// the DAG throws this if adding the edge introduces a cycle

								log.addError(
									StringUtils.getProcessString("recipe.error.circular.dependency",
										step1.getName(), step2.getName()));
								return;
							}
						}
					}
				}
			}
		}

		TopologicalOrderIterator<ProcessStep, String> iter = new TopologicalOrderIterator<>(graph);

		this.steps = new ArrayList<>();
		while (iter.hasNext())
		{
			steps.add(iter.next());
		}
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

	/*-------------------------------------------------------------------------*/
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

		return type.toString() + " #" + (count + 1);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	All ingredients for all steps in this recipe. May contain duplicates
	 * 	if multiple steps have the same ingredient type added.
	 */
	public List<IngredientAddition> getIngredients()
	{
		List<IngredientAddition> result = new ArrayList<>();

		for (ProcessStep step : getSteps())
		{
			if (step.getSupportedIngredientAdditions().size() > 0)
			{
				result.addAll(step.getIngredientAdditions());
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public List<ProcessStep> getStepsForIngredient(
		IngredientAddition.Type ingredientType)
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

				case LAUTER:
					Lauter lauter = new Lauter((Lauter)step);
					newSteps.add(lauter);
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

				case HEAT:
					Heat heat = new Heat((Heat)step);
					heat.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(heat);
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

				case SPLIT:
					Split split = new Split((Split)step);
					split.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(split);
					break;

				case COMBINE:
					Combine combine = new Combine((Combine)step);
					combine.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(combine);
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

	private List<IngredientAddition> getIngredientsForStepType(
		ProcessStep.Type type)
	{
		List<IngredientAddition> result = new ArrayList<>();

		for (ProcessStep step : this.getSteps())
		{
			if (step.getType() == type && !step.getSupportedIngredientAdditions().isEmpty())
			{
				result.addAll(step.getIngredientAdditions());
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	The step which outputs the given input volume.
	 */
	public ProcessStep getStepProducingVolume(String inputVolume)
	{
		for (ProcessStep ps : getSteps())
		{
			if (ps.getOutputVolumes().contains(inputVolume))
			{
				return ps;
			}
		}
		return null;
	}

	/*-------------------------------------------------------------------------*/
	public ProcessStep getStepOfAddition(IngredientAddition ingredient)
	{
		for (ProcessStep ps : getSteps())
		{
			if (ps.getIngredientAdditions() != null && ps.getIngredientAdditions().contains(ingredient))
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
	 * @return The beers out out from this recipe, empty list if none
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
	public List<String> getAllVolumeNames()
	{
		List<String> result = new ArrayList<>();

		for (ProcessStep step : steps)
		{
			result.addAll(step.getOutputVolumes());
		}

		return result;
	}
}
