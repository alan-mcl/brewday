package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwingQuantitySelectAndEditWidgetTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void unitSwitchUpdatesDisplayedValue()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantitySelectAndEditWidget w = new SwingQuantitySelectAndEditWidget(
			Quantity.Unit.KILOGRAMS, Quantity.Type.WEIGHT);
		w.setQuantity(new WeightUnit(1, Quantity.Unit.KILOGRAMS, false));
		assertTrue(w.getTextField().getText().contains("1"));
		w.getUnitCombo().setSelectedItem(Quantity.Unit.GRAMS);
		assertTrue(w.getTextField().getText().replace(" ", "").contains("1000")
			|| w.getTextField().getText().contains("1 000"));
	}

	@Test
	public void unitOptionsForWeightArePopulated()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantitySelectAndEditWidget w = new SwingQuantitySelectAndEditWidget(
			Quantity.Unit.GRAMS, Quantity.Type.WEIGHT);
		int n = w.getUnitCombo().getItemCount();
		assertTrue(n >= 4);
		boolean hasGrams = false;
		boolean hasKg = false;
		for (int i = 0; i < n; i++)
		{
			Quantity.Unit u = w.getUnitCombo().getItemAt(i);
			if (u == Quantity.Unit.GRAMS)
			{
				hasGrams = true;
			}
			if (u == Quantity.Unit.KILOGRAMS)
			{
				hasKg = true;
			}
		}
		assertTrue(hasGrams);
		assertTrue(hasKg);
	}

	@Test
	public void suffixMatchingComboUnitSwitchesCombo()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantitySelectAndEditWidget w = new SwingQuantitySelectAndEditWidget(
			Quantity.Unit.GRAMS, Quantity.Type.WEIGHT);
		w.getTextField().setText("5 kg");
		focusLost(w);
		assertEquals(Quantity.Unit.KILOGRAMS, w.getUnit());
		assertEquals("5", w.getTextField().getText().replace(" ", ""));
	}

	@Test
	public void volumeSuffixOnHopStyleComboSwitchesToMl()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantitySelectAndEditWidget w = new SwingQuantitySelectAndEditWidget(
			Quantity.Unit.GRAMS, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
		w.getTextField().setText("50 ml");
		focusLost(w);
		assertEquals(Quantity.Unit.MILLILITRES, w.getUnit());
		assertEquals("50", w.getTextField().getText().replace(" ", ""));
	}

	@Test
	public void setUnitOptionsCrossTypeRefreshDoesNotThrow()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantitySelectAndEditWidget w = new SwingQuantitySelectAndEditWidget(
			Quantity.Unit.GRAMS, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
		w.setQuantity(new WeightUnit(50, Quantity.Unit.GRAMS, false));
		w.setUnitOptions(Quantity.Unit.MILLILITRES, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
		w.setQuantity(new VolumeUnit(500, Quantity.Unit.MILLILITRES, false));
		assertEquals(Quantity.Unit.MILLILITRES, w.getUnit());
		assertTrue(w.getTextField().getText().contains("500"));
	}

	@Test
	public void manualCrossTypeUnitChangeReparsesInNewUnit()
	{
		SwingTestSupport.assumeDisplay();
		SwingQuantitySelectAndEditWidget w = new SwingQuantitySelectAndEditWidget(
			Quantity.Unit.GRAMS, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
		w.setQuantity(new WeightUnit(50, Quantity.Unit.GRAMS, false));
		w.getUnitCombo().setSelectedItem(Quantity.Unit.MILLILITRES);
		assertEquals(Quantity.Unit.MILLILITRES, w.getUnit());
		assertEquals("50", w.getTextField().getText().replace(" ", ""));
	}

	private static void focusLost(SwingQuantitySelectAndEditWidget w)
	{
		FocusEvent ev = new FocusEvent(w.getTextField(), FocusEvent.FOCUS_LOST);
		for (FocusListener listener : w.getTextField().getFocusListeners())
		{
			listener.focusLost(ev);
		}
	}
}
