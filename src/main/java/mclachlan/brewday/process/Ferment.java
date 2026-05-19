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
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.recipe.YeastCulture;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class Ferment extends FluidVolumeProcessStep
{
	/** fermentation time */
	private TimeUnit duration;

	/** fermentation temperature at phase start, in C */
	private TemperatureUnit startTemp;

	/** fermentation temperature at phase end, in C */
	private TemperatureUnit endTemp;

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
		TemperatureUnit startTemp,
		TemperatureUnit endTemp,
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions,
		boolean removeTrubAndChillerLoss)
	{
		super(name, description, Type.FERMENT, inputVolume, outputVolume);
		this.startTemp = startTemp;
		this.endTemp = endTemp;
		this.duration = duration;
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
		super.setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.FERMENT), StringUtils.getProcessString("ferment.desc"), Type.FERMENT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setOutputVolume(StringUtils.getProcessString("ferment.output", getName()));
		setStartTemp(new TemperatureUnit(20D));
		setEndTemp(new TemperatureUnit(20D));
		setDuration(new TimeUnit(14, DAYS, false));
		this.removeTrubAndChillerLoss = false;
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Ferment other)
	{
		super(other.getName(), other.getDescription(), Type.FERMENT, other.getInputVolume(), other.getOutputVolume());

		this.startTemp = other.startTemp == null ? null : new TemperatureUnit(other.startTemp);
		this.endTemp = other.endTemp == null ? null : new TemperatureUnit(other.endTemp);
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

		List<YeastAddition> stepPitches = getYeastAdditions();
		boolean hasYeast = !inputVolume.getYeastCultures().isEmpty() || !stepPitches.isEmpty();

		if (!hasYeast && inputVolume.getType() == Volume.Type.WORT)
		{
			log.addError(StringUtils.getProcessString("ferment.no.yeast.addition"));
			estimatedFinalGravity = inputVolume.getGravity();
			return;
		}

		FermentationResult fermentation = FermentationCalculator.fermentPhase(
			inputVolume,
			stepPitches,
			startTemp,
			endTemp,
			duration,
			log);

		Volume volOut;
		if (fermentation.hasFermentation())
		{
			TemperatureUnit avgTemp = fermentation.getAverageTemp();
			if (avgTemp == null)
			{
				avgTemp = FermentationCalculator.calcAverageTempC(startTemp, endTemp);
			}

			ColourUnit colourOut = inputVolume.getColour() == null
				? null
				: Equations.calcColourAfterFermentation(inputVolume.getColour());

			CarbonationUnit carbonationOut = Equations.calcEquilibriumCo2(
				avgTemp,
				Const.ONE_ATMOSPHERE_IN_KPA);

			DensityUnit originalGravity = inputVolume.getOriginalGravity() != null
				? inputVolume.getOriginalGravity()
				: inputVolume.getGravity();

			if (inputVolume.getType() == Volume.Type.WORT)
			{
				volOut = new Volume(getOutputVolume(), Volume.Type.BEER);
				volOut.setVolume(inputVolume.getVolume());
				volOut.setTemperature(inputVolume.getTemperature());
				volOut.setOriginalGravity(originalGravity);
				volOut.setColour(colourOut);
				volOut.setBitterness(inputVolume.getBitterness());
				volOut.setCarbonation(carbonationOut);
			}
			else
			{
				volOut = inputVolume.clone();
				volOut.setName(getOutputVolume());
				if (originalGravity != null)
				{
					volOut.setOriginalGravity(originalGravity);
				}
				volOut.setColour(colourOut);
				volOut.setCarbonation(carbonationOut);
			}

			volOut.setIngredientAdditions(
				buildOutputIngredients(inputVolume, fermentation.getEvolvedCultures()));
		}
		else
		{
			volOut = inputVolume.clone();
			volOut.setName(getOutputVolume());
		}

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);

		Volume beerVolume = volumes.getVolume(getOutputVolume());
		DensityUnit measuredFg = (DensityUnit)beerVolume.getMetric(Volume.Metric.GRAVITY);
		boolean estimatedFg = measuredFg == null || measuredFg.isEstimated();
		DensityUnit fg;
		if (estimatedFg && fermentation.hasFermentation() && fermentation.getEstimatedFg() != null)
		{
			estimatedFinalGravity = fermentation.getEstimatedFg();
			fg = estimatedFinalGravity;
		}
		else if (estimatedFg)
		{
			estimatedFinalGravity = inputVolume.getGravity();
			fg = estimatedFinalGravity;
		}
		else
		{
			fg = measuredFg;
		}

		PercentageUnit abvAdded;
		if (fermentation.hasFermentation())
		{
			abvAdded = Equations.calcAbvWithGravityChange(inputVolume.getGravity(), fg);
		}
		else
		{
			abvAdded = new PercentageUnit(0D, false);
		}
		beerVolume.setGravity(fg);

		double abvIn = inputVolume.getAbv() == null ? 0 : inputVolume.getAbv().get();
		beerVolume.setAbv(new PercentageUnit(abvIn + abvAdded.get(), abvAdded.isEstimated()));
	}

	/*-------------------------------------------------------------------------*/
	private List<IngredientAddition> buildOutputIngredients(
		Volume inputVolume,
		List<YeastCulture> evolvedCultures)
	{
		List<IngredientAddition> out = new ArrayList<>();

		for (IngredientAddition ia : inputVolume.getIngredientAdditions())
		{
			if (!(ia instanceof YeastCulture) && !(ia instanceof YeastAddition))
			{
				out.add(ia.clone());
			}
		}

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia instanceof YeastAddition || ia instanceof YeastCulture)
			{
				continue;
			}
			if (ia.getType() == IngredientAddition.Type.WATER)
			{
				continue;
			}
			out.add(ia.clone());
		}

		out.addAll(evolvedCultures);
		return out;
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
		if (isConstantTemperature())
		{
			return StringUtils.getProcessString(
				"ferment.step.desc.constant",
				endTemp.describe(CELSIUS));
		}
		return StringUtils.getProcessString(
			"ferment.step.desc.ramp",
			startTemp.describe(CELSIUS),
			endTemp.describe(CELSIUS));
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(IngredientAddition.Type.values());
	}

	/*-------------------------------------------------------------------------*/
	public TemperatureUnit getStartTemp()
	{
		return startTemp;
	}

	public void setStartTemp(TemperatureUnit startTemp)
	{
		this.startTemp = startTemp;
	}

	public TemperatureUnit getEndTemp()
	{
		return endTemp;
	}

	public void setEndTemp(TemperatureUnit endTemp)
	{
		this.endTemp = endTemp;
	}

	/**
	 * Representative fermentation temperature for legacy calculations (e.g. equilibrium CO₂).
	 * Returns {@link #getEndTemp()} until fermentation ramp modelling is implemented.
	 */
	@Deprecated
	public TemperatureUnit getTemperature()
	{
		return endTemp;
	}

	/**
	 * Sets both start and end temperature to the same value.
	 */
	@Deprecated
	public void setTemperature(TemperatureUnit temp)
	{
		this.startTemp = temp;
		this.endTemp = temp;
	}

	public boolean isConstantTemperature()
	{
		if (startTemp == null || endTemp == null)
		{
			return true;
		}
		return Math.abs(startTemp.get(CELSIUS) - endTemp.get(CELSIUS)) < 0.05;
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
						ia.describe()));
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
			new TemperatureUnit(getStartTemp()),
			new TemperatureUnit(getEndTemp()),
			new TimeUnit(getDuration().get()),
			cloneIngredients(getIngredientAdditions()),
			this.removeTrubAndChillerLoss);
	}
}
