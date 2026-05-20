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

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.PropertiesSilo;
import mclachlan.brewday.Settings;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.*;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Manual harness for {@link FermentationCalculator} and chained {@link Ferment} steps.
 */
public class TestFermentationCalculator
{
	public static void main(String[] args) throws Exception
	{
		installProcessStringsOnly();

		testSingleYeastLegacyParity();
		testLiquidYeastCellEstimate();
		testBlend();
		testAleLagerPitchRateBlend();
		testChainedFerment();
		testChainedFermentRetention();
		testChainedGenerationIncrement();
	}

	/*-------------------------------------------------------------------------*/
	private static void testSingleYeastLegacyParity()
	{
		System.out.println("--- single yeast / legacy parity ---");

		Yeast yeast = aleYeast("Test Ale", 0.75D);
		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		YeastAddition pitch = new YeastAddition(
			yeast,
			new WeightUnit(11D, GRAMS),
			GRAMS);

		double legacy = Equations.calcEstimatedAttenuation(wort, pitch);

		ProcessLog log = new ProcessLog();
		FermentationResult result = FermentationCalculator.fermentPhase(
			wort,
			List.of(pitch),
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			log);

		double newEff = result.getEffectiveAttenuation();
		double legacyFgGu = wort.getGravity().get(GU) * (1D - legacy);
		double legacyFgSg = (1000D + legacyFgGu) / 1000D;
		double newFgSg = result.getEstimatedFg().get(SPECIFIC_GRAVITY);
		double diffPct = Math.abs(newFgSg - legacyFgSg) / legacyFgSg * 100D;

		System.out.printf(
			"legacy atten=%.3f eff atten=%.3f legacy FG SG=%.4f new FG SG=%.4f SG diff=%.1f%%%n",
			legacy,
			newEff,
			legacyFgSg,
			newFgSg,
			diffPct);

		boolean hasEstimateWarning = log.getWarnings().stream()
			.anyMatch(w -> w.contains("Estimated dry yeast") || w.contains("cells/g heuristic"));
		System.out.println("  normal dry estimate is not a warning: " + !hasEstimateWarning);

		for (String msg : log.getMsgs())
		{
			System.out.println("  " + msg);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void testLiquidYeastCellEstimate()
	{
		System.out.println("--- liquid yeast cell estimate ---");

		Yeast yeast = aleYeast("Liquid Test", 0.75D);
		yeast.setForm(Yeast.Form.LIQUID);
		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		YeastAddition pitch = new YeastAddition(
			yeast,
			new VolumeUnit(125D, MILLILITRES),
			MILLILITRES);

		ProcessLog log = new ProcessLog();
		FermentationResult result = FermentationCalculator.fermentPhase(
			wort,
			List.of(pitch),
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			log);

		long cells = result.getEvolvedCultures().get(0).getCellCount();
		long expected = 100_000_000_000L;
		boolean ok = Math.abs(cells - expected) < expected * 0.05;
		boolean noWarning = log.getWarnings().isEmpty();

		System.out.printf(
			"125 mL liquid -> %d cells (expect ~100B): %s, no warning: %s%n",
			cells,
			ok,
			noWarning);
	}

	/*-------------------------------------------------------------------------*/
	private static void testAleLagerPitchRateBlend()
	{
		System.out.println("--- ale/lager weighted pitch rate ---");

		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		Yeast ale = aleYeast("Ale", 0.75D);
		Yeast lager = aleYeast("Lager", 0.75D);
		lager.setType(Yeast.Type.LAGER);

		double aleRatio = FermentationCalculator.fermentPhase(
			wort,
			List.of(new YeastAddition(ale, new WeightUnit(11D, GRAMS), GRAMS)),
			new TemperatureUnit(20D), new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			new ProcessLog()).getPitchRatio();

		double lagerRatio = FermentationCalculator.fermentPhase(
			wort,
			List.of(new YeastAddition(lager, new WeightUnit(11D, GRAMS), GRAMS)),
			new TemperatureUnit(20D), new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			new ProcessLog()).getPitchRatio();

		double blendRatio = FermentationCalculator.fermentPhase(
			wort,
			List.of(
				new YeastAddition(ale, new WeightUnit(5.5D, GRAMS), GRAMS),
				new YeastAddition(lager, new WeightUnit(5.5D, GRAMS), GRAMS)),
			new TemperatureUnit(20D), new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			new ProcessLog()).getPitchRatio();

		System.out.printf(
			"pitch ratio ale=%.2f lager=%.2f blend=%.2f blend between: %s%n",
			aleRatio,
			lagerRatio,
			blendRatio,
			blendRatio >= Math.min(aleRatio, lagerRatio)
				&& blendRatio <= Math.max(aleRatio, lagerRatio));
	}

	/*-------------------------------------------------------------------------*/
	private static void testChainedGenerationIncrement()
	{
		System.out.println("--- chained generation increment ---");

		Yeast yeast = aleYeast("Gen Test", 0.75D);
		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);

		EquipmentProfile equipment = new EquipmentProfile();
		equipment.setFermenterVolume(new VolumeUnit(30D, LITRES));

		Ferment step1 = new Ferment(
			"f1", "", "wort_in", "beer1",
			new TemperatureUnit(20D), new TemperatureUnit(20D),
			new TimeUnit(7, DAYS, false),
			new ArrayList<>(List.of(pitch)), false);
		step1.setInputVolume("wort_in");
		step1.setOutputVolume("beer1");

		Ferment step2 = new Ferment(
			"f2", "", "beer1", "beer2",
			new TemperatureUnit(18D), new TemperatureUnit(18D),
			new TimeUnit(7, DAYS, false),
			Collections.emptyList(), false);
		step2.setInputVolume("beer1");
		step2.setOutputVolume("beer2");

		Ferment step3 = new Ferment(
			"f3", "", "beer2", "beer3",
			new TemperatureUnit(18D), new TemperatureUnit(18D),
			new TimeUnit(7, DAYS, false),
			Collections.emptyList(), false);
		step3.setInputVolume("beer2");
		step3.setOutputVolume("beer3");

		Volumes volumes = new Volumes();
		volumes.addVolume("wort_in", wort);

		step1.apply(volumes, equipment, new ProcessLog());
		step2.apply(volumes, equipment, new ProcessLog());
		step3.apply(volumes, equipment, new ProcessLog());

		int gen = volumes.getVolume("beer3").getYeastCultures().get(0).getGeneration();
		System.out.printf("generation after 3 chained ferments (expect 1): %d %s%n", gen, gen == 1);
	}

	/*-------------------------------------------------------------------------*/
	private static void testBlend()
	{
		System.out.println("--- two-strain blend ---");

		Yeast low = aleYeast("Low Atten", 0.70D);
		Yeast high = aleYeast("High Atten", 0.80D);
		Volume wort = wortVolume(20D, 1.050D, 0.90D);

		List<YeastAddition> pitches = List.of(
			new YeastAddition(low, new WeightUnit(5.5D, GRAMS), GRAMS),
			new YeastAddition(high, new WeightUnit(5.5D, GRAMS), GRAMS));

		ProcessLog log = new ProcessLog();
		FermentationResult blend = FermentationCalculator.fermentPhase(
			wort,
			pitches,
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			log);

		double lowOnly = FermentationCalculator.fermentPhase(
			wort,
			List.of(pitches.get(0)),
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			new ProcessLog()).getEstimatedFg().get(SPECIFIC_GRAVITY);

		double highOnly = FermentationCalculator.fermentPhase(
			wort,
			List.of(pitches.get(1)),
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			new ProcessLog()).getEstimatedFg().get(SPECIFIC_GRAVITY);

		double blendFg = blend.getEstimatedFg().get(SPECIFIC_GRAVITY);

		System.out.printf(
			"low FG=%.4f high FG=%.4f blend FG=%.4f (blend more attenuated than singles: %s)%n",
			lowOnly,
			highOnly,
			blendFg,
			blendFg < Math.min(lowOnly, highOnly));
	}

	/*-------------------------------------------------------------------------*/
	private static void testChainedFerment()
	{
		System.out.println("--- chained ferment steps ---");

		Yeast yeast = aleYeast("Chained", 0.75D);
		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);

		EquipmentProfile equipment = new EquipmentProfile();
		equipment.setFermenterVolume(new VolumeUnit(30D, LITRES));

		Ferment primary = new Ferment(
			"primary",
			"",
			"wort_in",
			"beer_primary",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(7, DAYS, false),
			new ArrayList<>(List.of(pitch)),
			false);
		primary.setInputVolume("wort_in");
		primary.setOutputVolume("beer_primary");

		Ferment secondary = new Ferment(
			"secondary",
			"",
			"beer_primary",
			"beer_final",
			new TemperatureUnit(18D),
			new TemperatureUnit(18D),
			new TimeUnit(14, DAYS, false),
			Collections.emptyList(),
			false);
		secondary.setInputVolume("beer_primary");
		secondary.setOutputVolume("beer_final");

		Volumes volumes = new Volumes();
		volumes.addVolume("wort_in", wort);

		ProcessLog log = new ProcessLog();
		primary.apply(volumes, equipment, log);
		Volume afterPrimary = volumes.getVolume("beer_primary");
		double fgPrimary = afterPrimary.getGravity().get(SPECIFIC_GRAVITY);

		log = new ProcessLog();
		secondary.apply(volumes, equipment, log);
		Volume afterSecondary = volumes.getVolume("beer_final");
		double fgSecondary = afterSecondary.getGravity().get(SPECIFIC_GRAVITY);

		int cultureCount = afterSecondary.getYeastCultures().size();
		boolean hasPitchOnBeer = afterSecondary.getIngredientAdditions().stream()
			.anyMatch(ia -> ia instanceof YeastAddition);

		System.out.printf(
			"primary FG=%.4f secondary FG=%.4f lowered further: %s%n",
			fgPrimary,
			fgSecondary,
			fgSecondary < fgPrimary);
		System.out.printf(
			"cultures on output=%d pitch additions on beer=%s generation=%d%n",
			cultureCount,
			hasPitchOnBeer,
			afterSecondary.getYeastCultures().isEmpty()
				? -1
				: afterSecondary.getYeastCultures().get(0).getGeneration());
	}

	/*-------------------------------------------------------------------------*/
	private static void testChainedFermentRetention()
	{
		System.out.println("--- chained ferment iso/colour loss (once) ---");

		double isoInMg = 1000D;
		double srmIn = 5D;
		Yeast yeast = aleYeast("Retention", 0.75D);
		Volume wort = wortVolume(20D, 1.050D, 0.85D);
		wort.setIsoAlphaAcidsMg(new WeightUnit(isoInMg, MILLIGRAMS, false));
		wort.setColour(new ColourUnit(srmIn, SRM, false));

		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);
		EquipmentProfile equipment = new EquipmentProfile();
		equipment.setFermenterVolume(new VolumeUnit(30D, LITRES));

		Ferment primary = new Ferment(
			"primary",
			"",
			"wort_in",
			"beer_primary",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(7, DAYS, false),
			new ArrayList<>(List.of(pitch)),
			false);
		primary.setInputVolume("wort_in");
		primary.setOutputVolume("beer_primary");

		Ferment secondary = new Ferment(
			"secondary",
			"",
			"beer_primary",
			"beer_final",
			new TemperatureUnit(18D),
			new TemperatureUnit(18D),
			new TimeUnit(14, DAYS, false),
			Collections.emptyList(),
			false);
		secondary.setInputVolume("beer_primary");
		secondary.setOutputVolume("beer_final");

		Volumes volumes = new Volumes();
		volumes.addVolume("wort_in", wort);

		primary.apply(volumes, equipment, new ProcessLog());
		secondary.apply(volumes, equipment, new ProcessLog());

		Volume beer = volumes.getVolume("beer_final");
		double isoOut = beer.getIsoAlphaAcidsMg().get(MILLIGRAMS);
		double srmOut = beer.getColour().get(SRM);
		double expectedIso = isoInMg * Const.ISO_ALPHA_RETENTION_DURING_FERMENTATION;
		double expectedSrm = srmIn * (1D - Const.COLOUR_LOSS_DURING_FERMENTATION);
		double squaredIso = expectedIso * Const.ISO_ALPHA_RETENTION_DURING_FERMENTATION;

		boolean isoOnce = Math.abs(isoOut - expectedIso) < 0.01;
		boolean isoNotSquared = Math.abs(isoOut - squaredIso) > 0.01;
		boolean colourOnce = Math.abs(srmOut - expectedSrm) < 0.01;

		System.out.printf(
			"iso in=%.0f out=%.0f expect once=%.0f squared=%.0f once=%s not squared=%s%n",
			isoInMg,
			isoOut,
			expectedIso,
			squaredIso,
			isoOnce,
			isoNotSquared);
		System.out.printf(
			"colour in=%.2f out=%.2f expect once=%.2f once=%s%n",
			srmIn,
			srmOut,
			expectedSrm,
			colourOnce);
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

	/*-------------------------------------------------------------------------*/
	private static void installProcessStringsOnly() throws Exception
	{
		Database db = new Database(Paths.get("test_data/test_db").toAbsolutePath().toString());
		try (BufferedReader reader = new BufferedReader(
			new FileReader("data/strings/process.properties")))
		{
			Properties processStrings = new PropertiesSilo().load(reader, db);
			Field processStringsField = Database.class.getDeclaredField("processStrings");
			processStringsField.setAccessible(true);
			processStringsField.set(db, processStrings);
		}

		Field instanceField = Database.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, db);

		Map<String, String> settingsMap = new HashMap<>();
		settingsMap.put(
			Settings.HOP_BITTERNESS_FORMULAS,
			Settings.HopBitternessFormula.TINSETH.name());
		Settings settings = new Settings(settingsMap);
		Field settingsField = Database.class.getDeclaredField("settings");
		settingsField.setAccessible(true);
		settingsField.set(db, settings);
	}
}
