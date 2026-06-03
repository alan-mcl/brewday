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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;

/**
 *
 */
public class Stand extends FluidVolumeProcessStep
{
	/**
	 * stand duration
	 */
	private TimeUnit duration;

	/** equipment-profile kettle trub and chiller loss removed from outbound wort */
	private boolean removeTrubAndChillerLoss;

	/** Newtonian cooling coefficient k (per hour); see {@link Equations#calcNewtonianCoolingEndTemperature}. */
	private double coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;

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
		this(name, description, inputVolume, outputVolume, duration, ingredientAdditions, false);
	}

	/*-------------------------------------------------------------------------*/
	public Stand(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions,
		boolean removeTrubAndChillerLoss)
	{
		super(name, description, Type.STAND, inputVolume, outputVolume);
		this.duration = duration;
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
		this.setIngredients(ingredientAdditions);
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.STAND), StringUtils.getProcessString("stand.desc"), Type.STAND, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setOutputVolume(StringUtils.getProcessString("stand.output", getName()));

		duration = new TimeUnit(30, Quantity.Unit.MINUTES, false);
		this.removeTrubAndChillerLoss = false;
		this.coolingCoefficient = Equations.DEFAULT_STAND_COOLING_COEFFICIENT;
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Stand step)
	{
		super(step.getName(), step.getDescription(), Type.STAND, step.getInputVolume(), step.getOutputVolume());

		this.duration = step.duration;
		this.removeTrubAndChillerLoss = step.removeTrubAndChillerLoss;
		this.coolingCoefficient = step.coolingCoefficient;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile,
		ProcessLog log)
	{
		//
		// Input wort may be omitted when liquor additions define the stand volume (extract-style).
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
		// Water additions merge into the stand volume before hop-stand and steeping calculations.
		//
		boolean foundWaterAddition = false;
		for (WaterAddition ia : getWaterAdditions())
		{
			foundWaterAddition = true;
			input = Equations.dilute(input, ia, input.getName());
		}

		if (getInputVolume() == null && !foundWaterAddition)
		{
			log.addError(StringUtils.getProcessString("stand.no.water.additions"));
			return;
		}

		//
		// Clone a named input volume so stand IBU accumulation does not alter the registry entry
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
		// Steeped or soluble fermentables on the stand adjust gravity, colour, and any modeled bitterness
		// before whirlpool/hop-stand IBU is calculated.
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
		TemperatureUnit wortTemp = input.getTemperature();
		double coolingK = getCoolingCoefficient();

		//
		// Whirlpool / hop-stand: post-boil isomerisation at sub-boiling temperature adds IBU (MIBU uses
		// Newtonian cooling on Stand; other formulas share the same cooling model).
		//
		double equipUtil = equipmentProfile.getHopUtilisation().get();

		BitternessUnit commonHopStandIbu = new BitternessUnit(0);
		for (HopAddition hop : getHopAdditions())
		{
			Map<HopBitternessFormula, BitternessUnit> perHopIbu = new LinkedHashMap<>();
			for (HopBitternessFormula formula : reportedFormulas)
			{
				BitternessUnit hopIbu;
				if (formula == HopBitternessFormula.MIBU)
				{
					TimeUnit boilTime = new TimeUnit(
						hop.getTime().get(MINUTES) + hop.getBoiledTime().get(MINUTES));
					hopIbu = Equations.calcIbuMibuPostBoil(
						hop,
						boilTime,
						getDuration(),
						gravityIn,
						input.getVolume(),
						wortTemp,
						ambient,
						coolingK,
						equipUtil);
				}
				else if (formula == HopBitternessFormula.SMPH)
				{
					TimeUnit boilTime = new TimeUnit(
						hop.getTime().get(MINUTES) + hop.getBoiledTime().get(MINUTES));
					PhUnit kettlePh = PhVolumes.getPrimary(input);
					hopIbu = SmphEquations.calcPostBoilHopIbuSmph(
						hop,
						boilTime,
						getDuration(),
						gravityIn,
						input.getVolume(),
						wortTemp,
						ambient,
						coolingK,
						kettlePh,
						equipmentProfile.getElevation().get(Quantity.Unit.FOOT),
						equipUtil);
				}
				else
				{
					hopIbu = Equations.calcHopStandIbu(
						List.of(hop),
						gravityIn,
						input.getVolume(),
						new TimeUnit(60),
						getDuration(),
						wortTemp,
						ambient,
						coolingK);
					commonHopStandIbu.add(hopIbu);
				}
				bitternessByFormula.get(formula).add(hopIbu);
				perHopIbu.put(formula, hopIbu);
			}
			log.addVerboseMessage(StringUtils.getProcessString("log.hop.addition.ibu",
				describeHopAddition(hop, MINUTES),
				formatPerFormulaBitterness(reportedFormulas, perHopIbu)));
		}

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

		// A stand does not alter wort pH; carry it forward unchanged.
		PhVolumes.copyAll(input, volOut);

		//
		// Carry hop-acid state forward; stand hops add alpha and further isomerise per MIBU or IBU-derived
		// iso mass. Pre-isomerized extracts go directly to iso-alpha.
		//
		HopAcidVolumes.copyAll(input, volOut);
		for (HopAddition hop : getHopAdditions())
		{
			if (hop.getForm() != null
				&& hop.getForm().isPreIsomerized())
			{
				HopAcidVolumes.add(volOut, Volume.Metric.ISO_ALPHA_ACIDS_MG,
					Equations.calcHopAlphaAcidsMg(hop));
			}
			else
			{
				HopAcidVolumes.addHopAlpha(volOut, hop);
			}
		}
		if (reportedFormulas.contains(HopBitternessFormula.MIBU))
		{
			for (HopAddition hop : getHopAdditions())
			{
				if (hop.getForm() != null
					&& hop.getForm().isPreIsomerized())
				{
					continue;
				}
				HopAcidVolumes.isomerize(
					volOut,
					Brewday.getInstance().getHopAdditionIsoAlphaMgMibuPostBoil(
						equipmentProfile,
						input.getVolume(),
						gravityIn,
						input.getVolume(),
						gravityIn,
						hop,
						getDuration()));
			}
		}
		else if (!getHopAdditions().isEmpty())
		{
			HopAcidVolumes.isomerize(
				volOut,
				Equations.calcIsoAlphaAcidsMgFromIbu(commonHopStandIbu, input.getVolume()));
		}

		//
		// Optionally subtract trub and chiller loss before the wort leaves the kettle.
		//
		if (!KettleTrubChillerLossSubtract.subtractIfEnabled(
			volOut, equipmentProfile, removeTrubAndChillerLoss, log))
		{
			return;
		}

		VolumeUnit hopAbsorptionLoss = Equations.calcTotalHopAbsorptionLoss(getHopAdditions());
		if (hopAbsorptionLoss.get() > 0)
		{
			volOut.setVolume(new VolumeUnit(
				volOut.getVolume().get() - hopAbsorptionLoss.get()));
			log.addVerboseMessage(StringUtils.getProcessString("stand.hop.absorption.loss",
				hopAbsorptionLoss.get(Quantity.Unit.LITRES)));
		}

		BitternessVolumes.syncReportedDerived(volOut, reportedFormulas);

		//
		// Carry non-hop step additions (yeast for rehydration, misc, water, fermentables) on the output volume.
		//
		List<IngredientAddition> carried = new ArrayList<>();
		if (input.getIngredientAdditions() != null)
		{
			for (IngredientAddition ia : input.getIngredientAdditions())
			{
				if (ia.getType() != IngredientAddition.Type.HOPS)
				{
					carried.add(ia.clone());
				}
			}
		}
		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() != IngredientAddition.Type.HOPS)
			{
				carried.add(ia.clone());
			}
		}
		volOut.setIngredientAdditions(carried);

		//
		// Publish whirlpool / hop-stand wort for cool or ferment steps.
		//
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
		// Stand step supports being the first in a recipe

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
		return Arrays.asList(IngredientAddition.Type.values());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("duration", duration == null ? "null" : duration.get(Quantity.Unit.MINUTES) + "min");
		result.put("removeTrubAndChillerLoss", String.valueOf(removeTrubAndChillerLoss));
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
	public boolean isRemoveTrubAndChillerLoss()
	{
		return removeTrubAndChillerLoss;
	}

	public void setRemoveTrubAndChillerLoss(boolean removeTrubAndChillerLoss)
	{
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
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
						"stand.fermentable.addition",
						ia.describe()));
			}
			else if (ia.getType() == IngredientAddition.Type.HOPS)
			{
				result.add(
					StringUtils.getDocString(
						"stand.hop.addition",
						ia.describe(),
						ia.getTime().describe(MINUTES)));
			}
			else if (ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"stand.misc.addition",
						ia.describe(),
						ia.getTime().describe(MINUTES)));
			}
			else if (ia.getType() == IngredientAddition.Type.WATER)
			{
				result.add(
					StringUtils.getDocString(
						"stand.water.addition",
						ia.describe()));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		result.add(
			StringUtils.getDocString(
				"stand.duration",
				this.getInputVolume(),
				this.duration.describe(Quantity.Unit.MINUTES),
				volOut.getTemperature().describe(Quantity.Unit.CELSIUS)));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		return new Stand(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			this.getOutputVolume(),
			new TimeUnit(this.duration.get()),
			cloneIngredients(this.getIngredientAdditions()),
			this.removeTrubAndChillerLoss);
	}
}