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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.ui.UiQuantityDisplay;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;

/**
 * Newtonian cooling rest on an existing wort or beer volume (no step ingredient additions).
 */
public class Stand extends FluidVolumeProcessStep
{
	/** stand duration */
	private TimeUnit duration;

	/** Newtonian cooling coefficient k (per hour); see {@link Equations#calcNewtonianCoolingEndTemperature}. */
	private double coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;

	/** read from legacy DB on import only; used by {@link mclachlan.brewday.db.StandStepMigration}. */
	private boolean legacyRemoveTrubAndChillerLoss;

	/*-------------------------------------------------------------------------*/
	public Stand()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Stand(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions)
	{
		super(name, description, Type.STAND, inputVolume, outputVolume);
		this.duration = duration;
		this.setIngredients(ingredientAdditions);
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.STAND), StringUtils.getProcessString("stand.desc"), Type.STAND, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setOutputVolume(StringUtils.getProcessString("stand.output", getName()));

		duration = new TimeUnit(30, Quantity.Unit.MINUTES, false);
		this.coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Stand step)
	{
		super(step.getName(), step.getDescription(), Type.STAND, step.getInputVolume(), step.getOutputVolume());

		this.duration = step.duration;
		this.coolingCoefficient = step.coolingCoefficient;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile,
		ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume input = volumes.getVolume(getInputVolume()).clone();

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
		// Stand ends at a lower temperature after the rest duration; cooling shrinkage concentrates
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
			input.getType(),
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

		PhVolumes.copyAll(input, volOut);
		HopAcidVolumes.copyAll(input, volOut);
		BitternessVolumes.syncReportedDerived(volOut, reportedFormulas);

		//
		// Carry inbound ingredient additions on the output volume.
		//
		List<IngredientAddition> carried = new ArrayList<>();
		if (input.getIngredientAdditions() != null)
		{
			for (IngredientAddition ia : input.getIngredientAdditions())
			{
				carried.add(ia.clone());
			}
		}
		volOut.setIngredientAdditions(carried);

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		String inputVolume = getInputVolume();
		if (inputVolume == null || inputVolume.isBlank())
		{
			log.addError(StringUtils.getProcessString("stand.input.required"));
			return false;
		}
		if (!volumes.contains(inputVolume))
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
		return List.of();
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
		return StringUtils.getProcessString("stand.step.desc", duration.get(Quantity.Unit.MINUTES));
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
	public boolean isLegacyRemoveTrubAndChillerLoss()
	{
		return legacyRemoveTrubAndChillerLoss;
	}

	/*-------------------------------------------------------------------------*/
	public void setLegacyRemoveTrubAndChillerLoss(boolean legacyRemoveTrubAndChillerLoss)
	{
		this.legacyRemoveTrubAndChillerLoss = legacyRemoveTrubAndChillerLoss;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		Volume volOut = getRecipe().getVolumes().getVolume(this.getOutputVolume());

		List<String> result = new ArrayList<>();
		result.add(
			StringUtils.getDocString(
				"stand.duration",
				this.getInputVolume(),
				this.duration.describe(Quantity.Unit.MINUTES),
				UiQuantityDisplay.describe(volOut.getTemperature())));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		Stand stand = new Stand(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			this.getOutputVolume(),
			new TimeUnit(this.duration.get()),
			cloneIngredients(this.getIngredientAdditions()));
		stand.setCoolingCoefficient(this.coolingCoefficient);
		return stand;
	}
}
