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

package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class TestEquations
{
	/*-------------------------------------------------------------------------*/
	public static void testGetCombinedColour()
	{
		VolumeUnit v1 = new VolumeUnit(10, Quantity.Unit.LITRES);
		ColourUnit c1 = new ColourUnit(10, Quantity.Unit.SRM);
		VolumeUnit v2 = new VolumeUnit(10, Quantity.Unit.LITRES);
		ColourUnit c2 = new ColourUnit(10, Quantity.Unit.SRM);
		ColourUnit c = Equations.calcCombinedColour(v1, c1, v2, c2);
		System.out.println("c = [" + c + "]");

		v1 = new VolumeUnit(10, Quantity.Unit.LITRES);
		c1 = new ColourUnit(10, Quantity.Unit.SRM);
		v2 = new VolumeUnit(20, Quantity.Unit.LITRES);
		c2 = new ColourUnit(20, Quantity.Unit.SRM);
		c = Equations.calcCombinedColour(v1, c1, v2, c2);
		System.out.println("c = [" + c + "]");
	}

	/*-------------------------------------------------------------------------*/
	public static void testGetWortAttenuationLimit()
	{
		testMashTemp(new TemperatureUnit(58));
		testMashTemp(new TemperatureUnit(67.5));
		testMashTemp(new TemperatureUnit(70));
		testMashTemp(new TemperatureUnit(80));
	}

	/*-------------------------------------------------------------------------*/
	private static void testMashTemp(TemperatureUnit temp)
	{
		PercentageUnit limit = Equations.getWortAttenuationLimit(temp);
		System.out.println(temp+": "+limit);
	}

	/*-------------------------------------------------------------------------*/
	public static void testCalcSolubleFermentableAdditionGravity()
	{
		System.out.println("TestEquations.testCalcSolubleFermentableAdditionGravity");

		Fermentable fermentable = new Fermentable();
		fermentable.setYield(new PercentageUnit(1D));

		FermentableAddition fermentableAddition =
			new FermentableAddition(
				fermentable,
				new WeightUnit(1D, Quantity.Unit.POUNDS, false),
				new TimeUnit(3600D));

		VolumeUnit volumeUnit = new VolumeUnit(1D, Quantity.Unit.US_GALLON);

		DensityUnit densityUnit = Equations.calcSteepedFermentableAdditionGravity(fermentableAddition, volumeUnit);

		System.out.println("densityUnit = [" + densityUnit + "]");
	}

	/*-------------------------------------------------------------------------*/
	public static void testCalcSolubleFermentableBitternessContribution()
	{
		System.out.println("TestEquations.testCalcSolubleFermentableBitternessContribution");

		Fermentable fermentable = new Fermentable();
		fermentable.setType(Fermentable.Type.LIQUID_EXTRACT);
		fermentable.setIbuGalPerLb(33);

		FermentableAddition fermentableAddition =
			new FermentableAddition(
				fermentable,
				new WeightUnit(1D, Quantity.Unit.POUNDS, false),
				new TimeUnit(3600D));

		VolumeUnit volumeUnit = new VolumeUnit(1D, Quantity.Unit.US_GALLON);

		BitternessUnit ibu = Equations.calcSolubleFermentableAdditionBitternessContribution(fermentableAddition, volumeUnit);

		System.out.println("ibu = [" + ibu + "]");
	}

	/*-------------------------------------------------------------------------*/
	public static void testCalcMashExtractContent()
	{
		System.out.println("TestEquations.testCalcMashExtractContent");

		Fermentable testGrain1 = new Fermentable();
		testGrain1.setYield(new PercentageUnit(.8D));
		Fermentable testGrain2 = new Fermentable();
		testGrain2.setYield(new PercentageUnit(.8D));

		ArrayList<IngredientAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(testGrain1, new WeightUnit(5000D), new TimeUnit(3600D)));
		grainBill.add(new FermentableAddition(testGrain2, new WeightUnit(5000D), new TimeUnit(3600D)));

		WeightUnit totalGrainWeight = Equations.getTotalGrainWeight(grainBill);

		WaterAddition mashWater = new WaterAddition();
		mashWater.setVolume(new VolumeUnit(30000D));

		double mashEfficiency = .7D;

		VolumeUnit volumeOutMl = Equations.calcWortVolume(totalGrainWeight, mashWater.getVolume());

		System.out.println("volumeOutMl = [" + volumeOutMl.get(Quantity.Unit.MILLILITRES) + "]");

		DensityUnit gravityYield = Equations.calcMashExtractContentFromYield(
			grainBill, mashEfficiency, mashWater);
		System.out.println("gravityYield = [" + gravityYield.get(Quantity.Unit.SPECIFIC_GRAVITY) + "]");

		DensityUnit gravityPpg = Equations.calcMashExtractContentFromPppg(grainBill, mashEfficiency, volumeOutMl);
		System.out.println("gravityPpg = [" + gravityPpg + "]");
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcHopStandIbu()
	{
		System.out.println("TestEquations.testCalcHopStandIbu");

		Hop hop = new Hop();
		hop.setAlphaAcid(new PercentageUnit(.2D));
		HopAddition hopAdd = new HopAddition(hop, new WeightUnit(20, Quantity.Unit.GRAMS),
			new TimeUnit(60, Quantity.Unit.MINUTES, false));

		BitternessUnit ibu = Equations.calcHopStandIbu(
			Arrays.asList(hopAdd),
			new DensityUnit(1.040, Quantity.Unit.SPECIFIC_GRAVITY),
			new VolumeUnit(20, Quantity.Unit.LITRES),
			new TimeUnit(60, Quantity.Unit.MINUTES),
			new TimeUnit(20, Quantity.Unit.MINUTES));

		System.out.println("ibu = [" + ibu + "]");
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcIbuTinseth()
	{
		System.out.println("TestEquations.testCalcIbuTinseth");

		Hop hop = new Hop();
		hop.setAlphaAcid(new PercentageUnit(.2D));
		HopAddition hopAdd = new HopAddition(hop, new WeightUnit(20),
			new TimeUnit(60, Quantity.Unit.MINUTES, false));

		for (double grav=1.01D; grav <1.08; grav = grav+.01)
		{
			BitternessUnit v = Equations.calcIbuTinseth(
				hopAdd,
				new TimeUnit(60, Quantity.Unit.MINUTES, false),
				new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
				new VolumeUnit(20000),
				1.0D);

			System.out.println(grav+": "+v.get(Quantity.Unit.IBU));
		}

	}

	/*-------------------------------------------------------------------------*/
	private static EquipmentProfile getTestEquipment()
	{
		return new EquipmentProfile(
			"test equipment",
			"test equipment",
			.7D,
			25000D,
			2000D,
			.3D,
			30000D,
			.08D,
			10D,
			1D,
			25000D,
			2000D,
			2000D);
	}

	/*-------------------------------------------------------------------------*/
	private static void testCombinedLinearInterpolation()
	{
		System.out.println("TestEquations.testCombinedLinearInterpolation");

		Quantity quantity = Equations.calcCombinedLinearInterpolation(
			new VolumeUnit(10, Quantity.Unit.LITRES),
			new BitternessUnit(50, Quantity.Unit.IBU),
			new VolumeUnit(10, Quantity.Unit.LITRES),
			new BitternessUnit(0, Quantity.Unit.IBU));

		System.out.println("IBU = " + quantity);
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcHeatingTime()
	{
		System.out.println("TestEquations.testCalcHeatingTime");

		TimeUnit timeUnit = Equations.calcHeatingTime(
			new VolumeUnit(30, Quantity.Unit.LITRES),
			new TemperatureUnit(50, Quantity.Unit.CELSIUS),
			new TemperatureUnit(100, Quantity.Unit.CELSIUS),
			new PowerUnit(5, Quantity.Unit.KILOWATT, false));

		System.out.println("heating time (m)= " + timeUnit.get(Quantity.Unit.MINUTES));
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcPrimingSugarAmount()
	{
		System.out.println("TestEquations.testCalcPrimingSugarAmount");

		Fermentable test = new Fermentable("test");
		test.setYield(new PercentageUnit(.99));

		FermentableAddition fermentableAddition = Equations.calcPrimingSugarAmount(
			new VolumeUnit(20, Quantity.Unit.LITRES),
			test,
			new CarbonationUnit(2.5));

		System.out.println("fermentableAddition.quantity(g) = " + fermentableAddition.getQuantity().get(Quantity.Unit.GRAMS));
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		testGetCombinedColour();
		testGetWortAttenuationLimit();
		testCalcMashExtractContent();
		testCalcSolubleFermentableAdditionGravity();
		testCalcSolubleFermentableBitternessContribution();
		testCalcIbuTinseth();
		testCalcHopStandIbu();
		testCombinedLinearInterpolation();
		testCalcHeatingTime();
		testCalcPrimingSugarAmount();
	}
}
