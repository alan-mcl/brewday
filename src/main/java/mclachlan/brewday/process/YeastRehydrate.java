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
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.ui.UiQuantityDisplay;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;

/**
 * Dry yeast rehydration in water without fermentable steeping or hop-stand chemistry.
 */
public class YeastRehydrate extends FluidVolumeProcessStep
{
	/** rehydration duration */
	private TimeUnit duration;

	/** Newtonian cooling coefficient k (per hour); see {@link Equations#calcNewtonianCoolingEndTemperature}. */
	private double coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;

	/*-------------------------------------------------------------------------*/
	public YeastRehydrate()
	{
	}

	/*-------------------------------------------------------------------------*/
	public YeastRehydrate(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions)
	{
		super(name, description, Type.YEAST_REHYDRATE, inputVolume, outputVolume);
		this.duration = duration;
		this.setIngredients(ingredientAdditions);
	}

	/*-------------------------------------------------------------------------*/
	public YeastRehydrate(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.YEAST_REHYDRATE),
			StringUtils.getProcessString("yeast.rehydrate.desc"), Type.YEAST_REHYDRATE, null, null);

		setOutputVolume(StringUtils.getProcessString("yeast.rehydrate.output", getName()));

		duration = new TimeUnit(30, Quantity.Unit.MINUTES, false);
		this.coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;
	}

	/*-------------------------------------------------------------------------*/
	public YeastRehydrate(YeastRehydrate step)
	{
		super(step.getName(), step.getDescription(), Type.YEAST_REHYDRATE, step.getInputVolume(),
			step.getOutputVolume());

		this.duration = step.duration;
		this.coolingCoefficient = step.coolingCoefficient;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile,
		ProcessLog log)
	{
		//
		// Input wort may be omitted when water additions define the rehydration liquor.
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
		// Water additions merge into the rehydration volume before cooling.
		//
		boolean foundWaterAddition = false;
		for (WaterAddition ia : getWaterAdditions())
		{
			foundWaterAddition = true;
			input = Equations.dilute(input, ia, input.getName());
		}

		if (getInputVolume() == null && !foundWaterAddition)
		{
			log.addError(StringUtils.getProcessString("yeast.rehydrate.no.water.additions"));
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

		TemperatureUnit ambient = Equations.resolveEquipmentAmbientTemperature(equipmentProfile);
		double coolingK = getCoolingCoefficient();

		//
		// Rehydration ends at a lower temperature after the rest duration; cooling shrinkage concentrates
		// gravity, ABV, and colour on the smaller volume.
		//
		TemperatureUnit tempOut = Equations.calcNewtonianCoolingEndTemperature(
			input.getTemperature(),
			ambient,
			coolingK,
			getDuration());

		VolumeUnit volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(),
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

		// Rehydration does not alter wort pH; carry it forward unchanged.
		PhVolumes.copyAll(input, volOut);

		HopAcidVolumes.copyAll(input, volOut);

		BitternessVolumes.syncReportedDerived(volOut, reportedFormulas);

		//
		// Carry step additions (yeast, misc, water) on the output volume.
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
		// Yeast rehydrate step supports being the first in a recipe

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
			IngredientAddition.Type.YEAST,
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
		return StringUtils.getProcessString("yeast.rehydrate.step.desc", duration.get(Quantity.Unit.MINUTES));
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
			if (ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"yeast.rehydrate.misc.addition",
						ia.describe(),
						ia.getTime().describe(MINUTES)));
			}
			else if (ia.getType() == IngredientAddition.Type.WATER)
			{
				result.add(
					StringUtils.getDocString(
						"yeast.rehydrate.water.addition",
						ia.describe()));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		result.add(
			StringUtils.getDocString(
				"yeast.rehydrate.duration",
				this.getInputVolume(),
				this.duration.describe(Quantity.Unit.MINUTES),
				UiQuantityDisplay.describe(volOut.getTemperature())));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		YeastRehydrate yeastRehydrate = new YeastRehydrate(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			this.getOutputVolume(),
			new TimeUnit(this.duration.get()),
			cloneIngredients(this.getIngredientAdditions()));
		yeastRehydrate.setCoolingCoefficient(this.coolingCoefficient);
		return yeastRehydrate;
	}
}
