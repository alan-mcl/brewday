package mclachlan.brewday.db;

import java.util.ArrayList;
import java.util.List;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.HopStand;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.process.Steep;
import mclachlan.brewday.process.YeastRehydrate;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.YeastAddition;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class StandStepMigrationTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void cleanStandIsTrimmedInPlace()
	{
		Stand stand = legacyStand(List.of());
		Recipe recipe = recipeWithStep(stand);

		StandStepMigration.Result result = StandStepMigration.migrateRecipe(recipe);

		assertEquals(0, result.migrated());
		assertEquals(0, result.manualReview());
		assertTrue(recipe.getSteps().get(0) instanceof Stand);
		assertTrue(((Stand)recipe.getSteps().get(0)).getIngredientAdditions().isEmpty());
	}

	@Test
	public void hopsMigrateToHopStand()
	{
		assumeFalse(Database.getInstance().getHops().isEmpty());
		Hop hop = Database.getInstance().getHops().values().iterator().next();
		HopAddition ha = new HopAddition(hop, new WeightUnit(10, GRAMS), GRAMS, new TimeUnit(10, MINUTES, false));
		Stand stand = legacyStand(List.of(ha));
		Recipe recipe = recipeWithStep(stand);

		StandStepMigration.Result result = StandStepMigration.migrateRecipe(recipe);

		assertEquals(1, result.migrated());
		assertEquals(0, result.manualReview());
		assertTrue(recipe.getSteps().get(0) instanceof HopStand);
	}

	@Test
	public void yeastMigratesToYeastRehydrate()
	{
		assumeFalse(Database.getInstance().getYeasts().isEmpty());
		Yeast yeast = Database.getInstance().getYeasts().values().iterator().next();
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11, GRAMS), GRAMS);
		Stand stand = legacyStand(List.of(pitch));
		Recipe recipe = recipeWithStep(stand);

		StandStepMigration.Result result = StandStepMigration.migrateRecipe(recipe);

		assertEquals(1, result.migrated());
		assertTrue(recipe.getSteps().get(0) instanceof YeastRehydrate);
	}

	@Test
	public void fermentablesMigrateToSteep()
	{
		assumeFalse(Database.getInstance().getFermentables().isEmpty());
		Fermentable fermentable = Database.getInstance().getFermentables().values().iterator().next();
		FermentableAddition fa = new FermentableAddition(
			fermentable, new WeightUnit(0.5, KILOGRAMS), KILOGRAMS, new TimeUnit(30, MINUTES, false));
		Stand stand = legacyStand(List.of(fa));
		Recipe recipe = recipeWithStep(stand);

		StandStepMigration.Result result = StandStepMigration.migrateRecipe(recipe);

		assertEquals(1, result.migrated());
		assertTrue(recipe.getSteps().get(0) instanceof Steep);
	}

	@Test
	public void waterOnlyIsManualReview()
	{
		assumeFalse(Database.getInstance().getWaters().isEmpty());
		Water water = Database.getInstance().getWaters().values().iterator().next();
		WaterAddition wa = new WaterAddition(
			water,
			new VolumeUnit(2, LITRES),
			LITRES,
			new TemperatureUnit(20, CELSIUS, false),
			new TimeUnit(0, MINUTES, false));
		Stand stand = legacyStand(List.of(wa));
		Recipe recipe = recipeWithStep(stand);

		StandStepMigration.Result result = StandStepMigration.migrateRecipe(recipe);

		assertEquals(0, result.migrated());
		assertEquals(1, result.manualReview());
		assertTrue(recipe.getSteps().get(0) instanceof Stand);
	}

	@Test
	public void mixedIngredientsAreManualReview()
	{
		assumeFalse(Database.getInstance().getHops().isEmpty());
		assumeFalse(Database.getInstance().getFermentables().isEmpty());
		Hop hop = Database.getInstance().getHops().values().iterator().next();
		Fermentable fermentable = Database.getInstance().getFermentables().values().iterator().next();
		List<IngredientAddition> adds = List.of(
			new HopAddition(hop, new WeightUnit(10, GRAMS), GRAMS, new TimeUnit(5, MINUTES, false)),
			new FermentableAddition(
				fermentable, new WeightUnit(0.2, KILOGRAMS), KILOGRAMS, new TimeUnit(30, MINUTES, false)));
		Stand stand = legacyStand(adds);
		Recipe recipe = recipeWithStep(stand);

		StandStepMigration.Result result = StandStepMigration.migrateRecipe(recipe);

		assertEquals(0, result.migrated());
		assertEquals(1, result.manualReview());
		assertTrue(recipe.getSteps().get(0) instanceof Stand);
	}

	@Test
	public void legacyTrubOnlyMigratesToHopStand()
	{
		Stand stand = legacyStand(List.of());
		stand.setLegacyRemoveTrubAndChillerLoss(true);
		Recipe recipe = recipeWithStep(stand);

		StandStepMigration.Result result = StandStepMigration.migrateRecipe(recipe);

		assertEquals(1, result.migrated());
		assertTrue(recipe.getSteps().get(0) instanceof HopStand);
		assertTrue(((HopStand)recipe.getSteps().get(0)).isRemoveTrubAndChillerLoss());
	}

	/*-------------------------------------------------------------------------*/
	private static Stand legacyStand(List<IngredientAddition> additions)
	{
		Stand stand = new Stand(
			"legacyStand",
			"",
			"wort_in",
			"stand_out",
			new TimeUnit(30, MINUTES, false),
			new ArrayList<>(additions));
		stand.setCoolingCoefficient(0.1);
		return stand;
	}

	/*-------------------------------------------------------------------------*/
	private static Recipe recipeWithStep(ProcessStep step)
	{
		Recipe recipe = new Recipe("MigrationTest");
		recipe.getSteps().add(step);
		step.setRecipe(recipe);
		return recipe;
	}
}
