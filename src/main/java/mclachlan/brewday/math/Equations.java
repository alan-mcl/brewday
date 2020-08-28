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

package mclachlan.brewday.math;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.process.Volume;
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
	public static TemperatureUnit calcCombinedTemperature(
		VolumeUnit currentVolume,
		TemperatureUnit currentTemperature,
		VolumeUnit volumeAddition,
		TemperatureUnit tempAddition)
	{
		boolean estimated =
			currentVolume.isEstimated() || currentTemperature.isEstimated() ||
			volumeAddition.isEstimated() || tempAddition.isEstimated();

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
			),
			Quantity.Unit.CELSIUS,
			estimated);
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
		boolean estimated = volumeIn.isEstimated() || gravityIn.isEstimated() || volumeOut.isEstimated();

		return new DensityUnit(
			gravityIn.get() *
				volumeIn.get(Quantity.Unit.MILLILITRES) /
				volumeOut.get(Quantity.Unit.MILLILITRES),
			gravityIn.getUnit(),
			estimated);
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
		boolean estimated = v1.isEstimated() || d1.isEstimated() || v2.isEstimated() || d2.isEstimated();

		return new DensityUnit(
			(v1.get() + v2.get()) /
				(v1.get()/d1.get()
					+
					v2.get()/d2.get()),
			d1.getUnit(),
			estimated);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the colour of the combined fluids.
	 * Source: I made this up
	 * @return New colour of the output volume.
	 */
	public static ColourUnit calcCombinedColour(
		VolumeUnit v1,
		ColourUnit c1,
		VolumeUnit v2,
		ColourUnit c2)
	{
		return (ColourUnit)calcCombinedLinearInterpolation(v1, c1, v2, c2);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Uses linear interpolation to calculate a general combined quantity.
	 * Tries its best to return the right quantity class.
	 * Source: I made this up
	 */
	public static Quantity calcCombinedLinearInterpolation(
		VolumeUnit v1,
		Quantity q1,
		VolumeUnit v2,
		Quantity q2)
	{
		if (v1 == null || q1 == null || v2 == null || q2 == null)
		{
			return null;
		}

		boolean estimated = v1.isEstimated() || q1.isEstimated() || v2.isEstimated() || q2.isEstimated();

		double vc = v1.get() + v2.get();
		double qc = (v1.get() / vc * q1.get()) + (v2.get() / vc * q2.get());

		Quantity result = Quantity.parseQuantity(""+qc, q1.getUnit());
		result.setEstimated(estimated);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the bitterness of the combined fluids.
	 * Source: I made this up
	 * @return New bitterness of the combined volume.
	 */
	public static BitternessUnit calcCombinedBitterness(
		VolumeUnit v1,
		BitternessUnit b1,
		VolumeUnit v2,
		BitternessUnit b2)
	{
		return (BitternessUnit)calcCombinedLinearInterpolation(v1, b1, v2, b2);
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
		boolean estimated = volumeIn.isEstimated() || tempDecrease.isEstimated();

		return new VolumeUnit(
			volumeIn.get(Quantity.Unit.MILLILITRES) *
			(1 - (Const.COOLING_SHRINKAGE * tempDecrease.get(Quantity.Unit.CELSIUS))),
			Quantity.Unit.MILLILITRES,
			estimated);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the ABV change when a volume change occurs
	 *
	 * @return the new ABV
	 */
	public static PercentageUnit calcAbvWithVolumeChange(
		VolumeUnit volumeIn,
		PercentageUnit abvIn,
		VolumeUnit volumeOut)
	{
		if (volumeIn == null || abvIn == null || volumeOut == null)
		{
			return null;
		}
		boolean estimated = volumeIn.isEstimated() || abvIn.isEstimated() || volumeOut.isEstimated();

		double abvInD = abvIn==null ? 0 : abvIn.get();
		double volInD = volumeIn.get();
		double volOutD = volumeOut.get();
		return new PercentageUnit(abvInD * volInD / volOutD, estimated);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the ABV change when a gravity change occurs.
	 * Source: http://www.brewunited.com/abv_calculator.php
	 *
	 * @return the new ABV, expressed within 0..1
	 */
	public static PercentageUnit calcAbvWithGravityChange(
		DensityUnit gravityIn,
		DensityUnit gravityOut)
	{
		double abv = (gravityIn.get(Quantity.Unit.SPECIFIC_GRAVITY) - gravityOut.get(Quantity.Unit.SPECIFIC_GRAVITY)) * Const.ABV_CONST;
		boolean estimated = gravityIn.isEstimated() || gravityOut.isEstimated();
		return new PercentageUnit(abv/100D, estimated);
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
		boolean estimated = grainWeight.isEstimated() || waterVolume.isEstimated();

		return new VolumeUnit(
			waterVolume.get(Quantity.Unit.MILLILITRES)
				- absorbedWater.get(Quantity.Unit.MILLILITRES)
				+ waterDisplacement + grainWeight.get(Quantity.Unit.GRAMS),
			Quantity.Unit.MILLILITRES,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param grainWeight in g
	 * @return volume of water absorbed in the grain, in ml
	 */
	public static VolumeUnit calcAbsorbedWater(WeightUnit grainWeight)
	{
		boolean estimated = grainWeight.isEstimated();

		return new VolumeUnit(
			grainWeight.get(Quantity.Unit.KILOGRAMS) * Const.GRAIN_WATER_ABSORPTION,
			Quantity.Unit.LITRES,
			estimated);
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

		boolean estimated = absorbedWater.isEstimated() || grainWeight.isEstimated();
		return new VolumeUnit(
			waterVolume.get(Quantity.Unit.MILLILITRES)
				- absorbedWater.get(Quantity.Unit.MILLILITRES),
			Quantity.Unit.MILLILITRES,
			estimated);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Calculates the SRM of the output wort using the Morey formula. Source:
	 * http://brewwiki.com/index.php/Estimating_Color
	 *
	 * @param waterVolume in ml
	 * @return wort colour in SRM
	 */
	public static ColourUnit calcColourSrmMoreyFormula(
		List<IngredientAddition> grainBill,
		VolumeUnit waterVolume)
	{
		if (grainBill.isEmpty())
		{
			return new ColourUnit(0D, Quantity.Unit.SRM, false);
		}

		// calc malt colour units
		double mcu = 0D;
		for (IngredientAddition item : grainBill)
		{
			FermentableAddition fa = (FermentableAddition)item;
			Fermentable f = fa.getFermentable();

			double colour = f.getColour().get(Quantity.Unit.SRM); // I think this was imported as Lovibond?
			double weight = fa.getQuantity().get(Quantity.Unit.POUNDS);

			mcu += (colour * weight);
		}

		mcu /= waterVolume.get(Quantity.Unit.US_GALLON);

		// apply Dan Morey's formula
		return new ColourUnit(1.499D * (Math.pow(mcu, 0.6859D)), Quantity.Unit.SRM, true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the colour impact of a boil.
	 *
	 * @param colourIn
	 * @return
	 */
	public static ColourUnit calcColourAfterBoil(ColourUnit colourIn)
	{
		//
		// Brewday has an issue with colour calculations: existing formulae (eg
		// Morey) require the use of MCUs based on post-boil gravity.
		// (source: http://www.beersmith.com/forum/index.php?topic=5797.0)
		// But Brewday can't easily do that because the process steps are
		// decoupled and there isn't necessarily a 1:1 mapping from mash to boil.
		//
		// One option would be passing MCUs around as a metric in the volumes,
		// waiting to arrive at a post-boil volume. I doubt this would work
		// properly and haven't tried it yet.
		//
		// Instead I'm doing this: the typical homebrew process produces a post-boil
		// volume about 60% of the input water. Working out a table of SRM values
		// shows me that the SRM output is 42% higher when the MCU's are worked
		// out with 60% of the water volume.
		// So to model this in Brewday at boil time we increase the SRM by 42%.
		//
		// This is kinda wacky I admit. But to quote Palmer, there are "inherent
		// limits of any model for beer colour" so I guess it's best to be a bit
		// relaxed about this stuff.
		//

		double srmIn = colourIn.get(Quantity.Unit.SRM);
		return new ColourUnit(srmIn*1.42, Quantity.Unit.SRM);
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
		boolean estimated = volumeIn.isEstimated() || colourIn.isEstimated() || volumeOut.isEstimated();

		return new ColourUnit(colourIn.get(Quantity.Unit.SRM) *
			volumeIn.get(Quantity.Unit.MILLILITRES) /
			volumeOut.get(Quantity.Unit.MILLILITRES),
			Quantity.Unit.SRM,
			estimated);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @param volumeIn assumed IBU of 0
	 */
	public static BitternessUnit calcBitternessWithVolumeChange(
		VolumeUnit volumeIn,
		BitternessUnit bitternessIn,
		VolumeUnit volumeOut)
	{
		if (bitternessIn == null)
		{
			return new BitternessUnit(0);
		}

		boolean estimated = volumeIn.isEstimated() || bitternessIn.isEstimated() || volumeOut.isEstimated();

		return new BitternessUnit(
			bitternessIn.get(Quantity.Unit.IBU) *
				volumeIn.get(Quantity.Unit.MILLILITRES) /
				volumeOut.get(Quantity.Unit.MILLILITRES),
			Quantity.Unit.IBU,
			estimated);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @param colour in SRM
	 * @return colour after fermentation, in SRM
	 */
	public static ColourUnit calcColourAfterFermentation(ColourUnit colour)
	{
		return new ColourUnit(
			colour.get(Quantity.Unit.SRM) * (1 - Const.COLOUR_LOSS_DURING_FERMENTATION),
			Quantity.Unit.SRM,
			colour.isEstimated());
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
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double utilisation)
	{
		// adjust to sg
		double aveGrav = wortGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY);

		double bignessFactor = 1.65D * Math.pow(0.000125, aveGrav-1);
		double boilTimeFactor = (1D - Math.exp(-0.04 * steepDuration.get(Quantity.Unit.MINUTES))) / 4.15D;
		double decimalAAUtilisation = bignessFactor * boilTimeFactor;

		Hop h = hopAddition.getHop();
		double alpha = h.getAlphaAcid().get(Quantity.Unit.PERCENTAGE);
		double weight = hopAddition.getQuantity().get(Quantity.Unit.GRAMS);

		double mgPerL = (alpha * weight * 1000) / (wortVolume.get(Quantity.Unit.LITRES));

		boolean estimated = wortGravity.isEstimated() || wortVolume.isEstimated();

		return new BitternessUnit(
			(mgPerL * decimalAAUtilisation) * utilisation,
			Quantity.Unit.IBU,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	The total IBUs from the whole hop bill, using the Tinseth method.
	 */
	public static BitternessUnit calcTotalIbuTinseth(
		List<HopAddition> hopAdditions,
		DensityUnit wortDensity,
		VolumeUnit wortVolume,
		double utilisation)
	{
		BitternessUnit bitternessOut = new BitternessUnit(0);
		for (IngredientAddition hopCharge : hopAdditions)
		{
			bitternessOut.add(
				Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					wortDensity,
					wortVolume,
					utilisation));
		}

		return bitternessOut;
	}

	/*-------------------------------------------------------------------------*/

	public static TemperatureUnit calcStandEndingTemperature(
		TemperatureUnit inputTemp,
		TimeUnit standDuration)
	{
		double inC = inputTemp.get(Quantity.Unit.CELSIUS);
		double lossC = Const.HEAT_LOSS * standDuration.get(Quantity.Unit.HOURS);
		return new TemperatureUnit(inC - lossC, Quantity.Unit.CELSIUS);
	}


	/*-------------------------------------------------------------------------*/

	/**
	 * Source: https://alchemyoverlord.wordpress.com/2015/05/12/a-modified-ibu-measurement-especially-for-late-hopping/
	 * @return
	 * 	The IBU added by a given post-boil hop stand.
	 */
	public static BitternessUnit calcHopStandIbu(
		List<IngredientAddition> hopAdditions,
		DensityUnit wortDensity,
		VolumeUnit wortVolume,
		TimeUnit boilTime,
		TimeUnit coolTime)
	{
		double hopStandUtilization;

		double integrationTime = 0.001;
  		double decimalAArating = 0.0;

  		double boilMin = boilTime.get(Quantity.Unit.MINUTES);
  		double coolMin = coolTime.get(Quantity.Unit.MINUTES);
  		double boilGravity = wortDensity.get(Quantity.Unit.SPECIFIC_GRAVITY);

		for (double t = boilMin; t < boilMin + coolMin; t = t + integrationTime)
		{
			double dU = -1.65 * Math.pow(0.000125, (boilGravity-1.0)) * -0.04 * Math.exp(-0.04*t) / 4.15;

			// this is how the source article does it. this is cool, one day...

//			surfaceArea_cm2 = 3.14159 * (kettleDiameter_cm/2.0) * (kettleDiameter_cm/2.0);
//			openingArea_cm2 = 3.14159 * (openingDiameter_cm/2.0) * (openingDiameter_cm/2.0);
//			effectiveArea_cm2 = sqrt(surfaceArea_cm2 * openingArea_cm2);
//			b = (0.0002925 * effectiveArea_cm2 / volume_liters) + 0.00538;
//			temp_degK = 53.70 * exp(-1.0 * b * (t - boilTime_min)) + 319.55;

			// ... but for now instead we just use the cooling constant fudge
			TemperatureUnit endTemp = calcStandEndingTemperature(new TemperatureUnit(100, Quantity.Unit.CELSIUS), coolTime);
			double tempK = endTemp.get(Quantity.Unit.KELVIN);

			double degreeOfUtilization = 2.39*Math.pow(10.0,11.0)*Math.exp(-9773.0/tempK);

			if (t < 5.0) degreeOfUtilization = 1.0;  // account for nonIAA components

			double combinedValue = dU * degreeOfUtilization;
			decimalAArating = decimalAArating + (combinedValue * integrationTime);
		}

		hopStandUtilization = decimalAArating;

		BitternessUnit bitternessOut = new BitternessUnit(0);
		for (IngredientAddition hopCharge : hopAdditions)
		{
			bitternessOut.add(
				Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					wortDensity,
					wortVolume,
					hopStandUtilization));
		}

		return new BitternessUnit(bitternessOut.get());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: https://sciencing.com/calculate-tons-cooling-cooling-tower-10058467.html
	 */
	public static TimeUnit calcHeatingTime(
		VolumeUnit volume,
		TemperatureUnit startTemp,
		TemperatureUnit endTemp,
		PowerUnit heatingPower)
	{
		double td = endTemp.get(Quantity.Unit.CELSIUS) - startTemp.get(Quantity.Unit.CELSIUS);

		double kWh = (4.2 * volume.get(Quantity.Unit.LITRES) * td ) / 3600;

		return new TimeUnit(
			kWh / heatingPower.get(Quantity.Unit.KILOWATT),
			Quantity.Unit.HOURS,
			true);
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

		boolean estimated = totalGrainWeight.isEstimated() || grainTemp.isEstimated();

		return new TemperatureUnit(
			(c*grainTemp.get(Quantity.Unit.CELSIUS)
			+ r*tw.get(Quantity.Unit.CELSIUS))
				/ (c + r),
			Quantity.Unit.CELSIUS,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://braukaiser.com/wiki/index.php/Effects_of_mash_parameters_on_fermentability_and_efficiency_in_single_infusion_mashing
	 * @param mashTemp
	 * 	The average mash temperature
	 * @return
	 * 	The estimated attenuation limit of the wort produced
	 */
	public static PercentageUnit getWortAttenuationLimit(
		TemperatureUnit mashTemp)
	{
		// per Braukaiser:
		// for mash temp >= 67.5C we model a line A = 0.9 - 0.04*(T - 67.5)
		// for mash temp < 67.5 we model a line A = 0.9 - 0.015*(67.5-T)

		double result;
		double tempC = mashTemp.get(Quantity.Unit.CELSIUS);

		if (tempC >= 67.5)
		{
			result = 0.9 - 0.04*(tempC-67.5);
		}
		else
		{
			result = 0.9 - 0.015*(67.5-tempC);
		}

		return new PercentageUnit(result, true);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 *
	 * @return
	 * 	Estimated apparent attenuation, in %
	 */
	public static double calcEstimatedAttenuation(Volume inputWort, YeastAddition yeastAddition)
	{
		if (yeastAddition == null)
		{
			return 0D;
		}

		PercentageUnit wortAttenuationLimit = inputWort.getFermentability();
		if (wortAttenuationLimit == null)
		{
			// assume the peak
			wortAttenuationLimit = new PercentageUnit(0.9D);
		}

		Yeast yeast = yeastAddition.getYeast();
		double yeastAttenuation = yeast.getAttenuation().get(Quantity.Unit.PERCENTAGE);
		double wortAttenuation = wortAttenuationLimit.get(Quantity.Unit.PERCENTAGE);

		// Return an attenuation midway between the yeast average attenuation and
		// the wort attenuation limit.
		// I have no scientific basis for this piece of math, it just feel about
		// right from personal experience looking at the listed yeast attenuation
		// numbers in the db

		if (wortAttenuation < yeastAttenuation)
		{
			return wortAttenuation + (yeastAttenuation-wortAttenuation)/2;
		}
		else
		{
			return yeastAttenuation + (wortAttenuation-yeastAttenuation)/2;
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates mash gravity using the grain yield to derive degrees Plato
	 * <p>
	 * Source: http://braukaiser.com/wiki/index.php/Understanding_Efficiency
	 */
	public static DensityUnit calcMashExtractContentFromYield(
		List<IngredientAddition> grainBill,
		double mashEfficiency,
		WaterAddition mashWater)
	{
		WeightUnit totalGrainWeight = getTotalGrainWeight(grainBill);

		// mash water-to-grain ratio in l/kg
		double r =
			(mashWater.getVolume().get(Quantity.Unit.LITRES)) /
				totalGrainWeight.get(Quantity.Unit.KILOGRAMS);

		double result = 0D;

		for (IngredientAddition item : grainBill)
		{
			FermentableAddition fa = (FermentableAddition)item;
			double yield = fa.getFermentable().getYield().get(Quantity.Unit.PERCENTAGE);
			double proportion = fa.getQuantity().get(Quantity.Unit.GRAMS) /
				totalGrainWeight.get(Quantity.Unit.GRAMS);

			result += mashEfficiency * 100 * proportion * (yield / (r + yield));
		}

		return new DensityUnit(result, DensityUnit.Unit.PLATO, true);
	}

	/*-------------------------------------------------------------------------*/
	public static WeightUnit getTotalGrainWeight(List<IngredientAddition> grainBill)
	{
		double result = 0D;
		for (IngredientAddition item : grainBill)
		{
			result += item.getQuantity().get(Quantity.Unit.GRAMS);
		}
		return new WeightUnit(result, Quantity.Unit.GRAMS, false);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates mash gravity using the extract points / ppg method to derive SG.
	 * <p>
	 * Source: https://byo.com/article/hitting-target-original-gravity-and-volume-advanced-homebrewing/
	 */
	public static DensityUnit calcMashExtractContentFromPppg(
		List<IngredientAddition> grainBill,
		double mashEfficiency,
		VolumeUnit volumeOut)
	{
		double extractPoints = 0D;
		for (IngredientAddition item : grainBill)
		{
			if (item instanceof FermentableAddition)
			{
				FermentableAddition fa = (FermentableAddition)item;
				PercentageUnit yield = fa.getFermentable().getYield();
				double pppg = calcExtractPotentialFromYield(yield);
				extractPoints += fa.getQuantity().get(Quantity.Unit.POUNDS) * pppg;
			}
		}

		double actualExtract = extractPoints * mashEfficiency;


		double gal = volumeOut.get(Quantity.Unit.US_GALLON);
		return new DensityUnit(actualExtract / gal);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the gravity returned by steeping the given grains.
	 * Source: Beersmith
	 */
	public static DensityUnit calcSteepedGrainsGravity(
		List<IngredientAddition> grainBill,
		VolumeUnit volumeOut)
	{
		// treat a steep like a mash with 15% efficiency
		return calcMashExtractContentFromPppg(grainBill, 0.15D, volumeOut);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the gravity provided by just dissolving the given fermentable
	 * in the given volume of fluid.
	 * <p>
	 * Source: http://braukaiser.com/wiki/index.php/Troubleshooting_Brewhouse_Efficiency
	 * @return The additional gravity
	 */
	public static DensityUnit calcSteepedFermentableAdditionGravity(
		FermentableAddition fermentableAddition,
		VolumeUnit volume)
	{
		Fermentable fermentable = fermentableAddition.getFermentable();
		Fermentable.Type type = fermentable.getType();

		double pppg = calcExtractPotentialFromYield(fermentable.getYield());

		if (type == Fermentable.Type.GRAIN || type == Fermentable.Type.ADJUNCT)
		{
			// these are not soluble
			// however if these are grains with a diastatic power of 0 then we expect some
			// gravit from steeping them

			if (type == Fermentable.Type.GRAIN && fermentable.getDiastaticPower().get() <= 0)
			{
				// drawing a curve from the data here: http://howtobrew.com/book/section-2/steeping-specialty-grains/mechanics-of-steeping
				// we estimate the ppg from the yield

				pppg = -27.087*fermentable.getYield().get() + 33.188;
			}
			else
			{
				return new DensityUnit(0);
			}
		}

		double weightLb = fermentableAddition.getQuantity().get(Quantity.Unit.POUNDS);
		double volumeGal = volume.get(Quantity.Unit.US_GALLON);

		double points = weightLb * pppg / volumeGal;

		return new DensityUnit(points, Quantity.Unit.GU, true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the colour provided by just dissolving the given fermentable
	 * in the given volume of fluid.
	 * @return The additional colour
	 */
	public static ColourUnit calcSolubleFermentableAdditionColourContribution(
		FermentableAddition fermentableAddition,
		VolumeUnit volume)
	{
		Fermentable.Type type = fermentableAddition.getFermentable().getType();
		if (type == Fermentable.Type.GRAIN || type == Fermentable.Type.ADJUNCT)
		{
			// these are not soluble
			return new ColourUnit(0);
		}

		VolumeUnit fermVol;
		if (fermentableAddition.getQuantity() instanceof VolumeUnit)
		{
			fermVol = (VolumeUnit)fermentableAddition.getQuantity();
		}
		else if (fermentableAddition.getQuantity() instanceof WeightUnit)
		{
			// assume a 1kg to 1l conversion
			fermVol = new VolumeUnit(fermentableAddition.getQuantity().get(Quantity.Unit.KILOGRAMS), Quantity.Unit.LITRES);
		}
		else
		{
			throw new BrewdayException("Invalid: "+fermentableAddition.getQuantity());
		}

		return calcColourWithVolumeChange(
			fermVol,
			fermentableAddition.getFermentable().getColour(),
			volume);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the bitternmess provided by just dissolving the given fermentable
	 * in the given volume of fluid. This only works for Fermentables of type
	 * EXTRACT and a non zero ibuGalPerLb property.
	 * @return The additional bitterness.
	 */
	public static BitternessUnit calcSolubleFermentableAdditionBitternessContribution(
		FermentableAddition fermentableAddition,
		VolumeUnit volume)
	{
		Fermentable.Type type = fermentableAddition.getFermentable().getType();
		if (type != Fermentable.Type.LIQUID_EXTRACT && type != Fermentable.Type.DRY_EXTRACT)
		{
			// no IBU provided
			return new BitternessUnit(0);
		}

		double ibuGalPerLb = fermentableAddition.getFermentable().getIbuGalPerLb();
		if (ibuGalPerLb <= 0)
		{
			// no IBU provided
			return new BitternessUnit(0);
		}

		double amountInLbs = fermentableAddition.getQuantity().get(Quantity.Unit.POUNDS);

		// todo: this is based on a 60-minute boil; should be adjusting for boil time
		// source: BeerXML spec
		return new BitternessUnit(amountInLbs * ibuGalPerLb / volume.get(Quantity.Unit.US_GALLON));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://www.howtobrew.com/book/section-2/what-is-malted-grain/extraction-and-maximum-yield
	 * @param yield
	 * 	the grain yield in %
	 * @return
	 * 	the extract potential in ppg
	 */
	public static double calcExtractPotentialFromYield(PercentageUnit yield)
	{
		// Extract potential in USA units:
		// GU that can be achieved with 1.00 pound (455 g) of malt mashed in 1.00 gallon (3.78 L) of water.
		// source: https://byo.com/article/understanding-malt-spec-sheets-advanced-brewing/

		return 46 * yield.get(Quantity.Unit.PERCENTAGE);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://beersmith.com/blog/2010/09/07/apparent-and-real-attenuation-for-beer-brewers-part-1/
	 * @param start
	 * 	The starting gravity
	 * @param end
	 * 	The final gravity
	 * @return
	 * 	The % attenuation
	 */
	public static double calcAttenuation(DensityUnit start, DensityUnit end)
	{
		double sgStart = start.get(Quantity.Unit.SPECIFIC_GRAVITY);
		double sgEnd = end.get(Quantity.Unit.SPECIFIC_GRAVITY);

		return (sgStart - sgEnd) / (sgStart - 1D);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://braukaiser.com/wiki/index.php/Accurately_Calculating_Sugar_Additions_for_Carbonation
	 * See also: https://byo.com/article/master-the-action-carbonation/
	 *
	 * @param inputVolume
	 * 	The volume to be carbonated
	 * @param priming
	 * 	The nature and quantity of the substance used for priming
	 * @return
	 * 	The carbonation of the beer volume, in volumes CO2
	 */
	public static CarbonationUnit calcCarbonation(
		VolumeUnit inputVolume,
		FermentableAddition priming)
	{
		Fermentable fermentable = priming.getFermentable();

		if (fermentable.getType() == Fermentable.Type.GRAIN ||
			fermentable.getType() == Fermentable.Type.ADJUNCT)
		{
			// these are not fermentable without modification; zero carbonation
			return new CarbonationUnit(0);
		}

		WeightUnit weight = (WeightUnit)priming.getQuantity();
		double yield = fermentable.getYield().get(Quantity.Unit.PERCENTAGE);

		// Each gram of fermentable extract is fermented into equal parts (by weight)
		// of alcohol and CO2 (this is not exactly true, but close enough for this calculation).

		double gramsPerLitre = 0.5D * yield * weight.get(Quantity.Unit.GRAMS)
			/ inputVolume.get(Quantity.Unit.LITRES);

		boolean estimated = inputVolume.isEstimated();

		return new CarbonationUnit(gramsPerLitre, Quantity.Unit.GRAMS_PER_LITRE, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://braukaiser.com/wiki/index.php/Accurately_Calculating_Sugar_Additions_for_Carbonation
	 * See also: https://byo.com/article/master-the-action-carbonation/
	 */
	public static FermentableAddition calcPrimingSugarAmount(
		VolumeUnit inputVolume,
		Fermentable primingSubstance,
		CarbonationUnit targetCarb)
	{
		double v = inputVolume.get(Quantity.Unit.LITRES);
		double c = targetCarb.get(Quantity.Unit.GRAMS_PER_LITRE);
		double y = primingSubstance.getYield().get(Quantity.Unit.PERCENTAGE);

		// Each gram of fermentable extract is fermented into equal parts (by weight)
		// of alcohol and CO2 (this is not exactly true, but close enough for this calculation).

		double g = (v * c) / (0.5 * y);

		return new FermentableAddition(primingSubstance, new WeightUnit(g, Quantity.Unit.GRAMS), new TimeUnit(0));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Source: http://braukaiser.com/documents/CO2_content_metric.pdf
	 *
	 * @param temp the temp of the solution
	 * @param pressure the pressure under which the solution is, in kPa
	 */
	public static CarbonationUnit calcEquilibriumCo2(
		TemperatureUnit temp,
		PressureUnit pressure)
	{
		double tBeer = temp.get(Quantity.Unit.KELVIN);
		double gramsPerLitre = (pressure.get(Quantity.Unit.BAR))
			* Math.pow(2.71828182845904, -10.73797 + (2617.25 / tBeer))
			* 10;

		boolean estimated = temp.isEstimated() || pressure.isEstimated();

		return new CarbonationUnit(gramsPerLitre, Quantity.Unit.GRAMS_PER_LITRE, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Dilutes the given Volume with the given water addition and returns a
	 * new Volume representing the mixture.
	 */
	public static Volume dilute(Volume input, WaterAddition waterAddition, String outputVolumeName)
	{
		VolumeUnit volumeOut = new VolumeUnit(input.getVolume());
		volumeOut.add(waterAddition.getVolume());

		TemperatureUnit tempOut = calcCombinedTemperature(
			input.getVolume(),
			input.getTemperature(),
			waterAddition.getVolume(),
			waterAddition.getTemperature());

		DensityUnit gravityOut = calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		PercentageUnit abvOut = calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// assuming the water is at zero SRM and zero IBU

		ColourUnit colourOut = calcColourWithVolumeChange(
			input.getVolume(),
			input.getColour(),
			volumeOut);
		BitternessUnit bitternessOut =
			calcBitternessWithVolumeChange(
				input.getVolume(),
				input.getBitterness(),
				volumeOut);

		return new Volume(
			outputVolumeName,
			input.getType(),
			volumeOut,
			tempOut,
			input.getFermentability(),
			gravityOut,
			abvOut,
			colourOut,
			bitternessOut);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://braukaiser.com/wiki/index.php/Decoction_Mashing
	 * @return
	 * 	The volume that needs to be decocted to hit a certain mash temp
	 */
	public static VolumeUnit calcDecoctionVolume(
		VolumeUnit mashVolume,
		TemperatureUnit startTemp,
		TemperatureUnit targetTemp)
	{
		// decoction volume = total mash volume * (target temp - start temp) / (boil temp - start temp)

		double mashVolLitres = mashVolume.get(Quantity.Unit.LITRES);
		double ratio =
			(targetTemp.get(Quantity.Unit.CELSIUS) - startTemp.get(Quantity.Unit.CELSIUS)) /
				(100 - startTemp.get(Quantity.Unit.CELSIUS));

		return new VolumeUnit(mashVolLitres * ratio, Quantity.Unit.LITRES);
	}

}
