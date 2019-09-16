package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
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
		fermentable.setYield(1D);

		FermentableAddition fermentableAddition =
			new FermentableAddition(
				fermentable,
				new WeightUnit(1D, Quantity.Unit.POUNDS, false),
				new TimeUnit(3600D));

		VolumeUnit volumeUnit = new VolumeUnit(1D, Quantity.Unit.US_GALLON);

		DensityUnit densityUnit = Equations.calcSolubleFermentableAdditionGravity(fermentableAddition, volumeUnit);

		System.out.println("densityUnit = [" + densityUnit + "]");

	}

	/*-------------------------------------------------------------------------*/
	public static void testCalcMashExtractContent()
	{
		System.out.println("TestEquations.testCalcMashExtractContent");

		Fermentable testGrain1 = new Fermentable();
		testGrain1.setYield(.8D);
		Fermentable testGrain2 = new Fermentable();
		testGrain2.setYield(.8D);

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
			1D,
			25000D,
			2000D,
			2000D);
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		testGetCombinedColour();
		testGetWortAttenuationLimit();
		testCalcMashExtractContent();
		testCalcSolubleFermentableAdditionGravity();
	}
}
