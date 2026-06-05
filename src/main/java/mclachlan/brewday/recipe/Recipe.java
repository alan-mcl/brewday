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

import java.util.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.style.Style;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.util.Log;
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
		this.tags = new ArrayList<>(other.tags);

		for (ProcessStep ps : other.steps)
		{
			this.steps.add(ps.clone(ps.getName()));
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
		run(false);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Runs the recipe end to end. When verbose is true, the log includes extra
	 * detail per step: step type, configuration properties, and individual
	 * ingredient additions.
	 */
	public void run(boolean verbose)
	{
		log = new ProcessLog();
		this.volumes = new Volumes();

		sortSteps(log);

		EquipmentProfile equipment = Database.getInstance().getEquipmentProfiles().get(this.equipmentProfile);

		this.run(volumes, equipment, log, verbose);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Runs the recipe end to end, populating the given volumes, equipment and log.
	 */
	public void run(Volumes volumes, EquipmentProfile equipment, ProcessLog log)
	{
		run(volumes, equipment, log, false);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Runs the recipe end to end, populating the given volumes, equipment and log.
	 * When verbose is true, extra diagnostic detail is emitted per step.
	 */
	public void run(Volumes volumes, EquipmentProfile equipment, ProcessLog log, boolean verbose)
	{
		log.setVerbose(verbose);

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

				logStepDetail(s, log);

				for (String inputVolume : s.getInputVolumes())
				{
					if (volumes.contains(inputVolume))
					{
						Volume v = volumes.getVolume(inputVolume);
						log.addMessage(StringUtils.getProcessString("log.volume.in", v.describeOneLine(), v.getIngredientAdditions().size()));
					}
					else
					{
						log.addMessage(StringUtils.getProcessString("log.volume.missing", inputVolume));
					}
				}

				s.apply(volumes, equipment, log);

				if (verbose)
				{
					logVerboseAdditions(s, volumes, equipment, log);
				}

				for (String outputVolume : s.getOutputVolumes())
				{
					if (volumes.contains(outputVolume))
					{
						Volume v = volumes.getVolume(outputVolume);
						log.addMessage(StringUtils.getProcessString("log.volume.out", v.describeOneLine(), v.getIngredientAdditions().size()));
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
				Brewday.getInstance().getLog().log(Log.LOUD, e);
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

				logStepDetail(s, log);

				for (String inputVolume : s.getInputVolumes())
				{
					Volume v = volumes.getVolume(inputVolume);
					log.addMessage(StringUtils.getProcessString("log.volume.in", v.describeOneLine(), "?"));
				}

				s.dryRun(this, log);

				for (String outputVolume : s.getOutputVolumes())
				{
					Volume v = volumes.getVolume(outputVolume);
					log.addMessage(StringUtils.getProcessString("log.volume.out", v.describeOneLine(), "?"));
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
	 * Logs a single line describing the step type and all its configured
	 * properties. Emitted in both default and verbose modes.
	 */
	private void logStepDetail(ProcessStep s, ProcessLog log)
	{
		String props = "";

		Map<String, String> map = s.describeProperties();
		if (map != null && !map.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Map.Entry<String, String> e : map.entrySet())
			{
				if (!first)
				{
					sb.append(", ");
				}
				sb.append(e.getKey()).append("=").append(e.getValue());
				first = false;
			}
			props = sb.toString();
		}

		log.addMessage(StringUtils.getProcessString("log.step.detail", s.getType().name(), props));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Logs each ingredient addition of the given step on a single line,
	 * including the addition type, quantity, time and any calculated metrics
	 * that can be derived (grist %, gravity contribution, bitterness
	 * contribution). Verbose mode only; called after {@link ProcessStep#apply}
	 * so that computed volumes are available.
	 */
	private void logVerboseAdditions(
		ProcessStep s,
		Volumes volumes,
		EquipmentProfile equipment,
		ProcessLog log)
	{
		Volume inputVol = firstFluidVolume(s.getInputVolumes(), volumes);
		Volume outputVol = firstFluidVolume(s.getOutputVolumes(), volumes);

		for (IngredientAddition ia : s.getIngredientAdditions())
		{
			// don't do hop additions because the steps themselves have it in
			if(ia.getType() != IngredientAddition.Type.HOPS)
			{
				String metrics = describeAdditionMetrics(s, ia, inputVol, outputVol, equipment);

				log.addMessage(StringUtils.getProcessString(
					"log.addition",
					ia.getType().name(),
					ia.describe(),
					ia.getTime().get(Quantity.Unit.MINUTES),
					metrics));
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Best-effort recomputation of calculated metrics for a single ingredient
	 * addition, used for verbose logging only. Returns a suffix string (may be
	 * empty) appended to the addition log line. Values are approximate: they
	 * are recomputed from the step's input/output volumes rather than read from
	 * the step's internal calculation, so they may differ slightly from the
	 * metrics applied to the volume.
	 */
	private String describeAdditionMetrics(
		ProcessStep s,
		IngredientAddition ia,
		Volume inputVol,
		Volume outputVol,
		EquipmentProfile equipment)
	{
		try
		{
			if (ia instanceof FermentableAddition fa)
			{
				Fermentable.Type type = fa.getFermentable().getType();

				if (type == Fermentable.Type.GRAIN || type == Fermentable.Type.ADJUNCT)
				{
					// grist percentage by weight across this step's grain bill
					List<FermentableAddition> grainBill = new ArrayList<>();
					for (IngredientAddition item : s.getIngredientAdditions())
					{
						if (item instanceof FermentableAddition)
						{
							grainBill.add((FermentableAddition)item);
						}
					}
					double total = Equations.calcTotalGrainWeight(grainBill).get(Quantity.Unit.GRAMS);
					if (total > 0)
					{
						double perc = fa.getQuantity().get(Quantity.Unit.GRAMS) / total * 100D;
						return StringUtils.getProcessString("log.addition.grist", perc);
					}
				}
				else if (inputVol != null && inputVol.getVolume() != null)
				{
					// soluble fermentable: gravity contribution
					DensityUnit gravity = Equations.calcSteepedFermentableAdditionGravity(fa, inputVol.getVolume());
					return StringUtils.getProcessString("log.addition.gravity", gravity.get(Quantity.Unit.GU));
				}
			}
			else if (ia instanceof HopAddition ha)
			{
				List<Settings.HopBitternessFormula> formulas =
					Settings.parseReportedFormulas(Database.getInstance().getSettings());

				if (!formulas.isEmpty()
					&& inputVol != null && inputVol.getVolume() != null && inputVol.getGravity() != null
					&& outputVol != null && outputVol.getVolume() != null && outputVol.getGravity() != null)
				{
					Settings.HopBitternessFormula formula = formulas.get(0);
					BitternessUnit ibu = Brewday.getInstance().getHopAdditionIBU(
						equipment,
						inputVol.getVolume(),
						inputVol.getGravity(),
						outputVol.getVolume(),
						outputVol.getGravity(),
						ha,
						formula);
					return StringUtils.getProcessString("log.addition.ibu",
						formula.toString(), ibu.get(Quantity.Unit.IBU));
				}
			}
		}
		catch (Exception e)
		{
			// metrics are best-effort diagnostics; never fail the run for them
			Brewday.getInstance().getLog().log(Log.LOUD, e);
		}

		return "";
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return the first MASH/WORT/BEER volume from the given volume ids that
	 * 	exists in {@code volumes}, or null if none.
	 */
	private Volume firstFluidVolume(Collection<String> volumeIds, Volumes volumes)
	{
		for (String id : volumeIds)
		{
			if (volumes.contains(id))
			{
				Volume v = volumes.getVolume(id);
				if (v.getType() == Volume.Type.MASH
					|| v.getType() == Volume.Type.WORT
					|| v.getType() == Volume.Type.BEER)
				{
					return v;
				}
			}
		}
		return null;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Populates {@code graph} with vertices ({@link ProcessStep}) and edges (volume id
	 * strings) using the same rules as {@link #sortSteps(ProcessLog)}: an edge
	 * {@code step1 -> step2} exists when some output volume id of {@code step1}
	 * equals an input volume id of {@code step2}. Does not modify {@link #getSteps()}.
	 *
	 * @return false if a cycle is detected (error is appended to {@code log})
	 */
	public boolean buildProcessStepDag(DirectedAcyclicGraph<ProcessStep, String> graph, ProcessLog log)
	{
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
								return false;
							}
						}
					}
				}
			}
		}
		return true;
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

		if (!buildProcessStepDag(graph, log))
		{
			return;
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

	/**
	 * @return
	 * 	The ingredients in this recipe, de-duplicated and sorted as follows:
	 * 	water, grains, hops, yeast, misc
	 */
	public List<IngredientAddition> getIngredientsBillOfMaterials()
	{
		List<IngredientAddition> ingredients = getIngredients();

		Map<String, IngredientAddition> additionMap = new HashMap<>();

		for (IngredientAddition ia : ingredients)
		{
			if (!additionMap.containsKey(ia.getName()))
			{
				// just add to the map
				additionMap.put(ia.getName(), ia.clone());
			}
			else
			{
				IngredientAddition current = additionMap.get(ia.getName());
				double total = current.getQuantity().get(current.getUnit()) + ia.getQuantity().get(current.getUnit());
				current.setQuantity(Quantity.parseQuantity(""+total, current.getUnit()));
			}
		}

		List<IngredientAddition> result = new ArrayList<>(additionMap.values());

		result.sort(UiUtils.getIngredientAdditionComparator());

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

				case FLY_SPARGE:
					FlySparge flySparge = new FlySparge((FlySparge)step);
					flySparge.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(flySparge);
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

				case STEEP:
					Steep steep = new Steep((Steep)step);
					steep.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(steep);
					break;

				case HOP_STAND:
					HopStand hopStand = new HopStand((HopStand)step);
					hopStand.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(hopStand);
					break;

				case YEAST_REHYDRATE:
					YeastRehydrate yeastRehydrate = new YeastRehydrate((YeastRehydrate)step);
					yeastRehydrate.addIngredientAdditions(this.getIngredientsForStepType(step.getType()));
					newSteps.add(yeastRehydrate);
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

	/*-------------------------------------------------------------------------*/
	public boolean containsStepWithName(String newValue)
	{
		for (ProcessStep ps : steps)
		{
			if (ps.getName().equals(newValue))
			{
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Renames an output volume across this recipe.
	 * <p>
	 * Updates the name on the producing step, rewrites any downstream input
	 * references on consuming steps, and finally updates the runtime
	 * {@link Volumes} registry. The step DAG topology is preserved, so no
	 * re-sort or rerun is required.
	 *
	 * @throws BrewdayException if either name is null/blank, if they are equal,
	 * or if {@code newName} already exists as a volume name in this recipe.
	 */
	public void renameVolume(String oldName, String newName)
	{
		if (oldName == null || oldName.isBlank())
		{
			throw new BrewdayException("old volume name is null or blank");
		}
		if (newName == null || newName.isBlank())
		{
			throw new BrewdayException("new volume name is null or blank");
		}
		if (oldName.equals(newName))
		{
			throw new BrewdayException("old and new volume names are equal ["+oldName+"]");
		}
		if (getAllVolumeNames().contains(newName))
		{
			throw new BrewdayException("volume already exists ["+newName+"]");
		}

		for (ProcessStep step : steps)
		{
			if (step.getOutputVolumes().contains(oldName))
			{
				renameStepOutputVolume(step, oldName, newName);
			}
			if (step.getInputVolumes().contains(oldName))
			{
				renameStepInputVolume(step, oldName, newName);
			}
		}

		volumes.renameVolume(oldName, newName);
	}

	/*-------------------------------------------------------------------------*/
	private void renameStepOutputVolume(ProcessStep step, String oldName, String newName)
	{
		if (step instanceof Mash m)
		{
			if (oldName.equals(m.getOutputMashVolume()))
			{
				m.setOutputMashVolume(newName);
			}
		}
		else if (step instanceof MashInfusion mi)
		{
			if (oldName.equals(mi.getOutputMashVolume()))
			{
				mi.setOutputMashVolume(newName);
			}
		}
		else if (step instanceof Lauter l)
		{
			if (oldName.equals(l.getOutputLauteredMashVolume()))
			{
				l.setOutputLauteredMashVolume(newName);
			}
			if (oldName.equals(l.getOutputFirstRunnings()))
			{
				l.setOutputFirstRunnings(newName);
			}
		}
		else if (step instanceof BatchSparge bs)
		{
			if (oldName.equals(bs.getOutputCombinedWortVolume()))
			{
				bs.setOutputCombinedWortVolume(newName);
			}
			if (oldName.equals(bs.getOutputMashVolume()))
			{
				bs.setOutputMashVolume(newName);
			}
			if (oldName.equals(bs.getOutputSpargeRunnings()))
			{
				bs.setOutputSpargeRunnings(newName);
			}
		}
		else if (step instanceof FlySparge fs)
		{
			if (oldName.equals(fs.getOutputCollectedWort()))
			{
				fs.setOutputCollectedWort(newName);
			}
			if (oldName.equals(fs.getOutputSpentGrain()))
			{
				fs.setOutputSpentGrain(newName);
			}
		}
		else if (step instanceof Boil b)
		{
			if (oldName.equals(b.getOutputWortVolume()))
			{
				b.setOutputWortVolume(newName);
			}
			if (oldName.equals(b.getOutputTrubVolume()))
			{
				b.setOutputTrubVolume(newName);
			}
		}
		else if (step instanceof Split sp)
		{
			if (oldName.equals(sp.getOutputVolume()))
			{
				sp.setOutputVolume(newName);
			}
			if (oldName.equals(sp.getOutputVolume2()))
			{
				sp.setOutputVolume2(newName);
			}
		}
		else if (step instanceof FluidVolumeProcessStep fv)
		{
			if (oldName.equals(fv.getOutputVolume()))
			{
				fv.setOutputVolume(newName);
			}
		}
		else
		{
			throw new BrewdayException("unsupported step type for output rename: "+step.getClass().getSimpleName());
		}
	}

	/*-------------------------------------------------------------------------*/
	private void renameStepInputVolume(ProcessStep step, String oldName, String newName)
	{
		if (step instanceof Mash m)
		{
			if (oldName.equals(m.getInputMashVolume()))
			{
				m.setInputMashVolume(newName);
			}
		}
		else if (step instanceof MashInfusion mi)
		{
			if (oldName.equals(mi.getInputMashVolume()))
			{
				mi.setInputMashVolume(newName);
			}
		}
		else if (step instanceof Lauter l)
		{
			if (oldName.equals(l.getInputMashVolume()))
			{
				l.setInputMashVolume(newName);
			}
		}
		else if (step instanceof BatchSparge bs)
		{
			if (oldName.equals(bs.getMashVolume()))
			{
				bs.setMashVolume(newName);
			}
			if (oldName.equals(bs.getWortVolume()))
			{
				bs.setWortVolume(newName);
			}
		}
		else if (step instanceof FlySparge fs)
		{
			if (oldName.equals(fs.getInputMashVolume()))
			{
				fs.setInputMashVolume(newName);
			}
		}
		else if (step instanceof Boil b)
		{
			if (oldName.equals(b.getInputWortVolume()))
			{
				b.setInputWortVolume(newName);
			}
		}
		else if (step instanceof Combine c)
		{
			if (oldName.equals(c.getInputVolume()))
			{
				c.setInputVolume(newName);
			}
			if (oldName.equals(c.getInputVolume2()))
			{
				c.setInputVolume2(newName);
			}
		}
		else if (step instanceof FluidVolumeProcessStep fv)
		{
			if (oldName.equals(fv.getInputVolume()))
			{
				fv.setInputVolume(newName);
			}
		}
		else
		{
			throw new BrewdayException("unsupported step type for input rename: "+step.getClass().getSimpleName());
		}
	}
}
