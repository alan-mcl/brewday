package mclachlan.brewday.process;

import java.util.ArrayList;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Equations;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MashInfusionProcessTest
{
	private static final String MASH_OUT = "mash_out";
	private static final String INFUSION_OUT = "infusion_out";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void mashInfusionRestHitsTargetTemperature()
	{
		Fermentable fermentable = Database.getInstance().getFermentables().values().iterator().next();
		Water water = Database.getInstance().getWaters().values().iterator().next();
		TimeUnit mashTime = new TimeUnit(60, MINUTES, false);

		FermentableAddition grain = new FermentableAddition(
			fermentable,
			new WeightUnit(5, KILOGRAMS),
			KILOGRAMS,
			mashTime);

		TemperatureUnit firstRest = new TemperatureUnit(50, CELSIUS);
		WaterAddition strikeWaterForCalc = new WaterAddition(
			water,
			new VolumeUnit(15, LITRES),
			LITRES,
			new TemperatureUnit(0),
			mashTime);
		TemperatureUnit strikeWaterTemp = Equations.calcWaterTemp(
			Equations.calcTotalGrainWeight(java.util.List.of(grain)),
			strikeWaterForCalc,
			new TemperatureUnit(20, CELSIUS),
			firstRest);

		WaterAddition strikeWater = new WaterAddition(
			water,
			new VolumeUnit(15, LITRES),
			LITRES,
			strikeWaterTemp,
			mashTime);

		ArrayList<IngredientAddition> mashAdditions = new ArrayList<>();
		mashAdditions.add(grain);
		mashAdditions.add(strikeWater);

		Mash mash = new Mash(
			"mash",
			"",
			mashAdditions,
			null,
			MASH_OUT,
			mashTime,
			new TemperatureUnit(20, CELSIUS));

		TemperatureUnit targetRest = new TemperatureUnit(65, CELSIUS);
		TemperatureUnit infusionWaterTemp = Equations.calcMashInfusionWaterTemp(
			Equations.calcTotalGrainWeight(mash.getFermentableAdditions()),
			strikeWater.getVolume(),
			firstRest,
			new VolumeUnit(5, LITRES),
			targetRest);

		WaterAddition infusionWater = new WaterAddition(
			water,
			new VolumeUnit(5, LITRES),
			LITRES,
			infusionWaterTemp,
			mashTime);

		MashInfusion mashInfusion = new MashInfusion(
			"infusion",
			"",
			MASH_OUT,
			INFUSION_OUT,
			new TimeUnit(2, MINUTES),
			mashTime);
		mashInfusion.getIngredientAdditions().add(infusionWater);

		Recipe recipe = new Recipe("MashInfusionProcessTest");
		recipe.setEquipmentProfile(
			Database.getInstance().getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE));
		recipe.getSteps().add(mash);
		recipe.getSteps().add(mashInfusion);

		recipe.run();

		assertTrue(recipe.getErrors().isEmpty());

		Volume mashOut = recipe.getVolumes().getVolume(INFUSION_OUT);
		assertEquals(targetRest.get(CELSIUS), mashOut.getTemperature().get(CELSIUS), 0.05);
	}
}
