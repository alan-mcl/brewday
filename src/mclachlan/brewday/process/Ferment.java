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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
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
	private double temp;

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
		double temp,
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
		super(recipe.getUniqueStepName(Type.FERMENT), "Ferment", Type.FERMENT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(getName()+" output");
		setTemperature(20D);
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
			log.addError("invalid equipment profile ["+equipmentProfile+"]");
			return;
		}

		WortVolume inputWort = (WortVolume)getInputVolume(volumes);

		// todo: should this be done here?
		inputWort.setVolume(inputWort.getVolume() - equipmentProfile.getTrubAndChillerLoss());

		if (inputWort.getVolume()*1.2 > equipmentProfile.getFermenterVolume())
		{
			log.addWarning(
				String.format(
					"Fermenter (%.2f l) may not be large enough for fermentation volume (%.2f l)",
					equipmentProfile.getFermenterVolume()/1000, inputWort.getVolume()/1000));
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
			log.addError("No yeast addition in fermentation step.");
			estimatedFinalGravity = inputWort.getGravity();
			return;
		}

		double estAtten = Equations.calcEstimatedAttenuation(inputWort, yeastAddition, temp);

		estimatedFinalGravity = new DensityUnit(inputWort.getGravity().get() * (1-estAtten));

		double abvOut = Equations.calcAvbWithGravityChange(inputWort.getGravity(), estimatedFinalGravity);
		double colourOut = Equations.calcColourAfterFermentation(inputWort.getColour());

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
		return String.format("Ferment: %.1fC", temp);
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(
			IngredientAddition.Type.YEAST,
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.FERMENTABLES);
	}

	/*-------------------------------------------------------------------------*/
	public double getTemperature()
	{
		return temp;
	}

	public void setTemperature(double temp)
	{
		this.temp = temp;
	}

	public DensityUnit getEstimatedFinalGravity()
	{
		return estimatedFinalGravity;
	}
}
