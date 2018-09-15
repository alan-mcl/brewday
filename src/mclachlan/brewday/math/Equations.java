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

package mclachlan.brewday.math;

import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class Equations
{
	/**
	 * Calculates the new temperature of the body of fluid after an addition of
	 * some amount at a different temperature.
	 *
	 * @return New temp of the combined fluid volume.
	 */
	public static double calcNewFluidTemperature(
		double currentVolume,
		double currentTemperature,
		double volumeAddition,
		double tempAddition)
	{
		return ((currentVolume * currentTemperature * Const.SPECIFIC_HEAT_OF_WATER) +
			volumeAddition * tempAddition * Const.SPECIFIC_HEAT_OF_WATER)
			/
			(currentVolume * Const.SPECIFIC_HEAT_OF_WATER + volumeAddition * Const.SPECIFIC_HEAT_OF_WATER);
	}

	/**
	 * Calculates the gravity change when a volume change occurs.
	 *
	 * @return New gravity of the output volume.
	 */
	public static double calcGravityWithVolumeChange(
		double volumeIn,
		double gravityIn,
		double volumeOut)
	{
		return gravityIn * volumeIn / volumeOut;
	}

	/**
	 * Calculates the volume decrease due to cooling.
	 *
	 * @return The new volume
	 */
	public static double calcCoolingShrinkage(double volumeIn,
		double tempDecrease)
	{
		return volumeIn * (1 - (Const.COOLING_SHRINKAGE * tempDecrease));
	}

	/**
	 * Calculates the ABV change when a volume change occurs
	 *
	 * @return the new ABV
	 */
	public static double calcAbvWithVolumeChange(
		double volumeIn,
		double abvIn,
		double volumeOut)
	{
		return abvIn * volumeIn / volumeOut;
	}

	/**
	 * Calculates the ABV change when a gravity change occurs
	 *
	 * @return the new ABV
	 */
	public static double calcAvbWithGravityChange(
		double gravityIn,
		double gravityOut)
	{
		return (gravityIn - gravityOut) / gravityOut * Const.ABV_CONST;
	}

	/**
	 * Calculates the volume of the a new mash
	 *
	 * @param grainWeight in g
	 * @param waterVolume in ml
	 * @return Volume in ml
	 */
	public static double calcMashVolume(double grainWeight, double waterVolume)
	{
		double absorbedWater = grainWeight / 1000 * Const.GRAIN_WATER_ABSORPTION;
		double waterDisplacement = grainWeight / 1000 * Const.GRAIN_WATER_DISPLACEMENT;

		return waterVolume - absorbedWater + waterDisplacement + grainWeight;
	}

	/**
	 * Calculates the max volume of wort that can be drained from a given mash
	 *
	 * @param grainWeight in g
	 * @param waterVolume in ml
	 * @return Volume in ml
	 */
	public static double calcWortVolume(double grainWeight, double waterVolume)
	{
		double absorbedWater = grainWeight / 1000 * Const.GRAIN_WATER_ABSORPTION;

		return waterVolume - absorbedWater;
	}

	/**
	 * Calculates the SRM of the output wort using the Morey formula. Source:
	 * http://brewwiki.com/index.php/Estimating_Color
	 *
	 * @param waterVolume in ml
	 * @return wort colour in SRM
	 */
	public static double calcSrmMoreyFormula(IngredientAddition<FermentableAddition> ingredientAddition, double waterVolume)
	{
		// calc malt colour units
		double mcu = 0D;
		for (FermentableAddition fermentableAddition : ingredientAddition.getIngredients())
		{
			Fermentable f = fermentableAddition.getFermentable();
			mcu += (f.getColour() * Convert.gramsToLbs(fermentableAddition.getWeight()));
		}

		mcu /= Convert.mlToGallons(waterVolume);

		// apply Dan Morey's formula
		return 1.499D * (Math.pow(mcu, 0.6859D));
	}

	/**
	 * @param volumeIn in ml, assumed SRM of 0
	 * @param colourIn in SRM
	 * @param volumeOut in ml
	 * @return colour in SRM
	 */
	public static double calcColourWithVolumeChange(double volumeIn, double colourIn, double volumeOut)
	{
		return colourIn * volumeIn / volumeOut;
	}

	/**
	 * @param colour in SRM
	 * @return colour after fermentation, in SRM
	 */
	public static double calcColourAfterFermentation(double colour)
	{
		return colour * (1 - Const.COLOUR_LOSS_DURING_FERMENTATION);
	}

	/**
	 * Source: http://www.realbeer.com/hops/research.html
	 * @param steepDuration in minutes
	 * @param wortGravity in GU (average during the steep duration)
	 * @param wortVolume in l (average during the steep duration)
	 */
	public static double calcIbuTinseth(
		IngredientAddition<HopAddition> hopCharge,
		double steepDuration,
		double wortGravity,
		double wortVolume)
	{
		double result = 0D;

		// adjust to decimal
		double aveGrav = (1000D + wortGravity) / 1000D;

		double bignessFactor = 1.65D * Math.pow(0.000125, aveGrav-1);
		double boilTimeFactor = (1D - Math.exp(-0.04 * steepDuration)) / 4.15D;
		double decimalAAUtilisation = bignessFactor * boilTimeFactor;

		for (HopAddition hopAddition : hopCharge.getIngredients())
		{
			Hop h = hopAddition.getHop();
			double mgPerL = (h.getAlphaAcid() * hopAddition.getWeight() * 1000) / (wortVolume/1000);
			result += (mgPerL * decimalAAUtilisation);
		}

		return result;
	}
}
