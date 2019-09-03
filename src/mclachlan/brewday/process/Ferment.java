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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.YeastAddition;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;

/**
 *
 */
public class Ferment extends FluidVolumeProcessStep
{
	// todo: time

	/** fermentation temperature in C */
	private TemperatureUnit temp;

	/** calculated */
	private DensityUnit estimatedFinalGravity = new DensityUnit();

	/*-------------------------------------------------------------------------*/
	public Ferment()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TemperatureUnit temp,
		List<IngredientAddition> ingredientAdditions)
	{
		super(name, description, Type.FERMENT, inputVolume, outputVolume);
		super.setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
		this.temp = temp;
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.FERMENT), StringUtils.getProcessString("ferment.desc"), Type.FERMENT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(StringUtils.getProcessString("ferment.output", getName()));
		setTemperature(new TemperatureUnit(20D));
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Ferment step)
	{
		super(step.getName(), step.getDescription(), Type.FERMENT, step.getInputVolume(), step.getOutputVolume());

		this.temp = step.temp;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolume(volumes, log))
		{
			return;
		}

		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		Volume inputWort = getInputVolume(volumes);

		//
		// I'm not sure if this is the best place to remove the "trub+chiller loss"
		// volume. This assumes that previous steps (eg cool, dilute) took place
		// in the boil kettle, and doing it here models the transfer of wort from
		// the kettle into the fermenter. But that won't always be the case.
		//
		// Todo: have a "remove trub+chiller loss" flag on various process steps
		// and support removing it at all those points.
		//
		inputWort.setVolume(new VolumeUnit(
			inputWort.getVolume().get()
				- equipmentProfile.getTrubAndChillerLoss()));

		if (inputWort.getVolume().get(Quantity.Unit.MILLILITRES)*1.2 > equipmentProfile.getFermenterVolume())
		{
			log.addWarning(
				StringUtils.getProcessString("ferment.fermenter.not.large.enough",
					equipmentProfile.getFermenterVolume()/1000, inputWort.getVolume().get(Quantity.Unit.LITRES)));
		}

		// todo: support for multiple yeast additions
		YeastAddition yeastAddition = null;
		for (IngredientAddition item : getIngredients())
		{
			if (item instanceof YeastAddition)
			{
				// todo: blends
				yeastAddition = (YeastAddition)item;
				break;
			}
		}

		if (yeastAddition == null)
		{
			log.addError(StringUtils.getProcessString("ferment.no.yeast.addition"));
			estimatedFinalGravity = inputWort.getGravity();
			return;
		}

		ColourUnit colourOut = Equations.calcColourAfterFermentation(inputWort.getColour());

		// assume that the beer is carbonated to the equilibrium point of the
		// fermentation temperature, at one atmosphere
		CarbonationUnit carbonationOut = Equations.calcEquilibriumCo2(
			this.getTemperature(),
			Const.ONE_ATMOSPHERE_IN_KPA);

		//
		// first set the output beer volume with what we establish from the input volume
		//
		Volume volOut = new Volume(getOutputVolume(), Volume.Type.BEER);
		volOut.setVolume(inputWort.getVolume());
		volOut.setTemperature(inputWort.getTemperature());
		volOut.setOriginalGravity(inputWort.getGravity());
		volOut.setColour(colourOut);
		volOut.setBitterness(inputWort.getBitterness());
		volOut.setCarbonation(carbonationOut);

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);

		//
		// Then, if the gravity is still "estimated", estimete the ABV otherwise
		// calculate it
		//
		Volume beerVolume = volumes.getVolume(getOutputVolume());
		DensityUnit measuredFg = (DensityUnit)beerVolume.getMetric(Volume.Metric.GRAVITY);
		boolean estimatedFg = measuredFg == null || measuredFg.isEstimated();
		DensityUnit fg;
		if (estimatedFg)
		{
			double estAtten = Equations.calcEstimatedAttenuation(inputWort, yeastAddition, temp);
			estimatedFinalGravity = new DensityUnit(inputWort.getGravity().get() * (1 - estAtten));
			fg = estimatedFinalGravity;
		}
		else
		{
			fg = measuredFg;
		}

		PercentageUnit abvOut = Equations.calcAvbWithGravityChange(inputWort.getGravity(), fg);
		beerVolume.setGravity(fg);
		// add any abv in the input wort, in the case of re-fermentations
		beerVolume.setAbv(new PercentageUnit(inputWort.getAbv().get() + abvOut.get(), abvOut.isEstimated()));
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		recipe.getVolumes().addVolume(getOutputVolume(), new Volume(Volume.Type.BEER));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString(
			"ferment.step.desc",
			temp.get(Quantity.Unit.CELSIUS));
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(
			IngredientAddition.Type.YEAST,
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.MISC,
			IngredientAddition.Type.FERMENTABLES);
	}

	/*-------------------------------------------------------------------------*/
	public TemperatureUnit getTemperature()
	{
		return temp;
	}

	public void setTemperature(TemperatureUnit temp)
	{
		this.temp = temp;
	}

	public DensityUnit getEstimatedFinalGravity()
	{
		return estimatedFinalGravity;
	}

	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		// todo: fermentation time
//		result.add(StringUtils.getDocString(
//			"ferment.duration",
//			this.getInputVolume(),
//			this.getDuration()));

		for (IngredientAddition ia : getIngredients())
		{
			if (ia.getType() == IngredientAddition.Type.HOPS || ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.hop.addition",
						ia.getQuantity().get(GRAMS),
						ia.getName(),
						ia.getTime()));
			}
			else if (ia.getType() == IngredientAddition.Type.YEAST)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.yeast.addition",
						ia.getQuantity().get(GRAMS),
						ia.getName(),
						ia.getTime()));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.fermentable.addition",
						ia.getQuantity().get(KILOGRAMS),
						ia.getName(),
						ia.getTime()));
			}
			else
			{
				throw new BrewdayException("invalid "+ia.getType());
			}
		}

		return result;
	}
}
