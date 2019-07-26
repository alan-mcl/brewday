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
	public static TemperatureUnit calcNewFluidTemperature(
		VolumeUnit currentVolume,
		TemperatureUnit currentTemperature,
		VolumeUnit volumeAddition,
		TemperatureUnit tempAddition)
	{
		return new TemperatureUnit(
			(
				(currentVolume.get(Quantity.Unit.MILLILITRES) *
					currentTemperature.get(Quantity.Unit.CELSIUS) *
					Const.SPECIFIC_HEAT_OF_WATER)
				+
				volumeAddition.get(Quantity.Unit.MILLILITRES) *
					tempAddition.get(Quantity.Unit.CELSIUS) *
					Const.SPECIFIC_HEAT_OF_WATER
			)
			/
			(
				currentVolume.get(Quantity.Unit.MILLILITRES) *
					Const.SPECIFIC_HEAT_OF_WATER
					+
					volumeAddition.get(Quantity.Unit.MILLILITRES) *
						Const.SPECIFIC_HEAT_OF_WATER
			));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the gravity change when a volume change occurs.
	 *
	 * @return New gravity of the output volume.
	 */
	public static DensityUnit calcGravityWithVolumeChange(
		VolumeUnit volumeIn,
		DensityUnit gravityIn,
		VolumeUnit volumeOut)
	{
		return new DensityUnit(
			gravityIn.get() *
				volumeIn.get(Quantity.Unit.MILLILITRES) /
				volumeOut.get(Quantity.Unit.MILLILITRES));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the gravity of the combined fluids.
	 * source: https://www.quora.com/How-do-I-find-the-specific-gravity-when-two-liquids-are-mixed
	 * @return New gravity of the output volume.
	 */
	public static DensityUnit calcCombinedGravity(
		VolumeUnit v1,
		DensityUnit d1,
		VolumeUnit v2,
		DensityUnit d2)
	{
		return new DensityUnit(
			(v1.get() + v2.get()) /
				(v1.get()/d1.get()
					+
					v2.get()/d2.get()));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the volume decrease due to cooling.
	 *
	 * @return The new volume
	 */
	public static VolumeUnit calcCoolingShrinkage(
		VolumeUnit volumeIn,
		TemperatureUnit tempDecrease)
	{
		return new VolumeUnit(volumeIn.get(Quantity.Unit.MILLILITRES) *
			(1 - (Const.COOLING_SHRINKAGE * tempDecrease.get(Quantity.Unit.CELSIUS))));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the ABV change when a volume change occurs
	 *
	 * @return the new ABV
	 */
	public static double calcAbvWithVolumeChange(
		VolumeUnit volumeIn,
		double abvIn,
		VolumeUnit volumeOut)
	{
		return abvIn * volumeIn.get() / volumeOut.get();
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the ABV change when a gravity change occurs
	 *
	 * @return the new ABV, expressed within 0..1
	 */
	public static double calcAvbWithGravityChange(
		DensityUnit gravityIn,
		DensityUnit gravityOut)
	{
		double abv = (gravityIn.get() - gravityOut.get()) / gravityOut.get() * Const.ABV_CONST;
		return abv/100D;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the volume of the a new mash
	 *
	 * @param grainWeight in g
	 * @param waterVolume in ml
	 * @return Volume in ml
	 */
	public static VolumeUnit calcMashVolume(
		WeightUnit grainWeight,
		VolumeUnit waterVolume)
	{
		VolumeUnit absorbedWater = calcAbsorbedWater(grainWeight);
		double waterDisplacement = grainWeight.get(Quantity.Unit.GRAMS) * Const.GRAIN_WATER_DISPLACEMENT;

		return new VolumeUnit(
			waterVolume.get(Quantity.Unit.MILLILITRES)
				- absorbedWater.get(Quantity.Unit.MILLILITRES)
				+ waterDisplacement + grainWeight.get(Quantity.Unit.GRAMS));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param grainWeight in g
	 * @return volume of water absorbed in the grain, in ml
	 */
	public static VolumeUnit calcAbsorbedWater(WeightUnit grainWeight)
	{
		return new VolumeUnit(
			grainWeight.get(Quantity.Unit.KILOGRAMS) * Const.GRAIN_WATER_ABSORPTION);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the max volume of wort that can be drained from a given mash
	 *
	 * @param grainWeight in g
	 * @param waterVolume in ml
	 * @return Volume in ml
	 */
	public static VolumeUnit calcWortVolume(
		WeightUnit grainWeight, VolumeUnit waterVolume)
	{
		VolumeUnit absorbedWater = calcAbsorbedWater(grainWeight);

		return new VolumeUnit(waterVolume.get(Quantity.Unit.MILLILITRES)
			- absorbedWater.get(Quantity.Unit.MILLILITRES));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the SRM of the output wort using the Morey formula. Source:
	 * http://brewwiki.com/index.php/Estimating_Color
	 *
	 * @param waterVolume in ml
	 * @return wort colour in SRM
	 */
	public static ColourUnit calcSrmMoreyFormula(
		List<IngredientAddition> grainBill,
		VolumeUnit waterVolume)
	{
		// calc malt colour units
		double mcu = 0D;
		for (IngredientAddition item : grainBill)
		{
			FermentableAddition fa = (FermentableAddition)item;
			Fermentable f = fa.getFermentable();
			mcu += (f.getColour() * fa.getWeight().get(Quantity.Unit.POUNDS));
		}

		mcu /= waterVolume.get(Quantity.Unit.US_GALLON);

		// apply Dan Morey's formula
		return new ColourUnit(1.499D * (Math.pow(mcu, 0.6859D)));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @param volumeIn in ml, assumed SRM of 0
	 * @param colourIn in SRM
	 * @param volumeOut in ml
	 * @return colour in SRM
	 */
	public static ColourUnit calcColourWithVolumeChange(
		VolumeUnit volumeIn,
		ColourUnit colourIn,
		VolumeUnit volumeOut)
	{
		return new ColourUnit(colourIn.get(Quantity.Unit.SRM) *
			volumeIn.get(Quantity.Unit.MILLILITRES) /
			volumeOut.get(Quantity.Unit.MILLILITRES));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @param colour in SRM
	 * @return colour after fermentation, in SRM
	 */
	public static ColourUnit calcColourAfterFermentation(ColourUnit colour)
	{
		return new ColourUnit(
			colour.get() * (1 - Const.COLOUR_LOSS_DURING_FERMENTATION));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Source: http://www.realbeer.com/hops/research.html
	 * @param steepDuration in minutes
	 * @param wortGravity in GU (average during the steep duration)
	 * @param wortVolume in l (average during the steep duration)
	 */
	public static BitternessUnit calcIbuTinseth(
		HopAddition hopAddition,
		double steepDuration,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double equipmentHopUtilisation)
	{
		// adjust to sg
		double aveGrav = wortGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY);

		double bignessFactor = 1.65D * Math.pow(0.000125, aveGrav-1);
		double boilTimeFactor = (1D - Math.exp(-0.04 * steepDuration)) / 4.15D;
		double decimalAAUtilisation = bignessFactor * boilTimeFactor;

		Hop h = hopAddition.getHop();
		double mgPerL = (h.getAlphaAcid() * hopAddition.getWeight().get(Quantity.Unit.GRAMS) * 1000) /
			(wortVolume.get(Quantity.Unit.LITRES));

		return new BitternessUnit((mgPerL * decimalAAUtilisation) * equipmentHopUtilisation);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Given grain and water, returns the resultant mash temp.
	 * Source: http://howtobrew.com/book/section-3/the-methods-of-mashing/calculations-for-boiling-water-additions
	 * (rearranged the terms)
	 * @return
	 *  mash temp in C
	 */
	public static TemperatureUnit calcMashTemp(
		WeightUnit totalGrainWeight,
		WaterAddition strikeWater,
		TemperatureUnit grainTemp)
	{
		// ratio water to grain
		double r = strikeWater.getVolume().get(Quantity.Unit.MILLILITRES) /
			totalGrainWeight.get(Quantity.Unit.GRAMS);

		TemperatureUnit tw = strikeWater.getTemperature();

		double c = Const.MASH_TEMP_THERMO_CONST;

		return new TemperatureUnit(
			(c*grainTemp.get(Quantity.Unit.CELSIUS)
			+ r*tw.get(Quantity.Unit.CELSIUS))
				/ (c + r));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	Estimated apparent attenuation, in %
	 */
	public static double calcEstimatedAttenuation(
		WortVolume inputWort,
		YeastAddition yeastAddition,
		TemperatureUnit fermentationTemp)
	{
		// todo: this is a giant thumb suck made up by me, replace with something more scientific
		// looks like beersmith uses a curve of some sort

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

		if (fermentationTemp.get(Quantity.Unit.CELSIUS) < yeast.getMinTemp())
		{
			mod -= 0.05D;
		}
		else if (fermentationTemp.get(Quantity.Unit.CELSIUS) > yeast.getMaxTemp())
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
		WeightUnit totalGrainWeight,
		double mashEfficiency,
		WaterAddition mashWater)
	{
		// mash water-to-grain ratio in l/kg
		double r =
			(mashWater.getVolume().get(Quantity.Unit.LITRES)) /
				totalGrainWeight.get(Quantity.Unit.KILOGRAMS);

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
		VolumeUnit volumeOut)
	{
		double extractPoints = 0D;
		for (IngredientAddition item : grainBill)
		{
			FermentableAddition g = (FermentableAddition)item;
			extractPoints += g.getWeight().get(Quantity.Unit.POUNDS) * g.getFermentable().getExtractPotential();
		}

		double actualExtract = extractPoints * mashEfficiency;

		return new DensityUnit(actualExtract / volumeOut.get(Quantity.Unit.US_GALLON));
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
		HopAddition hopAdd = new HopAddition(hop, new WeightUnit(20), 60);

		for (double grav=1.01D; grav <1.08; grav = grav+.01)
		{
			BitternessUnit v = calcIbuTinseth(
				hopAdd,
				60,
				new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
				new VolumeUnit(20000),
				1.0D);

			System.out.println(grav+": "+v.get(Quantity.Unit.IBU));
		}
	}
}
