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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration */
	private TimeUnit duration;

	private String inputWortVolume;
	private String outputWortVolume;

	// calculated
	private TimeUnit timeToBoil;

	/*-------------------------------------------------------------------------*/
	public Boil()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Boil(
		String name,
		String description,
		String inputWortVolume,
		String outputWortVolume,
		List<IngredientAddition> ingredientAdditions,
		TimeUnit duration)
	{
		super(name, description, Type.BOIL);
		this.inputWortVolume = inputWortVolume;
		this.outputWortVolume = outputWortVolume;
		setIngredients(ingredientAdditions);
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BOIL), StringUtils.getProcessString("boil.desc"), Type.BOIL);

		// todo: find last wort vol?
		this.inputWortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT);
		this.outputWortVolume = StringUtils.getProcessString("boil.output", getName());
		this.duration = new TimeUnit(60, Quantity.Unit.MINUTES, false);
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Boil step)
	{
		super(step.getName(), step.getDescription(), Type.BOIL);

		this.inputWortVolume = step.inputWortVolume;
		this.outputWortVolume = step.outputWortVolume;
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

		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		// Boil step supports being the first one, for e.g. during an extract batch
		Volume inputVolume = null;

		if (inputWortVolume != null)
		{
			inputVolume = volumes.getVolume(inputWortVolume);
		}
		else
		{
			// fake it and let the water additions save us

			inputVolume = new Volume("water volume",
				Volume.Type.WORT,
				new VolumeUnit(0),
				new TemperatureUnit(20, Quantity.Unit.CELSIUS),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new PercentageUnit(0),
				new ColourUnit(0, Quantity.Unit.SRM),
				new BitternessUnit(0, Quantity.Unit.IBU));
		}

		boolean foundWaterAddition = false;
		// collect up water additions
		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.WATER))
		{
			foundWaterAddition = true;
			inputVolume = Equations.dilute(inputVolume, (WaterAddition)ia, inputVolume.getName());
		}

		// if this is the first step in the recipe then we must have a water addition
		if (inputWortVolume==null && !foundWaterAddition)
		{
			log.addError(StringUtils.getProcessString("boil.no.water.additions"));
			return;
		}

		// check for boilover risk
		if (inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) * 1.2D >=
			equipmentProfile.getBoilKettleVolume().get(Quantity.Unit.MILLILITRES))
		{
			log.addWarning(
				StringUtils.getProcessString("boil.kettle.too.small",
					equipmentProfile.getBoilKettleVolume().get(Quantity.Unit.LITRES),
					inputVolume.getVolume().get(Quantity.Unit.LITRES)));
		}

		// gather up hop charges
		List<IngredientAddition> hopCharges = new ArrayList<>();
		for (IngredientAddition ia : getIngredients())
		{
			if (ia instanceof HopAddition)
			{
				hopCharges.add(ia);
			}
		}
		for (IngredientAddition ia : inputVolume.getIngredientAdditions())
		{
			if (ia instanceof HopAddition)
			{
				// These are probably FWH, treat them as if they are present at
				// the start of the boil too.
				hopCharges.add(
					new HopAddition(
						((HopAddition)ia).getHop(),
						((HopAddition)ia).getForm(),
						(WeightUnit)ia.getQuantity(),
						this.getDuration()));
			}
		}

		DensityUnit gravityIn = inputVolume.getGravity();
		ColourUnit colourIn = inputVolume.getColour();
		BitternessUnit bitternessIn = inputVolume.getBitterness();
		if (bitternessIn == null)
		{
			bitternessIn = new BitternessUnit(0, Quantity.Unit.IBU);
		}

		// gather up fermentable additions and add their contributions
		for (IngredientAddition item : getIngredients())
		{
			if (item instanceof FermentableAddition)
			{
				FermentableAddition fa = (FermentableAddition)item;

				// ignore GRAIN and ADJUNCT additions
				if (fa.getFermentable().getType() == Fermentable.Type.JUICE ||
					fa.getFermentable().getType() == Fermentable.Type.SUGAR ||
					fa.getFermentable().getType() == Fermentable.Type.LIQUID_EXTRACT ||
					fa.getFermentable().getType() == Fermentable.Type.DRY_EXTRACT)
				{
					// gravity impact
					DensityUnit gravity = Equations.calcSteepedFermentableAdditionGravity(fa, inputVolume.getVolume());
					gravityIn = new DensityUnit(gravityIn.get() + gravity.get());

					// colour impact
					ColourUnit col = Equations.calcSolubleFermentableAdditionColourContribution(fa, inputVolume.getVolume());
					colourIn = new ColourUnit(colourIn.get() + col.get());

					// bitterness impact
					BitternessUnit ibu = Equations.calcSolubleFermentableAdditionBitternessContribution(fa, inputVolume.getVolume());
					bitternessIn = new BitternessUnit(bitternessIn.get() + ibu.get());
				}
			}
		}

		TemperatureUnit tempOut = new TemperatureUnit(100D, Quantity.Unit.CELSIUS, false);

		double boilEvapourationRatePerHour =
			equipmentProfile.getBoilEvapourationRate().get();

		double boiledOff = inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) *
			boilEvapourationRatePerHour * (duration.get(Quantity.Unit.MINUTES)/60D);

		VolumeUnit volumeOut = new VolumeUnit(
			inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) - boiledOff);

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputVolume.getVolume(), gravityIn, volumeOut);

		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			inputVolume.getVolume(), inputVolume.getAbv(), volumeOut);

		// colour changes
		ColourUnit colourOut = Equations.calcColourAfterBoil(colourIn);
		colourOut = Equations.calcColourWithVolumeChange(
			inputVolume.getVolume(), colourOut, volumeOut);

		BitternessUnit bitternessOut = new BitternessUnit(bitternessIn);
		for (IngredientAddition hopCharge : hopCharges)
		{
			// Tinseth's equation is based on the post-boil volume.
			// Actually it's the "final volume" which BeerSmith seems to interpret
			// as the batch size i.e. after trub & chiller loss.
			bitternessOut.add(
				Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					new DensityUnit((gravityOut.get() + gravityIn.get()) / 2),
					new VolumeUnit(volumeOut.get()),
					equipmentProfile.getHopUtilisation().get()));
		}

		Volume postBoilOut = new Volume(
			outputWortVolume,
			inputVolume.getType(),
			volumeOut,
			tempOut,
			inputVolume.getFermentability(),
			gravityOut,
			abvOut,
			colourOut,
			bitternessOut);

		postBoilOut.setIngredientAdditions(inputVolume.getIngredientAdditions());

		volumes.addOrUpdateVolume(outputWortVolume, postBoilOut);

		// calculated fields
		timeToBoil = Equations.calcHeatingTime(
			inputVolume.getVolume(),
			inputVolume.getTemperature(),
			new TemperatureUnit(100, Quantity.Unit.CELSIUS),
			equipmentProfile.getBoilElementPower());
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (inputWortVolume!=null && !volumes.contains(inputWortVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputWortVolume));
			return false;
		}
		return true;
	}

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addVolume(outputWortVolume, new Volume(Volume.Type.WORT));
	}

	@Override
	protected void sortIngredients()
	{
		// sort ascending by time
		getIngredients().sort((o1, o2) -> (int)(o2.getTime().get() - o1.getTime().get()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("boil.step.desc", duration.get(Quantity.Unit.MINUTES));
	}

	/*-------------------------------------------------------------------------*/
	public String getInputWortVolume()
	{
		return inputWortVolume;
	}

	public String getOutputWortVolume()
	{
		return outputWortVolume;
	}

	public TimeUnit getDuration()
	{
		return duration;
	}

	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public void setInputWortVolume(String inputWortVolume)
	{
		this.inputWortVolume = inputWortVolume;
	}

	/*-------------------------------------------------------------------------*/
	public void setOutputWortVolume(String outputWortVolume)
	{
		this.outputWortVolume = outputWortVolume;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getInputVolumes()
	{
		return inputWortVolume==null?Collections.emptyList():Collections.singletonList(inputWortVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getOutputVolumes()
	{
		return outputWortVolume==null?Collections.emptyList():Collections.singletonList(outputWortVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(
			IngredientAddition.Type.FERMENTABLES,
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.MISC,
			IngredientAddition.Type.WATER);
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		Volume preBoilVol = getRecipe().getVolumes().getVolume(this.getInputWortVolume());
		result.add(StringUtils.getDocString(
			"boil.pre.boil",
			preBoilVol.getMetric(Volume.Metric.VOLUME).get(Quantity.Unit.LITRES),
			preBoilVol.getMetric(Volume.Metric.GRAVITY).get(Quantity.Unit.SPECIFIC_GRAVITY)));

		result.add(StringUtils.getDocString("boil.duration", this.duration.get(Quantity.Unit.MINUTES)));

		for (IngredientAddition ia : getIngredients())
		{
			if (ia.getType() == IngredientAddition.Type.HOPS || ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"boil.hop.addition",
						ia.getQuantity().get(Quantity.Unit.GRAMS),
						ia.getName(),
						ia.getTime().get(Quantity.Unit.MINUTES)));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"boil.fermentable.addition",
						ia.getQuantity().get(Quantity.Unit.KILOGRAMS),
						ia.getName(),
						ia.getTime().get(Quantity.Unit.MINUTES)));
			}
			else
			{
				throw new BrewdayException("invalid "+ia.getType());
			}
		}

		Volume postBoilVol = getRecipe().getVolumes().getVolume(this.getOutputWortVolume());
		result.add(StringUtils.getDocString(
			"boil.post.boil",
			postBoilVol.getMetric(Volume.Metric.VOLUME).get(Quantity.Unit.LITRES),
			postBoilVol.getMetric(Volume.Metric.GRAVITY).get(Quantity.Unit.SPECIFIC_GRAVITY)));

		return result;
	}

	@Override
	public ProcessStep clone()
	{
		return new Boil(
			this.getName(),
			this.getDescription(),
			this.getInputWortVolume(),
			this.getOutputWortVolume(),
			cloneIngredients(this.getIngredients()),
			new TimeUnit(this.getDuration().get()));
	}

	/*-------------------------------------------------------------------------*/

	public TimeUnit getTimeToBoil()
	{
		return timeToBoil;
	}
}
