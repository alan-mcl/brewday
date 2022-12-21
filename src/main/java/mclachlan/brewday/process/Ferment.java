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
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class Ferment extends FluidVolumeProcessStep
{
	/** fermentation time */
	private TimeUnit duration;

	/** fermentation temperature in C */
	private TemperatureUnit temp;

	/** calculated */
	private DensityUnit estimatedFinalGravity = new DensityUnit();

	/** should this step remove the equipment profile trub & chiller loss? */
	private boolean removeTrubAndChillerLoss;
	//
	// I'm not sure if this is the best place to remove the "trub+chiller loss"
	// volume. This assumes that previous steps (eg cool, dilute) took place
	// in the boil kettle, and doing it here models the transfer of wort from
	// the kettle into the fermenter. But that won't always be the case.
	//
	// Todo: have a "remove trub+chiller loss" flag on various process steps
	// and support removing it at all those points.
	//


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
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions,
		boolean removeTrubAndChillerLoss)
	{
		super(name, description, Type.FERMENT, inputVolume, outputVolume);
		this.temp = temp;
		this.duration = duration;
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
		super.setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.FERMENT), StringUtils.getProcessString("ferment.desc"), Type.FERMENT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(StringUtils.getProcessString("ferment.output", getName()));
		setTemperature(new TemperatureUnit(20D));
		setDuration(new TimeUnit(14, DAYS, false));
		this.removeTrubAndChillerLoss = false;
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Ferment other)
	{
		super(other.getName(), other.getDescription(), Type.FERMENT, other.getInputVolume(), other.getOutputVolume());

		this.temp = other.temp;
		this.duration = other.duration;
		this.removeTrubAndChillerLoss = other.removeTrubAndChillerLoss;
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

		Volume inputVolume = getInputVolume(volumes);

		// duplicate to avoid mucking with the original
		inputVolume = inputVolume.clone();

		if (removeTrubAndChillerLoss)
		{
			inputVolume.setVolume(new VolumeUnit(
				inputVolume.getVolume().get()
					- equipmentProfile.getTrubAndChillerLoss().get()));
		}

		// collect up any water additions and dilute the wort before boiling
		for (WaterAddition ia : getWaterAdditions())
		{
			inputVolume = Equations.dilute(inputVolume, ia, inputVolume.getName());
		}

		// todo: fermentable additions

		if (inputVolume.getVolume().get(Quantity.Unit.MILLILITRES)*1.2 >
			equipmentProfile.getFermenterVolume().get(MILLILITRES))
		{
			log.addWarning(
				StringUtils.getProcessString("ferment.fermenter.not.large.enough",
					equipmentProfile.getFermenterVolume().get(LITRES),
					inputVolume.getVolume().get(Quantity.Unit.LITRES)));
		}

		// todo: support for multiple yeast additions
		YeastAddition yeastAddition = null;
		for (IngredientAddition item : getIngredientAdditions())
		{
			if (item instanceof YeastAddition)
			{
				// todo: blends
				yeastAddition = (YeastAddition)item;
				break;
			}
		}

		// if we are starting with wort then this step needs to have a yeast addition
		if (yeastAddition == null && inputVolume.getType() == Volume.Type.WORT)
		{
			log.addError(StringUtils.getProcessString("ferment.no.yeast.addition"));
			estimatedFinalGravity = inputVolume.getGravity();
			return;
		}

		Volume volOut;
		if (yeastAddition != null)
		{
			ColourUnit colourOut = Equations.calcColourAfterFermentation(inputVolume.getColour());

			// assume that the beer is carbonated to the equilibrium point of the
			// fermentation temperature, at one atmosphere
			CarbonationUnit carbonationOut = Equations.calcEquilibriumCo2(
				this.getTemperature(),
				Const.ONE_ATMOSPHERE_IN_KPA);

			//
			// first set the output beer volume with what we establish from the input volume
			//
			volOut = new Volume(getOutputVolume(), Volume.Type.BEER);
			volOut.setVolume(inputVolume.getVolume());
			volOut.setTemperature(inputVolume.getTemperature());
			volOut.setOriginalGravity(inputVolume.getGravity());
			volOut.setColour(colourOut);
			volOut.setBitterness(inputVolume.getBitterness());
			volOut.setCarbonation(carbonationOut);
		}
		else
		{
			volOut = inputVolume.clone();
		}

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);

		//
		// If the gravity is still "estimated", estimate the ABV otherwise calculate it
		//
		Volume beerVolume = volumes.getVolume(getOutputVolume());
		DensityUnit measuredFg = (DensityUnit)beerVolume.getMetric(Volume.Metric.GRAVITY);
		boolean estimatedFg = measuredFg == null || measuredFg.isEstimated();
		DensityUnit fg;
		if (estimatedFg)
		{
			double estAtten = Equations.calcEstimatedAttenuation(inputVolume, yeastAddition);
			estimatedFinalGravity = new DensityUnit(inputVolume.getGravity().get() * (1 - estAtten));
			fg = estimatedFinalGravity;
		}
		else
		{
			fg = measuredFg;
		}

		PercentageUnit abvAdded;
		if (yeastAddition != null)
		{
			abvAdded = Equations.calcAbvWithGravityChange(inputVolume.getGravity(), fg);
		}
		else
		{
			abvAdded = new PercentageUnit(0D, false);
		}
		beerVolume.setGravity(fg);

		// add any abv in the input wort, in the case of re-fermentations
		double abvIn = inputVolume.getAbv()==null?0:inputVolume.getAbv().get();
		beerVolume.setAbv(new PercentageUnit(abvIn + abvAdded.get(), abvAdded.isEstimated()));
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

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
		return Arrays.asList(IngredientAddition.Type.values());
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

	public TimeUnit getDuration()
	{
		return duration;
	}

	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	public DensityUnit getEstimatedFinalGravity()
	{
		return estimatedFinalGravity;
	}

	public boolean isRemoveTrubAndChillerLoss()
	{
		return removeTrubAndChillerLoss;
	}

	public void setRemoveTrubAndChillerLoss(boolean removeTrubAndChillerLoss)
	{
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void sortIngredients()
	{
		// sort ascending by time
		getIngredientAdditions().sort((o1, o2) -> (int)(o2.getTime().get() - o1.getTime().get()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		result.add(StringUtils.getDocString(
			"ferment.duration",
			this.getInputVolume(),
			this.getDuration().describe(DAYS)));

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.HOPS || ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.hop.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else if (ia.getType() == IngredientAddition.Type.YEAST)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.yeast.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.fermentable.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else if (ia.getType() == IngredientAddition.Type.WATER)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.water.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else if (ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.misc.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		return result;
	}

	@Override
	public ProcessStep clone(String newName)
	{
		return new Ferment(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			StringUtils.getProcessString("ferment.output", newName),
			new TemperatureUnit(getTemperature().get()),
			new TimeUnit(getDuration().get()),
			cloneIngredients(getIngredientAdditions()),
			this.removeTrubAndChillerLoss);
	}
}
