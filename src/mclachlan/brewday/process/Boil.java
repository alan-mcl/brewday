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
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration */
	private TimeUnit duration;

	private String inputWortVolume;
	private String outputWortVolume;

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
		if (!volumes.contains(inputWortVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputWortVolume));
			return;
		}

		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		Volume inputWort = volumes.getVolume(inputWortVolume);

		if (inputWort.getVolume().get(Quantity.Unit.MILLILITRES)*1.2D >= equipmentProfile.getBoilKettleVolume())
		{
			log.addWarning(
				StringUtils.getProcessString("boil.kettle.too.small",
					equipmentProfile.getBoilKettleVolume()/1000,
					inputWort.getVolume().get(Quantity.Unit.LITRES)));
		}

		// gather up hop charges
		List<IngredientAddition> hopCharges = new ArrayList<IngredientAddition>();
		for (IngredientAddition item : getIngredients())
		{
			if (item instanceof HopAddition)
			{
				hopCharges.add(item);
			}
		}

		DensityUnit gravityIn = inputWort.getGravity();

		// gather up fermentable additions and add their gravity contributions
		for (IngredientAddition item : getIngredients())
		{
			if (item instanceof FermentableAddition)
			{
				FermentableAddition fa = (FermentableAddition)item;
				DensityUnit gravity = Equations.calcSolubleFermentableAdditionGravity(fa, inputWort.getVolume());
				gravityIn = new DensityUnit(gravityIn.get() + gravity.get());
			}
		}

		TemperatureUnit tempOut = new TemperatureUnit(100D, Quantity.Unit.CELSIUS, false);


		double boilEvapourationRatePerHour =
			equipmentProfile.getBoilEvapourationRate();

		double boiledOff = inputWort.getVolume().get(Quantity.Unit.MILLILITRES) *
			boilEvapourationRatePerHour * (duration.get(Quantity.Unit.MINUTES)/60D);

		VolumeUnit volumeOut = new VolumeUnit(
			inputWort.getVolume().get(Quantity.Unit.MILLILITRES) - boiledOff);

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputWort.getVolume(), gravityIn, volumeOut);

		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			inputWort.getVolume(), inputWort.getAbv(), volumeOut);

		// todo: account for kettle caramelisation darkening?
		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			inputWort.getVolume(), inputWort.getColour(), volumeOut);

		BitternessUnit bitternessOut = new BitternessUnit(inputWort.getBitterness());
		for (IngredientAddition hopCharge : hopCharges)
		{
			bitternessOut.add(
				Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					new DensityUnit((gravityOut.get() + gravityIn.get()) / 2),
					new VolumeUnit(volumeOut.get() + inputWort.getVolume().get()/2),
					equipmentProfile.getHopUtilisation()));
		}

		volumes.addOrUpdateVolume(
			outputWortVolume,
			new Volume(
				outputWortVolume,
				Volume.Type.WORT,
				volumeOut,
				tempOut,
				inputWort.getFermentability(),
				gravityOut,
				abvOut,
				colourOut,
				bitternessOut));
	}

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
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
		return new ArrayList<>(Collections.singletonList(inputWortVolume));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getOutputVolumes()
	{
		return Collections.singletonList(outputWortVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(
			IngredientAddition.Type.FERMENTABLES,
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.MISC);
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
			if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"boil.fermentable.addition",
						ia.getQuantity().get(Quantity.Unit.GRAMS),
						ia.getName()));
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
}
