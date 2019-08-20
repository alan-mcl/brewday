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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration in minutes */
	private double duration;

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
		double duration)
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
		this.duration = 60;
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

		// todo: fermentable additions in the boil
		List<IngredientAddition> hopCharges = new ArrayList<IngredientAddition>();
		for (IngredientAddition item : getIngredients())
		{
			if (item instanceof HopAddition)
			{
				hopCharges.add(item);
			}
		}

		TemperatureUnit tempOut = new TemperatureUnit(100D, Quantity.Unit.CELSIUS, false);

		double boilEvapourationRatePerHour =
			equipmentProfile.getBoilEvapourationRate();

		double boiledOff = inputWort.getVolume().get(Quantity.Unit.MILLILITRES) *
			boilEvapourationRatePerHour * (duration/60D);

		VolumeUnit volumeOut = new VolumeUnit(
			inputWort.getVolume().get(Quantity.Unit.MILLILITRES) - boiledOff);

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputWort.getVolume(), inputWort.getGravity(), volumeOut);

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
					new DensityUnit((gravityOut.get() + inputWort.getGravity().get()) / 2),
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

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("boil.step.desc", duration);
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

	public double getDuration()
	{
		return duration;
	}

	public void setDuration(double duration)
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
		// todo: fermentable additions
		return Arrays.asList(
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.MISC);
	}
}
