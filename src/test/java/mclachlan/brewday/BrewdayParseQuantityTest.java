package mclachlan.brewday;

import mclachlan.brewday.math.Quantity;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Type.COLOUR;
import static mclachlan.brewday.math.Quantity.Type.DIASTATIC_POWER;
import static mclachlan.brewday.math.Quantity.Type.FLUID_DENSITY;
import static mclachlan.brewday.math.Quantity.Type.TEMPERATURE;
import static mclachlan.brewday.math.Quantity.Type.VOLUME;
import static mclachlan.brewday.math.Quantity.Type.WEIGHT;
import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.EBC;
import static mclachlan.brewday.math.Quantity.Unit.FAHRENHEIT;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;
import static mclachlan.brewday.math.Quantity.Unit.LINTNER;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.LOVIBOND;
import static mclachlan.brewday.math.Quantity.Unit.MILLILITRES;
import static mclachlan.brewday.math.Quantity.Unit.SPECIFIC_GRAVITY;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static mclachlan.brewday.math.Quantity.Unit.US_GALLON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BrewdayParseQuantityTest
{
	private static Quantity parse(String text, Quantity.Type type, Quantity.Unit hint)
	{
		return Brewday.getInstance().parseQuantity(text, type, hint);
	}

	@Test
	public void kilogramHintIsNotTreatedAsGrams()
	{
		Quantity q = parse("5", WEIGHT, KILOGRAMS);
		assertNotNull(q);
		assertEquals(5D, q.get(KILOGRAMS), 0.0001);
	}

	@Test
	public void gallonAndFahrenheitHintsAreHonored()
	{
		Quantity gal = parse("20", VOLUME, US_GALLON);
		assertNotNull(gal);
		assertEquals(20D, gal.get(US_GALLON), 0.0001);

		Quantity temp = parse("68", TEMPERATURE, FAHRENHEIT);
		assertNotNull(temp);
		assertEquals(20D, temp.get(CELSIUS), 0.0001);
	}

	@Test
	public void suffixOverridesCompatibleHint()
	{
		Quantity kg = parse("5 kg", WEIGHT, GRAMS);
		assertNotNull(kg);
		assertEquals(5D, kg.get(KILOGRAMS), 0.0001);

		Quantity litres = parse("20 L", VOLUME, MILLILITRES);
		assertNotNull(litres);
		assertEquals(20D, litres.get(LITRES), 0.0001);

		Quantity fahrenheit = parse("68 F", TEMPERATURE, CELSIUS);
		assertNotNull(fahrenheit);
		assertEquals(20D, fahrenheit.get(CELSIUS), 0.0001);
	}

	@Test
	public void lIsLitresForVolumeAndLovibondForColour()
	{
		Quantity litres = parse("20 L", VOLUME, LITRES);
		assertNotNull(litres);
		assertEquals(20D, litres.get(LITRES), 0.0001);

		Quantity colour = parse("20 L", COLOUR, SRM);
		assertNotNull(colour);
		assertEquals(20D, colour.get(LOVIBOND), 0.0001);
	}

	@Test
	public void degreeLIsLovibondOrLintnerByType()
	{
		Quantity colour = parse("20 \u00b0L", COLOUR, SRM);
		assertNotNull(colour);
		assertEquals(20D, colour.get(LOVIBOND), 0.0001);

		Quantity lintner = parse("20 \u00b0L", DIASTATIC_POWER, LINTNER);
		assertNotNull(lintner);
		assertEquals(20D, lintner.get(LINTNER), 0.0001);
	}

	@Test
	public void suffixFromWrongTypeFails()
	{
		assertNull(parse("5 kg", VOLUME, LITRES));
		assertNull(parse("20 L", WEIGHT, GRAMS));
	}

	@Test
	public void specificGravityHeuristic()
	{
		Quantity noDecimal = parse("1050", FLUID_DENSITY, SPECIFIC_GRAVITY);
		assertNotNull(noDecimal);
		assertEquals(1.050D, noDecimal.get(SPECIFIC_GRAVITY), 0.0001);

		Quantity decimal = parse("1.050", FLUID_DENSITY, SPECIFIC_GRAVITY);
		assertNotNull(decimal);
		assertEquals(1.050D, decimal.get(SPECIFIC_GRAVITY), 0.0001);

		Quantity suffixed = parse("1.050 SG", FLUID_DENSITY, SPECIFIC_GRAVITY);
		assertNotNull(suffixed);
		assertEquals(1.050D, suffixed.get(SPECIFIC_GRAVITY), 0.0001);
	}

	@Test
	public void commaDecimalAndBlankAndGarbage()
	{
		Quantity comma = parse("20,5", VOLUME, LITRES);
		assertNotNull(comma);
		assertEquals(20.5D, comma.get(LITRES), 0.0001);

		assertNull(parse("", VOLUME, LITRES));
		assertNull(parse("   ", VOLUME, LITRES));
		assertNull(parse(null, VOLUME, LITRES));
		assertNull(parse("not-a-quantity", VOLUME, LITRES));
	}

	@Test
	public void ebcRoundTripUses197Factor()
	{
		Quantity q = parse("19.7 EBC", COLOUR, SRM);
		assertNotNull(q);
		assertEquals(10D, q.get(SRM), 0.0001);
		assertEquals(19.7D, q.get(EBC), 0.0001);
	}

	@Test(expected = BrewdayException.class)
	public void hintMustBelongToType()
	{
		parse("5", VOLUME, GRAMS);
	}
}
