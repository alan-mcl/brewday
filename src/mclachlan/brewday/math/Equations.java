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

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.process.WortVolume;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class Equations
{
	/*-------------------------------------------------------------------------*/
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

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the gravity change when a volume change occurs.
	 *
	 * @return New gravity of the output volume.
	 */
	public static DensityUnit calcGravityWithVolumeChange(
		double volumeIn,
		DensityUnit gravityIn,
		double volumeOut)
	{
		return new DensityUnit(gravityIn.getDensity() * volumeIn / volumeOut);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the gravity of the combined fluids.
	 * source: https://www.quora.com/How-do-I-find-the-specific-gravity-when-two-liquids-are-mixed
	 * @return New gravity of the output volume.
	 */
	public static DensityUnit calcCombinedGravity(double v1, DensityUnit d1, double v2, DensityUnit d2)
	{
		return new DensityUnit((v1 + v2) / (v1/d1.getDensity() + v2/d2.getDensity()));
	}

	/*-------------------------------------------------------------------------*/
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

	/*-------------------------------------------------------------------------*/
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

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the ABV change when a gravity change occurs
	 *
	 * @return the new ABV
	 */
	public static double calcAvbWithGravityChange(
		DensityUnit gravityIn,
		DensityUnit gravityOut)
	{
		return (gravityIn.getDensity() - gravityOut.getDensity()) / gravityOut.getDensity() * Const.ABV_CONST;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the volume of the a new mash
	 *
	 * @param grainWeight in g
	 * @param waterVolume in ml
	 * @return Volume in ml
	 */
	public static double calcMashVolume(double grainWeight, double waterVolume)
	{
		double absorbedWater = calcAbsorbedWater(grainWeight);
		double waterDisplacement = grainWeight / 1000 * Const.GRAIN_WATER_DISPLACEMENT;

		return waterVolume - absorbedWater + waterDisplacement + grainWeight;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param grainWeight in g
	 * @return volume of water absorbed in the grain, in ml
	 */
	public static double calcAbsorbedWater(double grainWeight)
	{
		return grainWeight * Const.GRAIN_WATER_ABSORPTION;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the max volume of wort that can be drained from a given mash
	 *
	 * @param grainWeight in g
	 * @param waterVolume in ml
	 * @return Volume in ml
	 */
	public static double calcWortVolume(double grainWeight, double waterVolume)
	{
		double absorbedWater = calcAbsorbedWater(grainWeight);

		return waterVolume - absorbedWater;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the SRM of the output wort using the Morey formula. Source:
	 * http://brewwiki.com/index.php/Estimating_Color
	 *
	 * @param waterVolume in ml
	 * @return wort colour in SRM
	 */
	public static double calcSrmMoreyFormula(List<IngredientAddition> grainBill, double waterVolume)
	{
		// calc malt colour units
		double mcu = 0D;
		for (IngredientAddition item : grainBill)
		{
			FermentableAddition fa = (FermentableAddition)item;
			Fermentable f = fa.getFermentable();
			mcu += (f.getColour() * Convert.gramsToLbs(fa.getWeight()));
		}

		mcu /= Convert.mlToGallons(waterVolume);

		// apply Dan Morey's formula
		return 1.499D * (Math.pow(mcu, 0.6859D));
	}

	/*-------------------------------------------------------------------------*/
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

	/*-------------------------------------------------------------------------*/
	/**
	 * @param colour in SRM
	 * @return colour after fermentation, in SRM
	 */
	public static double calcColourAfterFermentation(double colour)
	{
		return colour * (1 - Const.COLOUR_LOSS_DURING_FERMENTATION);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Source: http://www.realbeer.com/hops/research.html
	 * @param steepDuration in minutes
	 * @param wortGravity in GU (average during the steep duration)
	 * @param wortVolume in l (average during the steep duration)
	 */
	public static double calcIbuTinseth(
		HopAddition hopAddition,
		double steepDuration,
		DensityUnit wortGravity,
		double wortVolume,
		double equipmentHopUtilisation)
	{
		// adjust to sg
		double aveGrav = wortGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY);

		double bignessFactor = 1.65D * Math.pow(0.000125, aveGrav-1);
		double boilTimeFactor = (1D - Math.exp(-0.04 * steepDuration)) / 4.15D;
		double decimalAAUtilisation = bignessFactor * boilTimeFactor;

		Hop h = hopAddition.getHop();
		double mgPerL = (h.getAlphaAcid() * hopAddition.getWeight() * 1000) / (wortVolume/1000);
		return (mgPerL * decimalAAUtilisation) * equipmentHopUtilisation;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Given grain and water, returns the resultant mash temp.
	 * Source: http://howtobrew.com/book/section-3/the-methods-of-mashing/calculations-for-boiling-water-additions
	 * (rearranged the terms)
	 * @return
	 *  mash temp in C
	 */
	public static double calcMashTemp(
		double totalGrainWeight,
		WaterAddition strikeWater,
		double grainTemp)
	{
		// ratio water to grain
		double r = strikeWater.getVolume() / totalGrainWeight;

		double tw = strikeWater.getTemperature();

		double c = Const.MASH_TEMP_THERMO_CONST;

		return (c*grainTemp + r*tw) / (c + r);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	Estimated apparent attenuation, in %
	 */
	public static double calcEstimatedAttenuation(
		WortVolume inputWort,
		YeastAddition yeastAddition,
		double fermentationTemp)
	{
		// todo: this is a giant thumb suck made up by me, replace with something more scientific

		WortVolume.Fermentability wortFermentability = inputWort.getFermentability();

		Yeast yeast = yeastAddition.getYeast();
		double yeastAttenuation = yeast.getAttenuation();
		double mod = 0.0D;
		switch (wortFermentability)
		{
			case HIGH: mod += 0.05D; break;
			case MEDIUM: mod += 0.0D; break;
			case LOW: mod -= 0.05D; break;
			default: throw new BrewdayException(""+wortFermentability);
		}

		if (fermentationTemp < yeast.getMinTemp())
		{
			mod -= 0.05D;
		}
		else if (fermentationTemp > yeast.getMaxTemp())
		{
			mod += 0.05D;
		}

		//todo yeast pitch rate impact on attenuation

		return (yeastAttenuation + mod);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates mash gravity using the grain yield to derive degrees Plato
	 * <p>
	 * Source: http://braukaiser.com/wiki/index.php/Understanding_Efficiency
	 */
	public static DensityUnit calcMashExtractContent(
		List<IngredientAddition> grainBill,
		double totalGrainWeight,
		double mashEfficiency,
		WaterAddition mashWater)
	{
		// mash water-to-grain ratio in l/kg
		double r =
			(mashWater.getVolume()) / totalGrainWeight;

		double result = 0D;

		for (IngredientAddition item : grainBill)
		{
			FermentableAddition fa = (FermentableAddition)item;
			double yield = fa.getFermentable().getYield();
			result += (mashEfficiency * 100 * (yield / (r + yield)));
		}

		return new DensityUnit(result, DensityUnit.Unit.PLATO);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates mash gravity using the extract points / ppg method to derive SG.
	 * <p>
	 * Source: https://byo.com/article/hitting-target-original-gravity-and-volume-advanced-homebrewing/
	 */
	public static DensityUnit calcMashExtractContent(
		List<IngredientAddition> grainBill,
		double mashEfficiency,
		double volumeOut)
	{
		double extractPoints = 0D;
		for (IngredientAddition item : grainBill)
		{
			FermentableAddition g = (FermentableAddition)item;
			extractPoints += Convert.gramsToLbs(g.getWeight()) * g.getFermentable().getExtractPotential();
		}

		double actualExtract = extractPoints * mashEfficiency;

		return new DensityUnit(actualExtract / Convert.mlToGallons(volumeOut));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://www.howtobrew.com/book/section-2/what-is-malted-grain/extraction-and-maximum-yield
	 * @param yield
	 * 	the grain yield in %
	 * @return
	 * 	the extract potential in ppg
	 */
	public static double calcExtractPotentialFromYield(double yield)
	{
		return 46 * yield;
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		Hop hop = new Hop();
		hop.setAlphaAcid(.2);
		HopAddition hopAdd = new HopAddition(hop, 20, 60);

		for (double grav=1.01D; grav <1.08; grav = grav+.01)
		{
			double v = calcIbuTinseth(
				hopAdd,
				60,
				new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
				20000,
				1.0D);

			System.out.println(grav+": "+v);
		}
	}
}
