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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;

/**
 *
 */
public class Stand extends FluidVolumeProcessStep
{
	/** stand duration */
	private TimeUnit duration;

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
		TimeUnit duration)
	{
		super(name, description, Type.STAND, inputVolume, outputVolume);
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.STAND), StringUtils.getProcessString("stand.desc"), Type.STAND, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(StringUtils.getProcessString("stand.output", getName()));

		duration = new TimeUnit(30, Quantity.Unit.MINUTES, false);
	}

	/*-------------------------------------------------------------------------*/
	public Stand(Stand step)
	{
		super(step.getName(), step.getDescription(), Type.STAND, step.getInputVolume(), step.getOutputVolume());

		this.duration = step.duration;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
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
			// fake it and let the water additions save us

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

		// collect up water additions
		boolean foundWaterAddition = false;
		for (WaterAddition ia : getWaterAdditions())
		{
			foundWaterAddition = true;
			input = Equations.dilute(input, ia, input.getName());
		}

		// if this is the first step in the recipe then we must have a water addition
		if (getInputVolume()== null && !foundWaterAddition)
		{
			log.addError(StringUtils.getProcessString("stand.no.water.additions"));
			return;
		}

		DensityUnit gravityIn = input.getGravity();
		ColourUnit colourIn = input.getColour();
		BitternessUnit bitternessIn = input.getBitterness();

		// gather up fermentable additions and add their contributions
		List<FermentableAddition> steepedGrains = new ArrayList<>();
		for (FermentableAddition fa : getFermentableAdditions())
		{
			// gravity impact
			DensityUnit gravity = Equations.calcSteepedFermentableAdditionGravity(fa, input.getVolume());
			gravityIn = new DensityUnit(gravityIn.get() + gravity.get());

			// colour impact
			if (fa.getFermentable().getType() == Fermentable.Type.GRAIN || fa.getFermentable().getType() == Fermentable.Type.ADJUNCT)
			{
				steepedGrains.add(fa);
			}
			else
			{
				ColourUnit col = Equations.calcSolubleFermentableAdditionColourContribution(fa, input.getVolume());
				colourIn = new ColourUnit(colourIn.get() + col.get());
			}

			// bitterness impact
			BitternessUnit ibu = Equations.calcSolubleFermentableAdditionBitternessContribution(fa, input.getVolume());
			bitternessIn = new BitternessUnit(bitternessIn.get() + ibu.get());
		}
		if (steepedGrains.size() > 0)
		{
			ColourUnit col = Equations.calcColourSrmMoreyFormula(steepedGrains, input.getVolume());
			colourIn = new ColourUnit(colourIn.get() + col.get());
		}

		// account for hop stand bitterness
		BitternessUnit hopStandIbu = Equations.calcHopStandIbu(
			getHopAdditions(),
			gravityIn,
			input.getVolume(),
			new TimeUnit(60), // todo we should be passing the boiled-time along
			getDuration());
		BitternessUnit bitternessOut = new BitternessUnit(bitternessIn.get() + hopStandIbu.get());

		// calculate the drop off in temperature
		TemperatureUnit tempOut = Equations.calcStandEndingTemperature(
			input.getTemperature(),
			getDuration());

		// calculate cooling shrinkage
		VolumeUnit volumeOut = Equations.calcCoolingShrinkage(
			input.getVolume(),
			new TemperatureUnit(input.getTemperature().get(Quantity.Unit.CELSIUS)
				- tempOut.get(Quantity.Unit.CELSIUS)));

		// ... and the impact on other metrics of the cooling shrinkage
		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), gravityIn, volumeOut);
		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);
		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), colourIn, volumeOut);

		volumes.addOrUpdateVolume(
			getOutputVolume(),
			new Volume(
				getOutputVolume(),
				Volume.Type.WORT,
				volumeOut,
				tempOut,
				input.getFermentability(),
				gravityOut,
				abvOut,
				colourOut,
				bitternessOut));
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
		return Arrays.asList(
			IngredientAddition.Type.FERMENTABLES,
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.WATER,
			IngredientAddition.Type.MISC);
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
	@Override
	public List<String> getInstructions()
	{
		Volume volOut = getRecipe().getVolumes().getVolume(this.getOutputVolume());

		List<String> result = new ArrayList<>();

		for (WaterAddition wa : getWaterAdditions())
		{
			result.add(StringUtils.getDocString("stand.water.addition", wa.describe()));
		}

		for (FermentableAddition ia : getFermentableAdditions())
		{
			result.add(
				StringUtils.getDocString(
					"mash.fermentable.addition",
					ia.describe()));
		}

		for (HopAddition ia : getHopAdditions())
		{
			result.add(
				StringUtils.getDocString(
					"mash.hop.addition",
					ia.describe(),
					ia.getTime().describe(MINUTES)));
		}

		for (MiscAddition ia : getMiscAdditions())
		{
			result.add(
				StringUtils.getDocString(
					"mash.misc.addition",
					ia.describe(),
					ia.getTime().describe(MINUTES)));
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
	public ProcessStep clone()
	{
		return new Stand(
			this.getName(),
			this.getDescription(),
			this.getInputVolume(),
			this.getOutputVolume(),
			new TimeUnit(this.duration.get()));
	}
}