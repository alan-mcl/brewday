/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;

import static mclachlan.brewday.math.Quantity.Unit.*;

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
		System.out.println(temp + ": " + limit);
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
				Quantity.Unit.POUNDS,
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
				Quantity.Unit.POUNDS,
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
		testGrain1.setMoisture(new PercentageUnit(.02D));
		Fermentable testGrain2 = new Fermentable();
		testGrain2.setYield(new PercentageUnit(.8D));
		testGrain2.setMoisture(new PercentageUnit(.02D));

		ArrayList<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(testGrain1, new WeightUnit(5, KILOGRAMS), GRAMS, new TimeUnit(3600D)));
//		grainBill.add(new FermentableAddition(testGrain2, new WeightUnit(5, KILOGRAMS), GRAMS, new TimeUnit(3600D)));

		WeightUnit totalGrainWeight = Equations.calcTotalGrainWeight(grainBill);

		WaterAddition mashWater = new WaterAddition();
		mashWater.setVolume(new VolumeUnit(30, LITRES));

		double mashEfficiency = .7D;

		System.out.println("totalGrainWeight = [" + totalGrainWeight.describe(GRAMS) + "]");
		VolumeUnit volumeOutMl = Equations.calcWortVolume(grainBill, mashWater.getVolume(), 1);

		System.out.println("volumeOutMl = [" + volumeOutMl.describe(Quantity.Unit.MILLILITRES) + "]");

		DensityUnit gravityYield = Equations.calcMashExtractContentFromYield(
			grainBill, mashEfficiency, mashWater);
		System.out.println("gravityYield = [" + gravityYield.get(Quantity.Unit.SPECIFIC_GRAVITY) + "]");

		DensityUnit gravityPpg = Equations.calcMashExtractContentFromPppg(grainBill, mashEfficiency, volumeOutMl);
		System.out.println("gravityPpg = [" + gravityPpg + "]");
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcIbuMibu()
	{
		System.out.println("TestEquations.testCalcIbuMibu");

		Hop hop = new Hop();
		hop.setAlphaAcid(new PercentageUnit(0.10D));
		hop.setForm(Hop.Form.LEAF);
		HopAddition hopAdd = new HopAddition(
			hop,
			new WeightUnit(56.7, GRAMS),
			GRAMS,
			new TimeUnit(6, Quantity.Unit.MINUTES, false));

		DensityUnit gravity = new DensityUnit(1.060, Quantity.Unit.SPECIFIC_GRAVITY);
		VolumeUnit volume = new VolumeUnit(19.87, Quantity.Unit.LITRES);
		double kettleCm = Equations.estimateBoilKettleDiameterCm(volume);
		double openingCm = kettleCm;

		BitternessUnit lateHop = Equations.calcIbuMibu(
			hopAdd,
			new TimeUnit(6, Quantity.Unit.MINUTES),
			new TimeUnit(10, Quantity.Unit.MINUTES),
			gravity,
			volume,
			kettleCm,
			openingCm,
			1.0D);
		System.out.println("6 min boil + 10 min cool (expect ~26 IBU): " + lateHop.get(Quantity.Unit.IBU));

		HopAddition flameoutHop = new HopAddition(
			hop,
			new WeightUnit(56.7, GRAMS),
			GRAMS,
			new TimeUnit(0, Quantity.Unit.MINUTES, false));

		BitternessUnit flameout = Equations.calcIbuMibu(
			flameoutHop,
			new TimeUnit(0, Quantity.Unit.MINUTES),
			new TimeUnit(30, Quantity.Unit.MINUTES),
			gravity,
			volume,
			kettleCm,
			openingCm,
			1.0D);
		System.out.println("flameout + 30 min cool (expect ~23 IBU): " + flameout.get(Quantity.Unit.IBU));

		BitternessUnit boilOnly = Equations.calcIbuMibu(
			flameoutHop,
			new TimeUnit(0, Quantity.Unit.MINUTES),
			new TimeUnit(0, Quantity.Unit.MINUTES),
			gravity,
			volume,
			kettleCm,
			openingCm,
			1.0D);
		System.out.println("flameout boil-only (expect ~0 IBU): " + boilOnly.get(Quantity.Unit.IBU));
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcIbuTinseth()
	{
		System.out.println("TestEquations.testCalcIbuTinseth");

		Hop hop = new Hop();
		hop.setAlphaAcid(new PercentageUnit(.2D));
		hop.setForm(Hop.Form.PELLET_T90);
		HopAddition hopAdd = new HopAddition(hop, new WeightUnit(20), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES, false));

		for (double grav = 1.01D; grav < 1.08; grav = grav + .01)
		{
			BitternessUnit v = Equations.calcIbuTinseth(
				hopAdd,
				new TimeUnit(60, Quantity.Unit.MINUTES, false),
				new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
				new VolumeUnit(20000),
				1.0D);

			System.out.println(grav + ": " + v.get(Quantity.Unit.IBU));
		}

		// a test vs BeerSmith

		hop.setAlphaAcid(new PercentageUnit(.045D));
		hop.setForm(Hop.Form.LEAF);
		hopAdd = new HopAddition(hop, new WeightUnit(100, GRAMS), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES, false));

		double grav = 1.040;

		BitternessUnit b = Equations.calcIbuTinseth(
			hopAdd,
			new TimeUnit(60, Quantity.Unit.MINUTES, false),
			new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
			new VolumeUnit(20D, Quantity.Unit.LITRES),
			1.0D);

		System.out.println("b = " + b);
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcIbuRager()
	{
		System.out.println("TestEquations.testCalcIbuRager");

		Hop hop = new Hop();
		hop.setAlphaAcid(new PercentageUnit(.2D));
		hop.setForm(Hop.Form.PELLET_T90);
		HopAddition hopAdd = new HopAddition(hop, new WeightUnit(20), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES, false));

		for (double grav = 1.01D; grav < 1.08; grav = grav + .01)
		{
			BitternessUnit v = Equations.calcIbuRager(
				hopAdd,
				new TimeUnit(60, Quantity.Unit.MINUTES, false),
				new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
				new VolumeUnit(20000),
				1.0D);

			System.out.println(grav + ": " + v.get(Quantity.Unit.IBU));
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcIbuGaretz()
	{
		System.out.println("TestEquations.testCalcIbuGaretz");

		Hop hop = new Hop();
		hop.setAlphaAcid(new PercentageUnit(.2D));
		hop.setForm(Hop.Form.PELLET_T90);
		HopAddition hopAdd = new HopAddition(hop, new WeightUnit(20), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES, false));

		for (double grav = 1.01D; grav < 1.08; grav = grav + .01)
		{
			BitternessUnit v = Equations.calcIbuGaretz(
				hopAdd,
				new TimeUnit(60, Quantity.Unit.MINUTES, false),
				new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
				new VolumeUnit(20000),
				new VolumeUnit(18000),
				1.0D,
				0);

			System.out.println(grav + ": " + v.get(Quantity.Unit.IBU));
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcIbuDaniels()
	{
		System.out.println("TestEquations.testCalcIbuDaniels");

		Hop hop = new Hop();
		hop.setAlphaAcid(new PercentageUnit(.2D));
		hop.setForm(Hop.Form.PELLET_T90);
		HopAddition hopAdd = new HopAddition(hop, new WeightUnit(20), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES, false));

		for (double grav = 1.01D; grav < 1.08; grav = grav + .01)
		{
			BitternessUnit v = Equations.calcIbuDaniels(
				hopAdd,
				new TimeUnit(60, Quantity.Unit.MINUTES, false),
				new DensityUnit(grav, DensityUnit.Unit.SPECIFIC_GRAVITY),
				new VolumeUnit(20000),
				1.0D);

			System.out.println(grav + ": " + v.get(Quantity.Unit.IBU));
		}
	}

	/*-------------------------------------------------------------------------*/
	private static EquipmentProfile getTestEquipment()
	{
		return new EquipmentProfile(
			"test equipment",
			"test equipment",
			0,
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

		System.out.println("fermentableAddition.quantity(g) = " + fermentableAddition.getQuantity().get(GRAMS));
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcMashPhEzWater()
	{
		System.out.println("TestEquations.testCalcMashPhEzWater");

		Fermentable ferm = new Fermentable();
		ferm.setDistilledWaterPh(new PhUnit(5.7));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			ferm, new WeightUnit(5, Quantity.Unit.KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES)));

		Water water = new Water();
		water.setCalcium(new PpmUnit(1));
		water.setMagnesium(new PpmUnit(2));
		water.setSodium(new PpmUnit(3));
		water.setChloride(new PpmUnit(4));
		water.setSulfate(new PpmUnit(5));
		water.setBicarbonate(new PpmUnit(6));

		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, Quantity.Unit.LITRES), LITRES,
			new TemperatureUnit(70, Quantity.Unit.CELSIUS),
			new TimeUnit(60, Quantity.Unit.MINUTES));

		List<MiscAddition> miscAdditions = new ArrayList<>();

		PhUnit phUnit = Equations.calcMashPhEzWater(waterAddition, grainBill, miscAdditions);

		System.out.println("phUnit = " + phUnit);
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcAcidAdditionEzWater()
	{
		System.out.println("TestEquations.testCalcAcidAdditionEzWater");

		Fermentable ferm = new Fermentable();
		ferm.setDistilledWaterPh(new PhUnit(5.7));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			ferm, new WeightUnit(5, Quantity.Unit.KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES)));

		Water water = new Water();
		water.setCalcium(new PpmUnit(1));
		water.setMagnesium(new PpmUnit(2));
		water.setSodium(new PpmUnit(3));
		water.setChloride(new PpmUnit(4));
		water.setSulfate(new PpmUnit(5));
		water.setBicarbonate(new PpmUnit(6));
		water.setPh(new PhUnit(7));

		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, Quantity.Unit.LITRES), LITRES,
			new TemperatureUnit(70, Quantity.Unit.CELSIUS),
			new TimeUnit(60, Quantity.Unit.MINUTES));

		List<MiscAddition> miscAdditions = new ArrayList<>();

		Misc acid = new Misc("lactic acid 88%");
		acid.setWaterAdditionFormula(Misc.WaterAdditionFormula.LACTIC_ACID);
		acid.setAcidContent(new PercentageUnit(.88));


		VolumeUnit volumeUnit = Equations.calcMashAcidAdditionEzWater(acid, new PhUnit(5.2), waterAddition, grainBill, miscAdditions);

		System.out.println("acid addition (ml) = " + volumeUnit.get(MILLILITRES));
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcCombinedWaterProfile()
	{
		System.out.println("TestEquations.testCalcCombinedWaterProfile");

		Water w1 = new Water("w1");
		w1.setBicarbonate(new PpmUnit(100));
		w1.setSodium(new PpmUnit(100));
		w1.setCalcium(new PpmUnit(100));
		w1.setMagnesium(new PpmUnit(100));
		w1.setSulfate(new PpmUnit(100));
		w1.setChloride(new PpmUnit(100));
		w1.setPh(new PhUnit(5));

		Water w2 = new Water("w2");
		w2.setBicarbonate(new PpmUnit(200));
		w2.setSodium(new PpmUnit(200));
		w2.setCalcium(new PpmUnit(200));
		w2.setMagnesium(new PpmUnit(200));
		w2.setSulfate(new PpmUnit(200));
		w2.setChloride(new PpmUnit(200));
		w2.setPh(new PhUnit(7));

		Water water = Equations.calcCombinedWaterProfile(
			w1, new VolumeUnit(20, Quantity.Unit.LITRES),
			w2, new VolumeUnit(10, Quantity.Unit.LITRES));

		System.out.println("water = " + water.getBicarbonate());
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcAlkalinity()
	{
		System.out.println("TestEquations.testCalcAlkalinity");

		Water w1 = new Water("w1");
		w1.setBicarbonate(new PpmUnit(100));
		w1.setSodium(new PpmUnit(0));
		w1.setCalcium(new PpmUnit(0));
		w1.setMagnesium(new PpmUnit(0));
		w1.setSulfate(new PpmUnit(0));
		w1.setChloride(new PpmUnit(0));
		w1.setPh(new PhUnit(7));

		PpmUnit ppmUnit = Equations.calcAlkalinity(w1);
		System.out.println("Alkalinity (as ppm CaCo3)= " + ppmUnit);

		ppmUnit = Equations.calcAlkalinitySimple(w1);
		System.out.println("Alkalinity (simple)= " + ppmUnit);
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcMashPhMpH()
	{
		System.out.println("TestEquations.testCalcMashPhMpH");

		Fermentable ferm = new Fermentable();
		ferm.setDistilledWaterPh(new PhUnit(5.72));
		ferm.setBufferingCapacity(new ArbitraryPhysicalQuantity(45.5, MEQ_PER_KILOGRAM));

		Fermentable acidMalt = new Fermentable();
		acidMalt.setDistilledWaterPh(new PhUnit(5.72));
		acidMalt.setBufferingCapacity(new ArbitraryPhysicalQuantity(45.5, MEQ_PER_KILOGRAM));
		acidMalt.setLacticAcidContent(new PercentageUnit(.03));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			ferm, new WeightUnit(5, Quantity.Unit.KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES)));
		grainBill.add(new FermentableAddition(
					acidMalt, new WeightUnit(5, OUNCES), OUNCES,
					new TimeUnit(60, Quantity.Unit.MINUTES)));

		Water water = new Water();
		water.setCalcium(new PpmUnit(1));
		water.setMagnesium(new PpmUnit(2));
		water.setSodium(new PpmUnit(3));
		water.setChloride(new PpmUnit(4));
		water.setSulfate(new PpmUnit(5));
		water.setBicarbonate(new PpmUnit(0));
		water.setPh(new PhUnit(7));

		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, Quantity.Unit.LITRES), LITRES,
			new TemperatureUnit(70, Quantity.Unit.CELSIUS),
			new TimeUnit(60, Quantity.Unit.MINUTES));

		List<MiscAddition> miscAdditions = new ArrayList<>();

		Misc acid = new Misc("acid");
		acid.setWaterAdditionFormula(Misc.WaterAdditionFormula.PHOSPHORIC_ACID);
		acid.setAcidContent(new PercentageUnit(.10));

		miscAdditions.add(new MiscAddition(acid, new VolumeUnit(5, MILLILITRES), MILLILITRES, new TimeUnit(0)));

		PhUnit phUnit = Equations.calcMashPhMpH(waterAddition, grainBill, miscAdditions);

		System.out.println("phUnit = " + phUnit);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Kaiser Water: 5 kg pale malt, 20 L strike, moderate alkalinity.
	 * Hand-check against Kaiser_water_calculator_US_units.xls when available.
	 * Expected ~5.76 from paper linear model (grist pH 5.72, RA ~0.6 mEq/L, R=4).
	 */
	private static void testCalcMashPhKaiserWaterPale()
	{
		System.out.println("TestEquations.testCalcMashPhKaiserWaterPale");

		Fermentable pils = new Fermentable("Pilsner");
		pils.setType(Fermentable.Type.GRAIN);
		pils.setDistilledWaterPh(new PhUnit(5.72));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			pils, new WeightUnit(5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));

		Water water = new Water();
		water.setBicarbonate(new PpmUnit(100));
		water.setCalcium(new PpmUnit(50));
		water.setMagnesium(new PpmUnit(10));

		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, LITRES), LITRES,
			new TemperatureUnit(70, CELSIUS),
			new TimeUnit(60, MINUTES));

		PhUnit phUnit = Equations.calcMashPhKaiserWater(
			waterAddition, grainBill, new ArrayList<>());

		System.out.println("phUnit = " + phUnit);
		assertPhNear("pale mash pH", phUnit.get(PH), 5.76, 0.08);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Kaiser Water: base plus crystal specialty (10% crystal 60L by weight).
	 * Expected grist pH lower than pale-only; mash pH ~5.65 with same water as pale test.
	 */
	private static void testCalcMashPhKaiserWaterSpecialty()
	{
		System.out.println("TestEquations.testCalcMashPhKaiserWaterSpecialty");

		Fermentable pils = new Fermentable("Pilsner");
		pils.setType(Fermentable.Type.GRAIN);
		pils.setDistilledWaterPh(new PhUnit(5.72));

		Fermentable crystal = new Fermentable("Crystal 60L");
		crystal.setType(Fermentable.Type.GRAIN);
		crystal.setColour(new ColourUnit(60, SRM));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			pils, new WeightUnit(4.5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));
		grainBill.add(new FermentableAddition(
			crystal, new WeightUnit(0.5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));

		Water water = new Water();
		water.setBicarbonate(new PpmUnit(100));
		water.setCalcium(new PpmUnit(50));
		water.setMagnesium(new PpmUnit(10));

		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, LITRES), LITRES,
			new TemperatureUnit(70, CELSIUS),
			new TimeUnit(60, MINUTES));

		PhUnit phUnit = Equations.calcMashPhKaiserWater(
			waterAddition, grainBill, new ArrayList<>());

		System.out.println("phUnit = " + phUnit);
		assertPhNear("specialty mash pH", phUnit.get(PH), 5.65, 0.08);
		assertTrue("specialty lowers pH vs pale",
			phUnit.get(PH) < testCalcMashPhKaiserWaterPaleValue());
	}

	/*-------------------------------------------------------------------------*/

	private static double testCalcMashPhKaiserWaterPaleValue()
	{
		Fermentable pils = new Fermentable("Pilsner");
		pils.setType(Fermentable.Type.GRAIN);
		pils.setDistilledWaterPh(new PhUnit(5.72));
		List<FermentableAddition> grainBill = List.of(new FermentableAddition(
			pils, new WeightUnit(5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));
		Water water = new Water();
		water.setBicarbonate(new PpmUnit(100));
		water.setCalcium(new PpmUnit(50));
		water.setMagnesium(new PpmUnit(10));
		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, LITRES), LITRES,
			new TemperatureUnit(70, CELSIUS),
			new TimeUnit(60, MINUTES));
		return Equations.calcMashPhKaiserWater(
			waterAddition, grainBill, new ArrayList<>()).get(PH);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Kaiser Water: pale grist with phosphoric acid misc addition (MpH fixture analogue).
	 */
	private static void testCalcMashPhKaiserWaterWithAcid()
	{
		System.out.println("TestEquations.testCalcMashPhKaiserWaterWithAcid");

		Fermentable ferm = new Fermentable("Pilsner");
		ferm.setType(Fermentable.Type.GRAIN);
		ferm.setDistilledWaterPh(new PhUnit(5.72));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			ferm, new WeightUnit(5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));

		Water water = new Water();
		water.setBicarbonate(new PpmUnit(50));
		water.setCalcium(new PpmUnit(20));
		water.setMagnesium(new PpmUnit(5));

		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, LITRES), LITRES,
			new TemperatureUnit(70, CELSIUS),
			new TimeUnit(60, MINUTES));

		Misc acid = new Misc("phosphoric acid");
		acid.setWaterAdditionFormula(Misc.WaterAdditionFormula.PHOSPHORIC_ACID);
		acid.setAcidContent(new PercentageUnit(0.10));

		List<MiscAddition> miscAdditions = new ArrayList<>();
		miscAdditions.add(new MiscAddition(
			acid, new VolumeUnit(5, MILLILITRES), MILLILITRES, new TimeUnit(0)));

		PhUnit withoutAcid = Equations.calcMashPhKaiserWater(
			waterAddition, grainBill, new ArrayList<>());
		PhUnit phUnit = Equations.calcMashPhKaiserWater(
			waterAddition, grainBill, miscAdditions);

		System.out.println("phUnit (no acid) = " + withoutAcid);
		System.out.println("phUnit (with acid) = " + phUnit);
		assertTrue("phosphoric acid lowers mash pH",
			phUnit.get(PH) < withoutAcid.get(PH));

		VolumeUnit acidVol = Equations.calcMashAcidAdditionKaiserWater(
			acid, new PhUnit(5.2), waterAddition, grainBill, new ArrayList<>());
		System.out.println("acid addition to reach 5.2 (ml) = " + acidVol.get(MILLILITRES));
		assertTrue("acid addition volume is positive", acidVol.get(MILLILITRES) > 0);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Z pH (Water Book): 5 kg pale malt, 20 L strike, moderate alkalinity.
	 */
	private static void testCalcMashPhZPhPale()
	{
		System.out.println("TestEquations.testCalcMashPhZPhPale");

		// todo
	}

	/*-------------------------------------------------------------------------*/

	private static void testCalcMashPhZPhWaterContribution()
	{
		System.out.println("TestEquations.testCalcMashPhZPhWaterContribution");

		// todo
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Z pH: base plus crystal specialty (10% crystal 60L by weight).
	 */
	private static void testCalcMashPhZPhSpecialty()
	{
		System.out.println("TestEquations.testCalcMashPhZPhSpecialty");

		Fermentable pils = new Fermentable("Pilsner");
		pils.setType(Fermentable.Type.GRAIN);
		pils.setDistilledWaterPh(new PhUnit(5.72));
		pils.setBufferingCapacity(new ArbitraryPhysicalQuantity(45.5, MEQ_PER_KILOGRAM));

		Fermentable crystal = new Fermentable("Crystal 60L");
		crystal.setType(Fermentable.Type.GRAIN);
		crystal.setColour(new ColourUnit(60, SRM));
		crystal.setDistilledWaterPh(new PhUnit(4.76));
		crystal.setBufferingCapacity(new ArbitraryPhysicalQuantity(71.7, MEQ_PER_KILOGRAM));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			pils, new WeightUnit(4.5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));
		grainBill.add(new FermentableAddition(
			crystal, new WeightUnit(0.5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));

		WaterAddition waterAddition = buildZPhPaleWaterAddition();

		PhUnit phUnit = Equations.calcMashPhZPh(
			waterAddition, grainBill, new ArrayList<>());

		System.out.println("phUnit = " + phUnit);
		assertTrue("specialty lowers pH vs pale",
			phUnit.get(PH) < testCalcMashPhZPhPaleValue());
	}

	/*-------------------------------------------------------------------------*/

	private static double testCalcMashPhZPhPaleValue()
	{
		return Equations.calcMashPhZPh(
			buildZPhPaleWaterAddition(), buildZPhPaleGrainBill(), new ArrayList<>()).get(PH);
	}

	/*-------------------------------------------------------------------------*/

	private static List<FermentableAddition> buildZPhPaleGrainBill()
	{
		Fermentable pils = new Fermentable("Pilsner");
		pils.setType(Fermentable.Type.GRAIN);
		pils.setDistilledWaterPh(new PhUnit(5.72));
		pils.setBufferingCapacity(new ArbitraryPhysicalQuantity(45.5, MEQ_PER_KILOGRAM));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			pils, new WeightUnit(5, KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, MINUTES)));
		return grainBill;
	}

	/*-------------------------------------------------------------------------*/

	private static WaterAddition buildZPhPaleWaterAddition()
	{
		Water water = new Water();
		water.setBicarbonate(new PpmUnit(100));
		water.setCalcium(new PpmUnit(50));
		water.setMagnesium(new PpmUnit(10));

		return new WaterAddition(water,
			new VolumeUnit(20, LITRES), LITRES,
			new TemperatureUnit(70, CELSIUS),
			new TimeUnit(60, MINUTES));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Z pH: pale grist with phosphoric acid misc addition.
	 */
	private static void testCalcMashPhZPhWithAcid()
	{
		System.out.println("TestEquations.testCalcMashPhZPhWithAcid");

		List<FermentableAddition> grainBill = buildZPhPaleGrainBill();
		WaterAddition waterAddition = buildZPhPaleWaterAddition();

		Misc acid = new Misc("phosphoric acid");
		acid.setWaterAdditionFormula(Misc.WaterAdditionFormula.PHOSPHORIC_ACID);
		acid.setAcidContent(new PercentageUnit(0.10));

		List<MiscAddition> miscAdditions = new ArrayList<>();
		miscAdditions.add(new MiscAddition(
			acid, new VolumeUnit(5, MILLILITRES), MILLILITRES, new TimeUnit(0)));

		PhUnit withoutAcid = Equations.calcMashPhZPh(
			waterAddition, grainBill, new ArrayList<>());
		PhUnit phUnit = Equations.calcMashPhZPh(
			waterAddition, grainBill, miscAdditions);

		System.out.println("phUnit (no acid) = " + withoutAcid);
		System.out.println("phUnit (with acid) = " + phUnit);
		assertTrue("phosphoric acid lowers mash pH",
			phUnit.get(PH) < withoutAcid.get(PH));

		VolumeUnit acidVol = Equations.calcMashAcidAdditionZPh(
			acid, new PhUnit(5.2), waterAddition, grainBill, new ArrayList<>());
		System.out.println("acid addition to reach 5.2 (ml) = " + acidVol.get(MILLILITRES));
		assertTrue("acid addition volume is positive", acidVol.get(MILLILITRES) > 0);
	}

	/*-------------------------------------------------------------------------*/

	private static void assertPhNear(String label, double actual, double expected, double tolerance)
	{
		if (Math.abs(actual - expected) > tolerance)
		{
			throw new RuntimeException(label + ": expected ~" + expected
				+ " but got " + actual + " (tolerance " + tolerance + ")");
		}
	}

	/*-------------------------------------------------------------------------*/

	private static void assertTrue(String label, boolean condition)
	{
		if (!condition)
		{
			throw new RuntimeException(label + ": assertion failed");
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void testCalcAcidAdditionMpH()
	{
		Fermentable ferm = new Fermentable();
		ferm.setDistilledWaterPh(new PhUnit(5.72));
		ferm.setBufferingCapacity(new ArbitraryPhysicalQuantity(45.5, MEQ_PER_KILOGRAM));

		List<FermentableAddition> grainBill = new ArrayList<>();
		grainBill.add(new FermentableAddition(
			ferm, new WeightUnit(5, Quantity.Unit.KILOGRAMS), KILOGRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES)));

		Water water = new Water();
		water.setCalcium(new PpmUnit(1));
		water.setMagnesium(new PpmUnit(2));
		water.setSodium(new PpmUnit(3));
		water.setChloride(new PpmUnit(4));
		water.setSulfate(new PpmUnit(5));
		water.setBicarbonate(new PpmUnit(0));
		water.setPh(new PhUnit(7));

		WaterAddition waterAddition = new WaterAddition(water,
			new VolumeUnit(20, Quantity.Unit.LITRES), LITRES,
			new TemperatureUnit(70, Quantity.Unit.CELSIUS),
			new TimeUnit(60, Quantity.Unit.MINUTES));

		Misc acid = new Misc("acid");
		acid.setWaterAdditionFormula(Misc.WaterAdditionFormula.LACTIC_ACID);
		acid.setAcidContent(new PercentageUnit(.80));

		VolumeUnit volumeUnit = Equations.calcMashAcidAdditionMpH(acid, new PhUnit(5.2), waterAddition, grainBill, new ArrayList<>());

		System.out.println("acid addition (ml) = " + volumeUnit.get(MILLILITRES));
	}

	/*-------------------------------------------------------------------------*/
	public static void testNewtonianCooling()
	{
		System.out.println("TestEquations.testNewtonianCooling");

		TemperatureUnit t0 = new TemperatureUnit(100, CELSIUS);
		TemperatureUnit ta = new TemperatureUnit(20, CELSIUS);
		double k = 0.1D;

		TemperatureUnit atZero = Equations.calcNewtonianCoolingTemperature(t0, ta, k, 0D);
		boolean t0AtStart = Math.abs(atZero.get(CELSIUS) - 100D) < 0.01D;

		TemperatureUnit after1h = Equations.calcNewtonianCoolingEndTemperature(
			t0, ta, k, new TimeUnit(1, HOURS, false));
		boolean endOk = Math.abs(after1h.get(CELSIUS) - 92.4D) < 0.5D;

		TemperatureUnit mid = Equations.calcNewtonianCoolingTemperature(t0, ta, k, 0.5D);
		boolean midBetween = mid.get(CELSIUS) < t0.get(CELSIUS)
			&& mid.get(CELSIUS) > ta.get(CELSIUS);

		System.out.printf(
			"T(0)=T0 %s T(1h)~92.4C %s mid between %s%n",
			t0AtStart,
			endOk,
			midBetween);
	}

	/*-------------------------------------------------------------------------*/
	public static void testHopStandNewtonianCooling()
	{
		System.out.println("TestEquations.testHopStandNewtonianCooling");

		TemperatureUnit wort = new TemperatureUnit(95, CELSIUS);
		TemperatureUnit ambient = new TemperatureUnit(20, CELSIUS);

		double utilEarly = hopStandUtilizationFactorAtElapsedMin(wort, ambient, 0.1D, 0D);
		double utilLate = hopStandUtilizationFactorAtElapsedMin(wort, ambient, 0.1D, 25D);
		boolean utilDecreases = utilLate < utilEarly;

		System.out.printf("hop-stand util early=%.6f late=%.6f decreases=%s%n",
			utilEarly,
			utilLate,
			utilDecreases);
	}

	/*-------------------------------------------------------------------------*/
	private static double relativeUtilizationFactorKelvin(double tempK)
	{
		return 2.39E11 * Math.exp(-9773.0 / tempK);
	}

	/*-------------------------------------------------------------------------*/
	private static double hopStandUtilizationFactorAtElapsedMin(
		TemperatureUnit wort,
		TemperatureUnit ambient,
		double k,
		double standElapsedMin)
	{
		double tempK = Equations.calcNewtonianCoolingTemperature(
			wort,
			ambient,
			k,
			standElapsedMin / 60D).get(KELVIN);
		return relativeUtilizationFactorKelvin(tempK);
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		testNewtonianCooling();
		testHopStandNewtonianCooling();

		Database.getInstance().loadAll();

//		testGetCombinedColour();
//		testGetWortAttenuationLimit();
//		testCalcMashExtractContent();
//		testCalcSolubleFermentableAdditionGravity();
//		testCalcSolubleFermentableBitternessContribution();
//		testCalcIbuTinseth();
//		testCalcIbuRager();
//		testCalcIbuGaretz();
//		testCalcIbuDaniels();
//		testCalcIbuMibu();
//		testCombinedLinearInterpolation();
//		testCalcHeatingTime();
//		testCalcPrimingSugarAmount();
//		testCalcMashPhEzWater();
//		testCalcAcidAdditionEzWater();
//		testCalcCombinedWaterProfile();
//		testCalcAlkalinity();
//		testCalcMashPhMpH();
//		testCalcAcidAdditionMpH();
		testCalcMashPhKaiserWaterPale();
		testCalcMashPhKaiserWaterSpecialty();
		testCalcMashPhKaiserWaterWithAcid();
		testCalcMashPhZPhPale();
		testCalcMashPhZPhWaterContribution();
		testCalcMashPhZPhSpecialty();
		testCalcMashPhZPhWithAcid();
	}
}
