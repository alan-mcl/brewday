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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.YeastAddition;

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
	public void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log)
	{
		if (!validateInputVolume(volumes, log))
		{
			return;
		}

		EquipmentProfile equipmentProfile =
			Database.getInstance().getEquipmentProfiles().get(recipe.getEquipmentProfile());
		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		WortVolume inputWort = (WortVolume)getInputVolume(volumes);

		// todo: should we remove the trub/chiller loss here, or in some other step?
		// what about transfer out of the boil?
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
			}
		}

		if (yeastAddition == null)
		{
			log.addError(StringUtils.getProcessString("ferment.no.yeast.addition"));
			estimatedFinalGravity = inputWort.getGravity();
			return;
		}

		double estAtten = Equations.calcEstimatedAttenuation(inputWort, yeastAddition, temp);

		estimatedFinalGravity = new DensityUnit(inputWort.getGravity().get() * (1-estAtten));

		double abvOut = Equations.calcAvbWithGravityChange(inputWort.getGravity(), estimatedFinalGravity);
		ColourUnit colourOut = Equations.calcColourAfterFermentation(inputWort.getColour());

		volumes.addVolume(
			getOutputVolume(),
			new BeerVolume(
				inputWort.getVolume(),
				inputWort.getTemperature(),
				inputWort.getGravity(),
				estimatedFinalGravity,
				inputWort.getAbv() + abvOut,
				colourOut,
				inputWort.getBitterness()));
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public void dryRun(Recipe recipe, ErrorsAndWarnings log)
	{
		recipe.getVolumes().addVolume(getOutputVolume(), new BeerVolume());
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
}
