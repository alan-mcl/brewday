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
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;

/**
 * Specialty-grain and extract steeping without enzymatic conversion.
 */
public class Steep extends FluidVolumeProcessStep
{
	/** steep duration */
	private TimeUnit duration;

	/** Newtonian cooling coefficient k (per hour); see {@link Equations#calcNewtonianCoolingEndTemperature}. */
	private double coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;

	/*-------------------------------------------------------------------------*/
	public Steep()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Steep(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions)
	{
		super(name, description, Type.STEEP, inputVolume, outputVolume);
		this.duration = duration;
		this.setIngredients(ingredientAdditions);
	}

	/*-------------------------------------------------------------------------*/
	public Steep(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.STEEP), StringUtils.getProcessString("steep.desc"), Type.STEEP, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setOutputVolume(StringUtils.getProcessString("steep.output", getName()));

		duration = new TimeUnit(30, Quantity.Unit.MINUTES, false);
		this.coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;
	}

	/*-------------------------------------------------------------------------*/
	public Steep(Steep step)
	{
		super(step.getName(), step.getDescription(), Type.STEEP, step.getInputVolume(), step.getOutputVolume());

		this.duration = step.duration;
		this.coolingCoefficient = step.coolingCoefficient;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile,
		ProcessLog log)
	{
		//
		// Input wort may be omitted when liquor additions define the steep volume (extract-style).
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume input;
		if (getInputVolume() != null)
		{
			input = volumes.getVolume(getInputVolume());
		}
		else
		{
			input = new Volume("water volume",
				Volume.Type.WORT,
				new VolumeUnit(0),
				new TemperatureUnit(20, Quantity.Unit.CELSIUS),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new PercentageUnit(0),
				new ColourUnit(0, Quantity.Unit.SRM),
				new BitternessUnit(0, Quantity.Unit.IBU));
		}

		//
		// Water additions merge into the steep volume before extraction calculations.
		//
		boolean foundWaterAddition = false;
		for (WaterAddition ia : getWaterAdditions())
		{
			foundWaterAddition = true;
			input = Equations.dilute(input, ia, input.getName());
		}

		if (getInputVolume() == null && !foundWaterAddition)
		{
			log.addError(StringUtils.getProcessString("steep.no.water.additions"));
			return;
		}

		//
		// Clone a named input volume so calculations do not alter the registry entry
		// still referenced by upstream steps.
		//
		if (getInputVolume() != null)
		{
			input = input.clone();
		}

		List<HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());

		DensityUnit gravityIn = input.getGravity();
		ColourUnit colourIn = input.getColour();
		Map<HopBitternessFormula, BitternessUnit> bitternessByFormula = new LinkedHashMap<>();
		for (HopBitternessFormula formula : reportedFormulas)
		{
			bitternessByFormula.put(formula, BitternessVolumes.copyOrZero(input, formula));
		}

		//
		// Steeped or soluble fermentables adjust gravity, colour, and any modeled bitterness.
		//
		List<FermentableAddition> steepedGrains = new ArrayList<>();
		for (FermentableAddition fa : getFermentableAdditions())
		{
			DensityUnit gravity = Equations.calcSteepedFermentableAdditionGravity(fa, input.getVolume());
			gravityIn = new DensityUnit(gravityIn.get() + gravity.get());

			if (fa.getFermentable().getType() == Fermentable.Type.GRAIN || fa.getFermentable().getType() == Fermentable.Type.ADJUNCT)
			{
				steepedGrains.add(fa);
			}
			else
			{
				ColourUnit col = Equations.calcSolubleFermentableAdditionColourContribution(fa, input.getVolume());
				colourIn = new ColourUnit(colourIn.get() + col.get());
			}

			BitternessUnit ibu = Equations.calcSolubleFermentableAdditionBitternessContribution(fa, input.getVolume());
			for (HopBitternessFormula formula : reportedFormulas)
			{
				bitternessByFormula.get(formula).add(ibu);
			}
		}
		if (!steepedGrains.isEmpty())
		{
			ColourUnit col = Equations.calcColourSrmMoreyFormula(steepedGrains, input.getVolume());
			colourIn = new ColourUnit(colourIn.get() + col.get());
		}

		TemperatureUnit ambient = Equations.resolveEquipmentAmbientTemperature(equipmentProfile);
		double coolingK = getCoolingCoefficient();

		//
		// Grain removal: apparent water absorption (1 L/kg) reduces runoff volume before cooling.
		//
		VolumeUnit volumeAfterGrainRemoval = input.getVolume();
		if (!steepedGrains.isEmpty())
		{
			VolumeUnit grainAbsorptionLoss = Equations.calcAbsorbedWater(steepedGrains, 0.0);
			volumeAfterGrainRemoval = new VolumeUnit(
				input.getVolume().get() - grainAbsorptionLoss.get());
			if (volumeAfterGrainRemoval.get() <= 0)
			{
				log.addError(StringUtils.getProcessString("steep.grain.absorption.exceeds.volume",
					grainAbsorptionLoss.get(Quantity.Unit.LITRES),
					input.getVolume().get(Quantity.Unit.LITRES)));
				return;
			}
			if (grainAbsorptionLoss.get() > 0)
			{
				log.addVerboseMessage(StringUtils.getProcessString("steep.grain.absorption.loss",
					grainAbsorptionLoss.get(Quantity.Unit.LITRES)));
			}
		}

		//
		// Steep ends at a lower temperature after the rest duration; cooling shrinkage concentrates
		// gravity, ABV, and colour on the smaller volume.
		//
		TemperatureUnit tempOut = Equations.calcNewtonianCoolingEndTemperature(
			input.getTemperature(),
			ambient,
			coolingK,
			getDuration());

		VolumeUnit volumeOut = Equations.calcCoolingShrinkage(
			volumeAfterGrainRemoval,
			new TemperatureUnit(input.getTemperature().get(Quantity.Unit.CELSIUS)
				- tempOut.get(Quantity.Unit.CELSIUS)));

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), gravityIn, volumeOut);
		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);
		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), colourIn, volumeOut);

		Volume volOut = new Volume(
			getOutputVolume(),
			Volume.Type.WORT,
			volumeOut,
			tempOut,
			input.getFermentability(),
			gravityOut,
			abvOut,
			colourOut,
			BitternessVolumes.zero());
		for (HopBitternessFormula formula : reportedFormulas)
		{
			BitternessVolumes.set(volOut, formula, bitternessByFormula.get(formula));
		}

		// A steep does not alter wort pH; carry it forward unchanged.
		PhVolumes.copyAll(input, volOut);

		HopAcidVolumes.copyAll(input, volOut);

		BitternessVolumes.syncReportedDerived(volOut, reportedFormulas);

		//
		// Carry step additions (misc, water, fermentables) on the output volume.
		//
		List<IngredientAddition> carried = new ArrayList<>();
		if (input.getIngredientAdditions() != null)
		{
			for (IngredientAddition ia : input.getIngredientAdditions())
			{
				carried.add(ia.clone());
			}
		}
		for (IngredientAddition ia : getIngredientAdditions())
		{
			carried.add(ia.clone());
		}
		volOut.setIngredientAdditions(carried);

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		if (getInputVolume() == null)
		{
			recipe.getVolumes().addVolume(getOutputVolume(), new Volume(getOutputVolume(), Volume.Type.WORT));
		}
		else
		{
			super.dryRun(recipe, log);
		}
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		// Steep step supports being the first in a recipe

		String inputVolume = getInputVolume();
		if (inputVolume != null && !volumes.contains(inputVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputVolume));
			return false;
		}
		return true;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(
			IngredientAddition.Type.WATER,
			IngredientAddition.Type.FERMENTABLES,
			IngredientAddition.Type.MISC);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("duration", duration == null ? "null" : duration.get(Quantity.Unit.MINUTES) + "min");
		result.put("coolingCoefficient", String.valueOf(coolingCoefficient));
		result.put("inputVolume", String.valueOf(getInputVolume()));
		result.put("outputVolume", String.valueOf(getOutputVolume()));
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("steep.step.desc", duration.get(Quantity.Unit.MINUTES));
	}

	/*-------------------------------------------------------------------------*/
	public TimeUnit getDuration()
	{
		return duration;
	}

	/*-------------------------------------------------------------------------*/
	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public double getCoolingCoefficient()
	{
		return coolingCoefficient;
	}

	/*-------------------------------------------------------------------------*/
	public void setCoolingCoefficient(double coolingCoefficient)
	{
		this.coolingCoefficient = coolingCoefficient;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		Volume volOut = getRecipe().getVolumes().getVolume(this.getOutputVolume());

		List<String> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"steep.fermentable.addition",
						ia.describe()));
			}
			else if (ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"steep.misc.addition",
						ia.describe(),
						ia.getTime().describe(MINUTES)));
			}
			else if (ia.getType() == IngredientAddition.Type.WATER)
			{
				result.add(
					StringUtils.getDocString(
						"steep.water.addition",
						ia.describe()));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		result.add(
			StringUtils.getDocString(
				"steep.duration",
				this.getInputVolume(),
				this.duration.describe(Quantity.Unit.MINUTES),
				volOut.getTemperature().describe(Quantity.Unit.CELSIUS)));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		Steep steep = new Steep(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			this.getOutputVolume(),
			new TimeUnit(this.duration.get()),
			cloneIngredients(this.getIngredientAdditions()));
		steep.setCoolingCoefficient(this.coolingCoefficient);
		return steep;
	}
}
