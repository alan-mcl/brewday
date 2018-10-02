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
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.AdditionSchedule;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.recipe.YeastAdditionList;

/**
 *
 */
public class Ferment extends FluidVolumeProcessStep
{
	// todo: time

	/** fermentation temperature in C */
	private double temp;

	/** calculated */
	private double estimatedFinalGravity;

	/*-------------------------------------------------------------------------*/
	public Ferment(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double temp)
	{
		super(name, description, Type.FERMENT, inputVolume, outputVolume);
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
	@Override
	public void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log)
	{
		if (!validateInputVolume(volumes, log))
		{
			return;
		}

		WortVolume inputWort = (WortVolume)getInputVolume(volumes);

		// todo: support for multiple yeast additions
		YeastAddition yeastAddition = null;
		for (AdditionSchedule schedule : getIngredientAdditions())
		{
			Volume v = volumes.getVolume(schedule.getIngredientAddition());
			if (v instanceof YeastAdditionList)
			{
				if (((YeastAdditionList)v).getIngredients().size() > 0)
				{
					// todo: yeast blends
					yeastAddition = ((YeastAdditionList)v).getIngredients().get(0);
				}
			}
		}

		if (yeastAddition == null)
		{
			log.addError("No yeast addition in fermentation step.");
			estimatedFinalGravity = inputWort.getGravity();
			return;
		}

		double estAtten = Equations.getEstimatedAttenuation(inputWort, yeastAddition, temp);

		estimatedFinalGravity = inputWort.getGravity() * (1-estAtten);

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
	public String describe(Volumes v)
	{
		return String.format("Ferment: %.1fC", temp);
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<Volume.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(Volume.Type.YEAST, Volume.Type.HOPS, Volume.Type.FERMENTABLES);
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

	public double getEstimatedFinalGravity()
	{
		return estimatedFinalGravity;
	}
}
