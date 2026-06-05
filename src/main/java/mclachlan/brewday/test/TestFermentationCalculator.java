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
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Water;
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
		testStarterNoPackagingChemistry();
		testPitchCombineStarterFlow();
		testRehydrateStandCombine();
		testRehydrateYeastRehydrateCombine();
		testStarterLiquorFirstFerment();
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
		secondary.setFermentType(Ferment.FermentType.SECONDARY);

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
	private static void testStarterNoPackagingChemistry()
	{
		System.out.println("--- STARTER ferment skips iso/colour chemistry ---");

		double isoInMg = 1000D;
		double srmIn = 5D;
		Yeast yeast = aleYeast("Starter", 0.75D);
		Volume starterWort = wortVolume(1D, 1.040D, 0.9D);
		starterWort.setName("starter_wort");
		starterWort.setIsoAlphaAcidsMg(new WeightUnit(isoInMg, MILLIGRAMS, false));
		starterWort.setColour(new ColourUnit(srmIn, SRM, false));

		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);
		EquipmentProfile equipment = new EquipmentProfile();
		equipment.setFermenterVolume(new VolumeUnit(30D, LITRES));

		Ferment starter = new Ferment(
			"starter",
			"",
			"starter_wort",
			"starter_beer",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(2, DAYS, false),
			new ArrayList<>(List.of(pitch)),
			false,
			Ferment.FermentType.STARTER);
		starter.setInputVolume("starter_wort");
		starter.setOutputVolume("starter_beer");

		Volumes volumes = new Volumes();
		volumes.addVolume("starter_wort", starterWort);
		starter.apply(volumes, equipment, new ProcessLog());

		Volume out = volumes.getVolume("starter_beer");
		double isoOut = out.getIsoAlphaAcidsMg().get(MILLIGRAMS);
		double srmOut = out.getColour().get(SRM);
		boolean isoUnchanged = Math.abs(isoOut - isoInMg) < 0.01;
		boolean colourUnchanged = Math.abs(srmOut - srmIn) < 0.01;
		boolean sourceStarter = !out.getYeastCultures().isEmpty()
			&& out.getYeastCultures().get(0).getSourceType() == YeastSourceType.STARTER;

		System.out.printf(
			"iso unchanged=%s colour unchanged=%s source STARTER=%s%n",
			isoUnchanged,
			colourUnchanged,
			sourceStarter);
	}

	/*-------------------------------------------------------------------------*/
	private static void testPitchCombineStarterFlow()
	{
		System.out.println("--- pitch combine + PRIMARY ---");

		Yeast yeast = aleYeast("Flow", 0.75D);
		Volume mainWort = wortVolume(20D, 1.050D, 0.85D);
		mainWort.setName("main_wort");
		mainWort.setOriginalGravity(new DensityUnit(mainWort.getGravity()));

		Volume starterBeer = new Volume("starter_beer", Volume.Type.BEER);
		starterBeer.setVolume(new VolumeUnit(1D, LITRES));
		starterBeer.setGravity(new DensityUnit((1.030D - 1D) * 1000D, GU, false));
		starterBeer.setFermentability(new PercentageUnit(0.9D));
		starterBeer.setTemperature(new TemperatureUnit(20D));
		starterBeer.setColour(new ColourUnit(5D, SRM, false));
		YeastCulture culture = YeastCulture.fromPitch(
			new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS));
		culture.setCellCount(200_000_000_000L);
		culture.setSourceType(YeastSourceType.STARTER);
		starterBeer.addYeastCulture(culture);

		EquipmentProfile equipment = new EquipmentProfile();
		equipment.setFermenterVolume(new VolumeUnit(30D, LITRES));

		Combine combine = new Combine(
			"pitch",
			"",
			"main_wort",
			"starter_beer",
			"pitch_wort",
			true);
		combine.setInputVolume("main_wort");
		combine.setInputVolume2("starter_beer");
		combine.setOutputVolume("pitch_wort");

		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);
		Ferment primary = new Ferment(
			"primary",
			"",
			"pitch_wort",
			"beer_out",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			Collections.emptyList(),
			false,
			Ferment.FermentType.PRIMARY);
		primary.setInputVolume("pitch_wort");
		primary.setOutputVolume("beer_out");

		Volumes volumes = new Volumes();
		volumes.addVolume("main_wort", mainWort);
		volumes.addVolume("starter_beer", starterBeer);

		ProcessLog combineLog = new ProcessLog();
		combine.apply(volumes, equipment, combineLog);
		Volume pitchWort = volumes.getVolume("pitch_wort");

		boolean wortType = pitchWort.getType() == Volume.Type.WORT;
		boolean hasCulture = !pitchWort.getYeastCultures().isEmpty();
		boolean ogFromMain = pitchWort.getOriginalGravity() != null
			&& Math.abs(pitchWort.getOriginalGravity().get(GU) - mainWort.getGravity().get(GU)) < 0.5;

		double isoBefore = pitchWort.getIsoAlphaAcidsMg() == null
			? 0D
			: pitchWort.getIsoAlphaAcidsMg().get(MILLIGRAMS);
		primary.apply(volumes, equipment, new ProcessLog());
		Volume beer = volumes.getVolume("beer_out");
		double isoAfter = beer.getIsoAlphaAcidsMg().get(MILLIGRAMS);
		double expectedOnce = isoBefore * Const.ISO_ALPHA_RETENTION_DURING_FERMENTATION;
		boolean isoAppliedOnce = isoBefore <= 0D || Math.abs(isoAfter - expectedOnce) < 0.01;

		System.out.printf(
			"pitch wort type WORT=%s cultures=%s OG from main=%s iso once=%s combine errors=%d%n",
			wortType,
			hasCulture,
			ogFromMain,
			isoAppliedOnce,
			combineLog.getErrors().size());
	}

	/*-------------------------------------------------------------------------*/
	private static void testRehydrateStandCombine()
	{
		System.out.println("--- rehydrate: Stand WORT+yeast -> Combine WORT+WORT -> PRIMARY ---");

		Yeast yeast = aleYeast("Rehydrate", 0.75D);
		Volume mainWort = wortVolume(20D, 1.050D, 0.85D);
		mainWort.setName("main_wort");

		WaterAddition rehydrateWater = simpleWaterAddition(0.2D);
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);

		YeastRehydrate stand = new YeastRehydrate(
			"rehydrate",
			"",
			null,
			"rehydrate_liquor",
			new TimeUnit(30, MINUTES, false),
			new ArrayList<>(List.of(rehydrateWater, pitch)));
		stand.setOutputVolume("rehydrate_liquor");

		Combine combine = new Combine(
			"combine",
			"",
			"main_wort",
			"rehydrate_liquor",
			"pitch_wort",
			false);
		combine.setInputVolume("main_wort");
		combine.setInputVolume2("rehydrate_liquor");
		combine.setOutputVolume("pitch_wort");

		Ferment primary = new Ferment(
			"primary",
			"",
			"pitch_wort",
			"beer_out",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			Collections.emptyList(),
			false,
			Ferment.FermentType.PRIMARY);
		primary.setInputVolume("pitch_wort");
		primary.setOutputVolume("beer_out");

		EquipmentProfile equipment = minimalEquipment();

		Volumes volumes = new Volumes();
		volumes.addVolume("main_wort", mainWort);

		ProcessLog log = new ProcessLog();
		stand.apply(volumes, equipment, log);
		combine.apply(volumes, equipment, log);

		Volume pitchWort = volumes.getVolume("pitch_wort");
		boolean hasCulture = !pitchWort.getYeastCultures().isEmpty();

		log = new ProcessLog();
		primary.apply(volumes, equipment, log);
		boolean fermented = volumes.getVolume("beer_out").getType() == Volume.Type.BEER
			&& log.getErrors().isEmpty();

		System.out.printf(
			"rehydrate liquor vol=%.2fL cultures on pitch=%s primary ok=%s%n",
			volumes.getVolume("rehydrate_liquor").getVolume().get(LITRES),
			hasCulture,
			fermented);
	}

	/*-------------------------------------------------------------------------*/
	private static void testRehydrateYeastRehydrateCombine()
	{
		System.out.println("--- rehydrate: YeastRehydrate WORT+yeast -> Combine WORT+WORT -> PRIMARY ---");

		Yeast yeast = aleYeast("Rehydrate", 0.75D);
		Volume mainWort = wortVolume(20D, 1.050D, 0.85D);
		mainWort.setName("main_wort");

		WaterAddition rehydrateWater = simpleWaterAddition(0.2D);
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);

		YeastRehydrate yeastRehydrate = new YeastRehydrate(
			"rehydrate",
			"",
			null,
			"rehydrate_liquor",
			new TimeUnit(30, MINUTES, false),
			new ArrayList<>(List.of(rehydrateWater, pitch)));
		yeastRehydrate.setOutputVolume("rehydrate_liquor");

		Combine combine = new Combine(
			"combine",
			"",
			"main_wort",
			"rehydrate_liquor",
			"pitch_wort",
			false);
		combine.setInputVolume("main_wort");
		combine.setInputVolume2("rehydrate_liquor");
		combine.setOutputVolume("pitch_wort");

		Ferment primary = new Ferment(
			"primary",
			"",
			"pitch_wort",
			"beer_out",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			Collections.emptyList(),
			false,
			Ferment.FermentType.PRIMARY);
		primary.setInputVolume("pitch_wort");
		primary.setOutputVolume("beer_out");

		EquipmentProfile equipment = minimalEquipment();

		Volumes volumes = new Volumes();
		volumes.addVolume("main_wort", mainWort);

		ProcessLog log = new ProcessLog();
		yeastRehydrate.apply(volumes, equipment, log);
		combine.apply(volumes, equipment, log);

		Volume pitchWort = volumes.getVolume("pitch_wort");
		boolean hasCulture = !pitchWort.getYeastCultures().isEmpty();

		log = new ProcessLog();
		primary.apply(volumes, equipment, log);
		boolean fermented = volumes.getVolume("beer_out").getType() == Volume.Type.BEER
			&& log.getErrors().isEmpty();

		System.out.printf(
			"yeast rehydrate liquor vol=%.2fL cultures on pitch=%s primary ok=%s%n",
			volumes.getVolume("rehydrate_liquor").getVolume().get(LITRES),
			hasCulture,
			fermented);
	}

	/*-------------------------------------------------------------------------*/
	private static void testStarterLiquorFirstFerment()
	{
		System.out.println("--- STARTER liquor-first: Ferment bootstrap -> pitchCombine -> PRIMARY ---");

		Yeast yeast = aleYeast("StarterLiq", 0.75D);
		Volume mainWort = wortVolume(20D, 1.050D, 0.85D);
		mainWort.setName("main_wort");
		mainWort.setOriginalGravity(new DensityUnit(mainWort.getGravity()));

		WaterAddition starterWater = simpleWaterAddition(1D);
		YeastAddition pitch = new YeastAddition(yeast, new WeightUnit(11D, GRAMS), GRAMS);
		FermentableAddition dme = simpleDmeAddition(0.1D);

		Ferment starter = new Ferment(
			"starter",
			"",
			null,
			"starter_beer",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(2, DAYS, false),
			new ArrayList<>(List.of(starterWater, pitch, dme)),
			false,
			Ferment.FermentType.STARTER);
		starter.setOutputVolume("starter_beer");

		Combine combine = new Combine(
			"pitch",
			"",
			"main_wort",
			"starter_beer",
			"pitch_wort",
			true);
		combine.setInputVolume("main_wort");
		combine.setInputVolume2("starter_beer");
		combine.setOutputVolume("pitch_wort");

		Ferment primary = new Ferment(
			"primary",
			"",
			"pitch_wort",
			"beer_out",
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			Collections.emptyList(),
			false,
			Ferment.FermentType.PRIMARY);
		primary.setInputVolume("pitch_wort");
		primary.setOutputVolume("beer_out");

		EquipmentProfile equipment = minimalEquipment();

		Volumes volumes = new Volumes();
		volumes.addVolume("main_wort", mainWort);

		ProcessLog log = new ProcessLog();
		starter.apply(volumes, equipment, log);
		double starterLitres = 0D;
		boolean starterVolumeOk = false;
		if (volumes.contains("starter_beer"))
		{
			starterLitres = volumes.getVolume("starter_beer").getVolume().get(LITRES);
			starterVolumeOk = Math.abs(starterLitres - 1D) < 0.05D;
		}
		boolean starterOut = volumes.contains("starter_beer")
			&& volumes.getVolume("starter_beer").getType() == Volume.Type.BEER
			&& starterVolumeOk;

		log = new ProcessLog();
		combine.apply(volumes, equipment, log);
		boolean pitchWort = volumes.contains("pitch_wort")
			&& volumes.getVolume("pitch_wort").getType() == Volume.Type.WORT
			&& !volumes.getVolume("pitch_wort").getYeastCultures().isEmpty();

		log = new ProcessLog();
		primary.apply(volumes, equipment, log);
		boolean primaryOk = volumes.contains("beer_out") && log.getErrors().isEmpty();

		System.out.printf(
			"starter output=%s vol=%.2fL (~1L)=%s pitch wort=%s primary=%s starter errors=%d%n",
			starterOut,
			starterLitres,
			starterVolumeOk,
			pitchWort,
			primaryOk,
			log.getErrors().size());
	}

	/*-------------------------------------------------------------------------*/
	private static EquipmentProfile minimalEquipment()
	{
		EquipmentProfile equipment = new EquipmentProfile();
		equipment.setFermenterVolume(new VolumeUnit(30D, LITRES));
		equipment.setHopUtilisation(new PercentageUnit(1D));
		return equipment;
	}

	/*-------------------------------------------------------------------------*/
	private static WaterAddition simpleWaterAddition(double litres)
	{
		Water water = new Water("test water");
		return new WaterAddition(
			water,
			new VolumeUnit(litres, LITRES),
			LITRES,
			new TemperatureUnit(20, CELSIUS),
			new TimeUnit(0, MINUTES, false));
	}

	/*-------------------------------------------------------------------------*/
	private static FermentableAddition simpleDmeAddition(double kg)
	{
		Fermentable dme = new Fermentable("DME");
		dme.setType(Fermentable.Type.DRY_EXTRACT);
		dme.setYield(new PercentageUnit(0.65D));
		dme.setColour(new ColourUnit(2D, SRM));
		return new FermentableAddition(
			dme,
			new WeightUnit(kg, KILOGRAMS),
			KILOGRAMS,
			new TimeUnit(0, MINUTES, false));
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
		settingsMap.put(Settings.TINSETH_MAX_UTILISATION, "4.15");
		Settings settings = new Settings(settingsMap);
		Field settingsField = Database.class.getDeclaredField("settings");
		settingsField.setAccessible(true);
		settingsField.set(db, settings);
	}
}
