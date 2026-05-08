package mclachlan.brewday.ui.swing.widgets;

import java.awt.GraphicsEnvironment;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WeightUnit;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

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
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
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
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
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
}
