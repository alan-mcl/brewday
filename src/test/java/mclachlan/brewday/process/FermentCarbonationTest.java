package mclachlan.brewday.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.CarbonationUnit;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.YeastAddition;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.DAYS;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS_PER_LITRE;
import static mclachlan.brewday.math.Quantity.Unit.GU;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FermentCarbonationTest
{
	private static final String WORT_IN = "wort_in";
	private static final String BEER_PRIMARY = "beer_primary";
	private static final String BEER_FINAL = "beer_final";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void primaryFermentSetsAtmosphericEquilibriumCarbonation()
	{
		Yeast yeast = aleYeast("Primary Carb", 0.75D);
		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);

		Ferment primary = fermentStep(
			"primary",
			WORT_IN,
			BEER_PRIMARY,
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(7, DAYS, false),
			new ArrayList<>(List.of(pitch)),
			Ferment.FermentType.PRIMARY);

		Volumes volumes = new Volumes();
		volumes.addVolume(WORT_IN, wort);

		primary.apply(volumes, equipment(), new ProcessLog());

		CarbonationUnit expected = Equations.calcEquilibriumCo2(
			new TemperatureUnit(20D),
			Const.ONE_ATMOSPHERE_IN_KPA);
		CarbonationUnit actual = volumes.getVolume(BEER_PRIMARY).getCarbonation();

		assertEquals(expected.get(GRAMS_PER_LITRE), actual.get(GRAMS_PER_LITRE), 0.001);
	}

	@Test
	public void chainedSecondaryPreservesPrimaryCarbonation()
	{
		Yeast yeast = aleYeast("Chained Carb", 0.75D);
		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);

		Ferment primary = fermentStep(
			"primary",
			WORT_IN,
			BEER_PRIMARY,
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(7, DAYS, false),
			new ArrayList<>(List.of(pitch)),
			Ferment.FermentType.PRIMARY);

		Ferment secondary = fermentStep(
			"secondary",
			BEER_PRIMARY,
			BEER_FINAL,
			new TemperatureUnit(4D),
			new TemperatureUnit(4D),
			new TimeUnit(14, DAYS, false),
			Collections.emptyList(),
			Ferment.FermentType.SECONDARY);

		Volumes volumes = new Volumes();
		volumes.addVolume(WORT_IN, wort);

		primary.apply(volumes, equipment(), new ProcessLog());
		double primaryCarb = volumes.getVolume(BEER_PRIMARY).getCarbonation().get(GRAMS_PER_LITRE);

		secondary.apply(volumes, equipment(), new ProcessLog());

		double finalCarb = volumes.getVolume(BEER_FINAL).getCarbonation().get(GRAMS_PER_LITRE);
		double coldEquilibrium = Equations.calcEquilibriumCo2(
			new TemperatureUnit(4D),
			Const.ONE_ATMOSPHERE_IN_KPA).get(GRAMS_PER_LITRE);

		assertEquals(primaryCarb, finalCarb, 0.001);
		assertNotEquals(coldEquilibrium, finalCarb, 0.001);
	}

	/*-------------------------------------------------------------------------*/
	private static Ferment fermentStep(
		String name,
		String inputVolume,
		String outputVolume,
		TemperatureUnit startTemp,
		TemperatureUnit endTemp,
		TimeUnit duration,
		List<mclachlan.brewday.recipe.IngredientAddition> pitches,
		Ferment.FermentType fermentType)
	{
		Ferment step = new Ferment(
			name,
			"",
			inputVolume,
			outputVolume,
			startTemp,
			endTemp,
			duration,
			pitches,
			false,
			fermentType);
		step.setInputVolume(inputVolume);
		step.setOutputVolume(outputVolume);
		return step;
	}

	/*-------------------------------------------------------------------------*/
	private static EquipmentProfile equipment()
	{
		EquipmentProfile equipment = new EquipmentProfile();
		equipment.setFermenterVolume(new VolumeUnit(30D, LITRES));
		return equipment;
	}

	/*-------------------------------------------------------------------------*/
	private static Yeast aleYeast(String name, double attenuation)
	{
		Yeast yeast = new Yeast(name);
		yeast.setType(Yeast.Type.ALE);
		yeast.setForm(Yeast.Form.DRY);
		yeast.setAttenuation(new PercentageUnit(attenuation));
		yeast.setMinTemp(new TemperatureUnit(15D));
		yeast.setMaxTemp(new TemperatureUnit(24D));
		return yeast;
	}

	/*-------------------------------------------------------------------------*/
	private static Volume wortVolume(double litres, double sg, double fermentability)
	{
		Volume wort = new Volume("wort", Volume.Type.WORT);
		wort.setVolume(new VolumeUnit(litres, LITRES));
		wort.setGravity(new DensityUnit((sg - 1D) * 1000D, GU, false));
		wort.setFermentability(new PercentageUnit(fermentability));
		wort.setTemperature(new TemperatureUnit(20D));
		wort.setColour(new ColourUnit(5D, SRM, false));
		return wort;
	}
}
