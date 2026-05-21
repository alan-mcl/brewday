package mclachlan.brewday.process;

import java.util.ArrayList;
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.BitternessUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Downstream kettle steps must not mutate registry volumes still named as upstream outputs.
 */
public class VolumeRegistryImmutabilityTest
{
	private static final String MASH_OUT = "testMashOut";
	private static final String LAUTER_MASH_OUT = "testLauterMashOut";
	private static final String FIRST_RUNNINGS = "testFirstRunnings";
	private static final String POST_BOIL_WORT = "testPostBoilWort";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void boilDoesNotMutateLauterFirstRunningsBitternessOrHopAcids()
	{
		Fermentable fermentable = Database.getInstance().getFermentables().values().iterator().next();
		FermentableAddition grain = new FermentableAddition(
			fermentable,
			new WeightUnit(5, Quantity.Unit.KILOGRAMS),
			Quantity.Unit.KILOGRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES));

		Water strike = Database.getInstance().getWaters().values().iterator().next();
		WaterAddition strikeWater = new WaterAddition(
			strike,
			new VolumeUnit(15, Quantity.Unit.LITRES),
			Quantity.Unit.LITRES,
			new TemperatureUnit(70, Quantity.Unit.CELSIUS),
			new TimeUnit(60, Quantity.Unit.MINUTES));

		ArrayList<IngredientAddition> mashAdditions = new ArrayList<>();
		mashAdditions.add(grain);
		mashAdditions.add(strikeWater);

		Mash mash = new Mash(
			"mash",
			"",
			mashAdditions,
			null,
			MASH_OUT,
			new TimeUnit(60, Quantity.Unit.MINUTES),
			new TemperatureUnit(20, Quantity.Unit.CELSIUS));

		Lauter lauter = new Lauter(
			"lauter",
			"",
			MASH_OUT,
			LAUTER_MASH_OUT,
			FIRST_RUNNINGS);

		Hop hop = Database.getInstance().getHops().values().iterator().next();
		HopAddition boilHop = new HopAddition(
			hop,
			new WeightUnit(50, Quantity.Unit.GRAMS),
			Quantity.Unit.GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES));

		ArrayList<IngredientAddition> boilAdditions = new ArrayList<>();
		boilAdditions.add(boilHop);

		Boil boil = new Boil(
			"boil",
			"",
			FIRST_RUNNINGS,
			POST_BOIL_WORT,
			null,
			boilAdditions,
			new TimeUnit(60, Quantity.Unit.MINUTES),
			false);

		Recipe recipe = new Recipe("VolumeRegistryImmutabilityTest");
		recipe.setEquipmentProfile(
			Database.getInstance().getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE));
		recipe.getSteps().add(mash);
		recipe.getSteps().add(lauter);
		recipe.getSteps().add(boil);

		recipe.run();

		assertTrue(recipe.getErrors().isEmpty());

		Volume firstRunnings = recipe.getVolumes().getVolume(FIRST_RUNNINGS);
		Volume postBoil = recipe.getVolumes().getVolume(POST_BOIL_WORT);

		assertIbuZeroOrAbsent(firstRunnings, HopBitternessFormula.TINSETH);
		assertHopAcidZeroOrAbsent(firstRunnings, Volume.Metric.ALPHA_ACIDS_MG);
		assertHopAcidZeroOrAbsent(firstRunnings, Volume.Metric.ISO_ALPHA_ACIDS_MG);

		BitternessUnit postBoilIbu = postBoil.getBitterness(HopBitternessFormula.TINSETH);
		assertNotNull(postBoilIbu);
		assertTrue(
			"post-boil wort should have kettle hop bitterness",
			postBoilIbu.get(Quantity.Unit.IBU) > 0.5);
	}

	private static void assertIbuZeroOrAbsent(Volume volume, HopBitternessFormula formula)
	{
		BitternessUnit b = volume.getBitterness(formula);
		if (b != null)
		{
			assertTrue(
				formula + " on upstream volume should be zero after boil, was "
					+ b.get(Quantity.Unit.IBU),
				b.get(Quantity.Unit.IBU) < 0.01);
		}
	}

	private static void assertHopAcidZeroOrAbsent(Volume volume, Volume.Metric metric)
	{
		WeightUnit mass = metric == Volume.Metric.ALPHA_ACIDS_MG
			? volume.getAlphaAcidsMg()
			: volume.getIsoAlphaAcidsMg();
		if (mass != null)
		{
			assertTrue(
				metric + " on upstream volume should be zero after boil, was "
					+ mass.get(Quantity.Unit.MILLIGRAMS) + " mg",
				mass.get(Quantity.Unit.MILLIGRAMS) < 0.5);
		}
	}
}
