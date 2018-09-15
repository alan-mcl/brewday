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
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Convert;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class SingleInfusionMash extends ProcessStep
{
	private String outputMashVolume;
	private String grainBillVol;
	private String waterVol;

	/** duration in minutes */
	private double duration;

	// todo calculate from strike water
	private double mashTemp;

	public SingleInfusionMash(
		String name,
		String description,
		String grainBillVol,
		String waterVol,
		String outputMashVolume,
		double duration,
		double mashTemp)
	{
		super(name, description);
		this.outputMashVolume = outputMashVolume;

		this.grainBillVol = grainBillVol;
		this.waterVol = waterVol;
		this.duration = duration;
		this.mashTemp = mashTemp;
	}

	@Override
	public List<String> apply(Volumes v)
	{
		IngredientAddition<FermentableAddition> ingredientAddition = (IngredientAddition<FermentableAddition>)v.getVolume(grainBillVol);
		Water water = (Water)v.getVolume(waterVol);

		double grainWeight = 0D;
		for (FermentableAddition f : ingredientAddition.getIngredients())
		{
			grainWeight += f.getWeight();
		}


		// todo: account for different grains in the grain bill
		double volumeOut = Equations.calcMashVolume(grainWeight, water.getVolume());

		// source: https://byo.com/article/hitting-target-original-gravity-and-volume-advanced-homebrewing/
		double extractPoints = 0D;
		for (FermentableAddition g : ingredientAddition.getIngredients())
		{
			extractPoints += Convert.gramsToLbs(g.getWeight()) * g.getFermentable().getExtractPotential();
		}

		// todo: externalise mash efficiency
		double actualExtract = extractPoints * Const.MASH_EFFICIENCY;

		double gravityOut = actualExtract / Convert.mlToGallons(volumeOut);

		double colourOut = Equations.calcSrmMoreyFormula(ingredientAddition, volumeOut);

		v.addVolume(
			outputMashVolume,
			new MashVolume(
				volumeOut,
				ingredientAddition,
				water,
				mashTemp,
				gravityOut,
				colourOut));

		ArrayList<String> result = new ArrayList<String>();
		result.add(outputMashVolume);
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		Water w = (Water)v.getVolume(waterVol);

		return String.format("Mash: single infusion %.1fL at %.1fC", w.getVolume()/1000, mashTemp);
	}
}
