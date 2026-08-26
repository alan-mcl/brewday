package mclachlan.brewday.process;

import java.util.ArrayList;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static mclachlan.brewday.math.Quantity.Unit.PPM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MashWaterCombinationTest
{
	private static final String MASH1_OUT = "mash1_out";
	private static final String MASH2_OUT = "mash2_out";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	private static Water waterWithCalcium(String name, double calciumPpm)
	{
		Water water = new Water(name);
		water.setCalcium(new PpmUnit(calciumPpm, false));
		water.setMagnesium(new PpmUnit(0, false));
		water.setSodium(new PpmUnit(0, false));
		water.setSulfate(new PpmUnit(0, false));
		water.setChloride(new PpmUnit(0, false));
		water.setBicarbonate(new PpmUnit(0, false));
		water.setPh(new PhUnit(7, false));
		return water;
	}

	@Test
	public void chainedMashStepsCombineWaterAndGrainBill()
	{
		Fermentable fermentable = Database.getInstance().getFermentables().values().iterator().next();

		Water water1 = waterWithCalcium("low calcium", 50);
		Water water2 = waterWithCalcium("high calcium", 150);

		TimeUnit mashTime = new TimeUnit(60, MINUTES, false);

		FermentableAddition grain1 = new FermentableAddition(
			fermentable,
			new WeightUnit(3, KILOGRAMS),
			KILOGRAMS,
			mashTime);
		WaterAddition strike1 = new WaterAddition(
			water1,
			new VolumeUnit(10, LITRES),
			LITRES,
			new TemperatureUnit(70, CELSIUS, false),
			mashTime);

		ArrayList<IngredientAddition> mash1Additions = new ArrayList<>();
		mash1Additions.add(grain1);
		mash1Additions.add(strike1);

		Mash mash1 = new Mash(
			"mash1",
			"",
			mash1Additions,
			null,
			MASH1_OUT,
			mashTime,
			new TemperatureUnit(20, CELSIUS));

		FermentableAddition grain2 = new FermentableAddition(
			fermentable,
			new WeightUnit(2, KILOGRAMS),
			KILOGRAMS,
			mashTime);
		WaterAddition strike2 = new WaterAddition(
			water2,
			new VolumeUnit(5, LITRES),
			LITRES,
			new TemperatureUnit(75, CELSIUS, false),
			mashTime);

		ArrayList<IngredientAddition> mash2Additions = new ArrayList<>();
		mash2Additions.add(grain2);
		mash2Additions.add(strike2);

		Mash mash2 = new Mash(
			"mash2",
			"",
			mash2Additions,
			MASH1_OUT,
			MASH2_OUT,
			mashTime,
			new TemperatureUnit(20, CELSIUS));

		Recipe recipe = new Recipe("MashWaterCombinationTest");
		recipe.setEquipmentProfile(
			Database.getInstance().getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE));
		recipe.getSteps().add(mash1);
		recipe.getSteps().add(mash2);

		recipe.run();

		assertTrue(recipe.getErrors().isEmpty());

		Volume mashOut = recipe.getVolumes().getVolume(MASH2_OUT);
		WaterAddition combinedWater =
			(WaterAddition)mashOut.getIngredientAddition(IngredientAddition.Type.WATER);

		double expectedCa = (50 * 10 + 150 * 5) / 15.0;
		assertEquals(expectedCa, combinedWater.getWater().getCalcium().get(PPM), 0.5);
		assertEquals(15, combinedWater.getVolume().get(LITRES), 0.1);

		assertEquals(2, mashOut.getIngredientAdditions(IngredientAddition.Type.FERMENTABLES).size());
	}
}
