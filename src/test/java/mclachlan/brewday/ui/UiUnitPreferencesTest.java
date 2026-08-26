package mclachlan.brewday.ui;

import java.util.HashMap;
import java.util.Map;
import mclachlan.brewday.Settings;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.FAHRENHEIT;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.OUNCES;
import static mclachlan.brewday.math.Quantity.Unit.POUNDS;
import static mclachlan.brewday.math.Quantity.Unit.PSI;
import static mclachlan.brewday.math.Quantity.Unit.US_GALLON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UiUnitPreferencesTest
{
	@Test
	public void missingKeysUseDefaults()
	{
		UiUnitPreferences prefs = UiUnitPreferences.from(new Settings(Map.of()));
		UiUnitPreferences defaults = UiUnitPreferences.defaults();

		for (UiUnitPreferences.Slot slot : UiUnitPreferences.Slot.values())
		{
			assertEquals(defaults.get(slot), prefs.get(slot));
		}
	}

	@Test
	public void invalidKeysFallBackToDefaults()
	{
		Map<String, String> raw = new HashMap<>();
		raw.put(Settings.UX_UNIT_TEMPERATURE, "NOT_A_UNIT");
		raw.put(Settings.UX_UNIT_BATCH_VOLUME, "CELSIUS");

		UiUnitPreferences prefs = UiUnitPreferences.from(new Settings(raw));

		assertEquals(CELSIUS, prefs.get(UiUnitPreferences.Slot.TEMPERATURE));
		assertEquals(LITRES, prefs.get(UiUnitPreferences.Slot.BATCH_VOLUME));
	}

	@Test
	public void metricAndImperialPresets()
	{
		UiUnitPreferences metric = UiUnitPreferences.metric();
		assertEquals(KILOGRAMS, metric.get(UiUnitPreferences.Slot.FERMENTABLE_WEIGHT));
		assertEquals(GRAMS, metric.get(UiUnitPreferences.Slot.HOP_MISC_WEIGHT));
		assertEquals(LITRES, metric.get(UiUnitPreferences.Slot.BATCH_VOLUME));
		assertEquals(CELSIUS, metric.get(UiUnitPreferences.Slot.TEMPERATURE));

		UiUnitPreferences imperial = UiUnitPreferences.imperial();
		assertEquals(POUNDS, imperial.get(UiUnitPreferences.Slot.FERMENTABLE_WEIGHT));
		assertEquals(OUNCES, imperial.get(UiUnitPreferences.Slot.HOP_MISC_WEIGHT));
		assertEquals(US_GALLON, imperial.get(UiUnitPreferences.Slot.BATCH_VOLUME));
		assertEquals(FAHRENHEIT, imperial.get(UiUnitPreferences.Slot.TEMPERATURE));
		assertEquals(PSI, imperial.get(UiUnitPreferences.Slot.PRESSURE));
	}

	@Test
	public void persistAndClear()
	{
		Settings settings = new Settings(new HashMap<>());
		UiUnitPreferences.imperial().persist(settings);
		assertEquals("POUNDS", settings.get(Settings.UX_UNIT_FERMENTABLE_WEIGHT));
		assertEquals("FAHRENHEIT", settings.get(Settings.UX_UNIT_TEMPERATURE));

		UiUnitPreferences.clearPersisted(settings);
		assertNull(settings.get(Settings.UX_UNIT_FERMENTABLE_WEIGHT));
		assertNull(settings.get(Settings.UX_UNIT_TEMPERATURE));
	}

	@Test
	public void getUnitForStepAndIngredientUsesPreferences()
	{
		Settings settings = new Settings(new HashMap<>());
		UiUnitPreferences.imperial().persist(settings);

		assertEquals(POUNDS, settings.getUnitForStepAndIngredient(
			Quantity.Type.WEIGHT, ProcessStep.Type.MASH, IngredientAddition.Type.FERMENTABLES));
		assertEquals(OUNCES, settings.getUnitForStepAndIngredient(
			Quantity.Type.WEIGHT, ProcessStep.Type.BOIL, IngredientAddition.Type.HOPS));
		assertEquals(US_GALLON, settings.getUnitForStepAndIngredient(
			Quantity.Type.VOLUME, ProcessStep.Type.MASH, IngredientAddition.Type.WATER));
		assertEquals(FAHRENHEIT, settings.getUnitForStepAndIngredient(
			Quantity.Type.TEMPERATURE, ProcessStep.Type.FERMENT, IngredientAddition.Type.YEAST));
		assertEquals(Quantity.Unit.MINUTES, settings.getUnitForStepAndIngredient(
			Quantity.Type.TIME, ProcessStep.Type.BOIL, IngredientAddition.Type.HOPS));
		assertEquals(Quantity.Unit.DAYS, settings.getUnitForStepAndIngredient(
			Quantity.Type.TIME, ProcessStep.Type.FERMENT, IngredientAddition.Type.YEAST));
	}

	@Test
	public void smallLengthUsesInchWhenLengthPreferenceIsImperial()
	{
		UiUnitPreferences imperial = UiUnitPreferences.imperial();
		assertEquals(Quantity.Unit.INCH, imperial.getSmallLengthUnit(false));
		assertEquals(Quantity.Unit.INCH, imperial.getSmallLengthUnit(true));

		UiUnitPreferences metric = UiUnitPreferences.metric();
		assertEquals(Quantity.Unit.MILLIMETRE, metric.getSmallLengthUnit(false));
		assertEquals(Quantity.Unit.CENTIMETRE, metric.getSmallLengthUnit(true));
	}
}
