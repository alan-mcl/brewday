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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

/**
 * Combines two volumes into one.
 */
public class Combine extends FluidVolumeProcessStep
{
	private String inputVolume2;

	/*-------------------------------------------------------------------------*/
	public Combine()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Combine(
		String name,
		String description,
		String inputVolume,
		String inputVolume2,
		String outputVolume)
	{
		super(name, description, Type.COMBINE, inputVolume, outputVolume);
		setInputVolume2(inputVolume2);
	}

	/*-------------------------------------------------------------------------*/
	public Combine(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.COMBINE),
			StringUtils.getProcessString("combine.desc"), Type.COMBINE, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setInputVolume2(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));

		setOutputVolume(StringUtils.getProcessString("combine.output", getName()));
	}

	/*-------------------------------------------------------------------------*/
	public Combine(Combine other)
	{
		super(other.getName(), other.getDescription(), Type.COMBINE, other.getInputVolume(), other.getOutputVolume());
		this.inputVolume2 = other.inputVolume2;
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

		Volume input = getInputVolume(volumes);
		Volume input2 = volumes.getVolume(inputVolume2);

		if (input.getType() != input2.getType())
		{
			log.addError(StringUtils.getProcessString("combine.different.volume.types"));
			return;
		}

		Volume.Type typeOut = input.getType();

		ColourUnit colourOut = Equations.calcCombinedColour(
			input.getVolume(), input.getColour(),
			input2.getVolume(), input2.getColour());

		DensityUnit densityOut = Equations.calcCombinedGravity(
			input.getVolume(), input.getGravity(),
			input2.getVolume(), input2.getGravity());

		BitternessUnit bitternessOut = Equations.calcCombinedBitterness(
			input.getVolume(), input.getBitterness(),
			input2.getVolume(), input2.getBitterness());

		TemperatureUnit tempOut = Equations.calcCombinedTemperature(
			input.getVolume(), input.getTemperature(),
			input2.getVolume(), input2.getTemperature());

		PercentageUnit abvOut = (PercentageUnit)Equations.calcCombinedLinearInterpolation(
			input.getVolume(), input.getAbv(),
			input2.getVolume(), input2.getAbv());

		PercentageUnit fermOut = (PercentageUnit)Equations.calcCombinedLinearInterpolation(
			input.getVolume(), input.getFermentability(),
			input2.getVolume(), input2.getFermentability());

		CarbonationUnit carbOut = (CarbonationUnit)Equations.calcCombinedLinearInterpolation(
			input.getVolume(), input.getCarbonation(),
			input2.getVolume(), input2.getCarbonation());

		VolumeUnit volOut = new VolumeUnit(input.getVolume().get() + input2.getVolume().get());

		Set<IngredientAddition> additions = new HashSet<>();
		if (input.getIngredientAdditions() != null)
		{
			additions.addAll(input.getIngredientAdditions());
		}
		if (input2.getIngredientAdditions() != null)
		{
			additions.addAll(input2.getIngredientAdditions());
		}

		Volume result = new Volume(
			getOutputVolume(),
			typeOut,
			volOut,
			tempOut,
			fermOut,
			densityOut,
			abvOut,
			colourOut,
			bitternessOut);

		result.setCarbonation(carbOut);
		result.setIngredientAdditions(new ArrayList<>(additions));

		volumes.addOrUpdateVolume(getOutputVolume(), result);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (!super.validateInputVolumes(volumes, log) || !volumes.contains(inputVolume2))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputVolume2));
			return false;
		}
		return true;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString(
			"combine.step.desc",
			getInputVolume(),
			getInputVolume2());
	}

	/*-------------------------------------------------------------------------*/

	public String getInputVolume2()
	{
		return inputVolume2;
	}

	public void setInputVolume2(String inputVolume2)
	{
		this.inputVolume2 = inputVolume2;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		Volume volume = getRecipe().getVolumes().getVolume(getOutputVolume());

		return List.of(
			StringUtils.getDocString(
				"combine.doc",
				this.getInputVolume(),
				this.getInputVolume2(),
				volume.getVolume().describe(Quantity.Unit.LITRES)));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone()
	{
		return new Combine(this);
	}
}
