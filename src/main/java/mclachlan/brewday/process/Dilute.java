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
import mclachlan.brewday.recipe.WaterAddition;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class Dilute extends FluidVolumeProcessStep
{
	/*-------------------------------------------------------------------------*/
	public Dilute()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Dilute(String name,
		String description,
		String inputVolume,
		String outputVolume,
		List<IngredientAddition> ingredientAdditions)
	{
		super(name, description, Type.DILUTE, inputVolume, outputVolume);
		setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
	}

	/*-------------------------------------------------------------------------*/
	public Dilute(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.DILUTE), StringUtils.getProcessString("dilute.desc"), Type.DILUTE, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(StringUtils.getProcessString("dilute.output", getName()));
	}

	/*-------------------------------------------------------------------------*/
	public Dilute(Dilute step)
	{
		super(step.getName(), step.getDescription(), Type.DILUTE, step.getInputVolume(), step.getOutputVolume());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume input = getInputVolume(volumes);

		WaterAddition waterAddition = null;
		for (IngredientAddition item : getIngredientAdditions())
		{
			if (item instanceof WaterAddition)
			{
				// todo: support for multiple water additions?
				waterAddition = (WaterAddition)item;
			}
		}

		if (waterAddition == null)
		{
			log.addError(StringUtils.getProcessString("dilute.no.water.addition", getName()));
			return;
		}

		Volume result = Equations.dilute(input, waterAddition, getOutputVolume());

		volumes.addOrUpdateVolume(getOutputVolume(), result);
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Collections.singletonList(IngredientAddition.Type.WATER);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("dilute.step.desc");
	}

	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		for (WaterAddition wa : getWaterAdditions())
		{
			result.add(
				StringUtils.getDocString(
					"dilute.water.addition",
					wa.describe(),
					wa.getTemperature().describe(Quantity.Unit.CELSIUS)));
		}

		Volume postDilutionVol = getRecipe().getVolumes().getVolume(this.getOutputVolume());
		result.add(StringUtils.getDocString(
			"dilute.post.dilution",
			postDilutionVol.getVolume().describe(LITRES),
			postDilutionVol.getGravity().describe(SPECIFIC_GRAVITY),
			postDilutionVol.getTemperature().describe(CELSIUS)));

		return result;
	}

	@Override
	public ProcessStep clone()
	{
		return new Dilute(
			this.getName(),
			this.getDescription(),
			this.getInputVolume(),
			this.getOutputVolume(),
			cloneIngredients(getIngredientAdditions()));
	}
}
