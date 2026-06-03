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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.PropertiesSilo;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.*;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Manual harness for {@link YeastCalculator}.
 */
public class TestYeastCalculator
{
	public static void main(String[] args) throws Exception
	{
		installProcessStringsOnly();
		testDryCellEstimate();
		testLiquidCellEstimate();
		testSlurryEstimate();
		testAgeViabilityDecay();
		testPitchRatioParityWithFermentPhase();
	}

	/*-------------------------------------------------------------------------*/
	private static void testDryCellEstimate()
	{
		System.out.println("--- dry cell estimate ---");
		Yeast yeast = aleYeast();
		yeast.setForm(Yeast.Form.DRY);
		YeastCulture culture = new YeastCulture(
			yeast,
			new WeightUnit(11D, GRAMS),
			GRAMS,
			0L,
			null,
			0,
			YeastActivityState.ACTIVE,
			YeastSourceType.DIRECT_PITCH);
		YeastCalculator.CellEstimate est = YeastCalculator.estimateCellsForCulture(culture);
		long expected = 11L * YeastCalculator.DRY_YEAST_CELLS_PER_GRAM;
		System.out.printf(
			"11 g dry -> %s (expect ~220B): %s%n",
			YeastCalculator.formatCells(est.cells()),
			Math.abs(est.cells() - expected) < expected * 0.01);
	}

	/*-------------------------------------------------------------------------*/
	private static void testLiquidCellEstimate()
	{
		System.out.println("--- liquid cell estimate ---");
		Yeast yeast = aleYeast();
		yeast.setForm(Yeast.Form.LIQUID);
		YeastCulture culture = new YeastCulture(
			yeast,
			new VolumeUnit(125D, MILLILITRES),
			MILLILITRES,
			0L,
			null,
			0,
			YeastActivityState.ACTIVE,
			YeastSourceType.DIRECT_PITCH);
		YeastCalculator.CellEstimate est = YeastCalculator.estimateCellsForCulture(culture);
		long expected = YeastCalculator.LIQUID_PACKAGE_CELLS;
		System.out.printf(
			"125 mL liquid -> %s (expect ~100B): %s%n",
			YeastCalculator.formatCells(est.cells()),
			Math.abs(est.cells() - expected) < expected * 0.05);
	}

	/*-------------------------------------------------------------------------*/
	private static void testSlurryEstimate()
	{
		System.out.println("--- slurry source estimate ---");
		Yeast yeast = aleYeast();
		yeast.setForm(Yeast.Form.LIQUID);
		YeastCulture culture = new YeastCulture(
			yeast,
			new VolumeUnit(100D, MILLILITRES),
			MILLILITRES,
			0L,
			null,
			0,
			YeastActivityState.ACTIVE,
			YeastSourceType.REPITCHED_SLURRY);
		YeastCalculator.CellEstimate est = YeastCalculator.estimateCellsForCulture(culture);
		long expected = (long)(100D * YeastCalculator.SLURRY_DEFAULT_CELLS_PER_ML);
		System.out.printf(
			"100 mL slurry @ 1B/mL default -> %s (expect 100B): %s lowConf=%s%n",
			YeastCalculator.formatCells(est.cells()),
			est.cells() == expected,
			est.lowConfidence());
	}

	/*-------------------------------------------------------------------------*/
	private static void testAgeViabilityDecay()
	{
		System.out.println("--- package age viability ---");
		PercentageUnit fresh = YeastCalculator.estimateViabilityFromAge(
			Yeast.Form.LIQUID,
			LocalDate.now().minusMonths(1),
			LocalDate.now(),
			YeastCalculator.StorageTemperature.COLD_4C);
		PercentageUnit old = YeastCalculator.estimateViabilityFromAge(
			Yeast.Form.LIQUID,
			LocalDate.now().minusMonths(6),
			LocalDate.now(),
			YeastCalculator.StorageTemperature.COLD_4C);
		System.out.printf(
			"1 mo liquid cold=%.0f%% 6 mo=%.0f%% older lower: %s%n",
			fresh.get(PERCENTAGE) * 100D,
			old.get(PERCENTAGE) * 100D,
			old.get(PERCENTAGE) < fresh.get(PERCENTAGE));
	}

	/*-------------------------------------------------------------------------*/
	private static void testPitchRatioParityWithFermentPhase()
	{
		System.out.println("--- pitch ratio parity with fermentPhase ---");

		Yeast yeast = aleYeast();
		yeast.setForm(Yeast.Form.DRY);
		Volume wort = wortVolume(20D, 12.5D);
		YeastAddition pitch = new YeastAddition(
			yeast,
			new WeightUnit(11D, GRAMS),
			GRAMS);

		FermentationResult ferment = FermentationCalculator.fermentPhase(
			wort,
			List.of(pitch),
			new TemperatureUnit(20D),
			new TemperatureUnit(20D),
			new TimeUnit(14, DAYS, false),
			new ProcessLog());

		YeastCalculator.Result calc = YeastCalculator.calculate(new YeastCalculator.PitchInput(
			yeast,
			pitch.getQuantity(),
			pitch.getUnit(),
			YeastSourceType.DIRECT_PITCH,
			YeastCalculator.CellCountMode.ESTIMATE_FROM_QUANTITY,
			null,
			null,
			YeastCalculator.ViabilityMode.DEFAULT_BY_SOURCE,
			null,
			null,
			null,
			null,
			20D,
			12.5D,
			new TemperatureUnit(20D)));

		double diff = Math.abs(ferment.getPitchRatio() - calc.pitchRatio());
		System.out.printf(
			"fermentPhase ratio=%.3f calculator=%.3f diff=%.4f match=%s%n",
			ferment.getPitchRatio(),
			calc.pitchRatio(),
			diff,
			diff < 0.02);
	}

	/*-------------------------------------------------------------------------*/
	private static Yeast aleYeast()
	{
		Yeast yeast = new Yeast("Test Ale");
		yeast.setType(Yeast.Type.ALE);
		yeast.setForm(Yeast.Form.DRY);
		yeast.setAttenuation(new PercentageUnit(0.75D, false));
		return yeast;
	}

	/*-------------------------------------------------------------------------*/
	private static Volume wortVolume(double litres, double plato)
	{
		Volume wort = new Volume("wort", Volume.Type.WORT);
		wort.setVolume(new VolumeUnit(litres, LITRES));
		wort.setGravity(new DensityUnit(plato, PLATO));
		wort.setFermentability(new PercentageUnit(0.85D, false));
		return wort;
	}

	/*-------------------------------------------------------------------------*/
	private static void installProcessStringsOnly() throws Exception
	{
		Database db = new Database(Paths.get("test_data/test_db").toAbsolutePath().toString());
		try (BufferedReader reader = new BufferedReader(
			new FileReader("data/strings/process.properties")))
		{
			java.util.Properties processStrings = new PropertiesSilo().load(reader, db);
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
