package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.WeightUnit;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SwingQuantityEditWidgetTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void percentageRoundTripDisplay() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setQuantity(new PercentageUnit(0.05));
		assertEquals(0.05, w.parseOrNull().get(), 0.000001);
		assertTrue(w.getText().contains("5"));
	}

	@Test
	public void densityRoundTripSpecificGravity() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<DensityUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.SPECIFIC_GRAVITY);
		w.setQuantity(new DensityUnit(50));
		DensityUnit out = w.parseOrNull();
		assertEquals(50D, out.get(), 0.0001);
		assertTrue(w.getText().contains("1.05"));
	}

	@Test
	public void lovibondRoundTrip() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<ColourUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.LOVIBOND);
		w.setQuantity(new ColourUnit(4, Quantity.Unit.LOVIBOND, false));
		ColourUnit out = w.parseOrNull();
		assertEquals(4D, out.get(Quantity.Unit.LOVIBOND), 0.01);
	}

	@Test
	public void srmRoundTrip() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<ColourUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.SRM);
		w.setQuantity(new ColourUnit(4, Quantity.Unit.SRM, false));
		ColourUnit out = w.parseOrNull();
		assertEquals(4D, out.get(Quantity.Unit.SRM), 0.01);
	}

	@Test
	public void nullClearsField()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setQuantity(new PercentageUnit(0.1));
		w.setQuantity(null);
		assertTrue(w.isBlank());
		assertNull(w.parseOrNull());
	}

	@Test
	public void setEditableTogglesField()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setEditable(false);
		assertFalse(w.getTextField().isEditable());
		w.setEditable(true);
		assertTrue(w.getTextField().isEditable());
	}

	@Test
	public void suffixConvertsToFieldUnit() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<WeightUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.GRAMS);
		w.setText("5 kg");
		WeightUnit out = w.parseOrNull();
		assertEquals(5000D, out.get(Quantity.Unit.GRAMS), 0.001);
	}

	@Test
	public void temperatureSuffixConvertsToFieldUnit() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<TemperatureUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.CELSIUS);
		w.setText("68 F");
		TemperatureUnit out = w.parseOrNull();
		assertEquals(20D, out.get(Quantity.Unit.CELSIUS), 0.5);
	}

	@Test
	public void wrongTypeSuffixThrowsOnParse()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<WeightUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.GRAMS);
		w.setText("20 L");
		try
		{
			w.parseOrNull();
			fail("expected NumberFormatException");
		}
		catch (NumberFormatException expected)
		{
			// ok
		}
	}

	@Test
	public void failedSuffixParseRevertsOnFocusLost()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantityEditWidget<WeightUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.GRAMS);
		w.setQuantity(new WeightUnit(100, Quantity.Unit.GRAMS, false));
		w.setText("20 L");
		focusLost(w);
		assertEquals("100", w.getText().replace(" ", ""));
	}

	private static void focusLost(SwingQuantityEditWidget<?> w)
	{
		FocusEvent ev = new FocusEvent(w.getTextField(), FocusEvent.FOCUS_LOST);
		for (FocusListener listener : w.getTextField().getFocusListeners())
		{
			listener.focusLost(ev);
		}
	}
}
