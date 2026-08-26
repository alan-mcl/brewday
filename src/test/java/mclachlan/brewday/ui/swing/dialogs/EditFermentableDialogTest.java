package mclachlan.brewday.ui.swing.dialogs;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EditFermentableDialogTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void keyBindingsExistForEscapeAndCtrlEnter() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		EditFermentableDialog dialog = new EditFermentableDialog(null, new Fermentable("My Fermentable"), false);

		Object escapeAction = dialog.getRootPane()
			.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			.get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
		assertEquals("dialog.cancel", escapeAction);

		JTextArea description = (JTextArea)getField(dialog, "descriptionArea");
		Object ctrlEnterAction = description.getInputMap(JComponent.WHEN_FOCUSED)
			.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
		assertEquals("dialog.commit.from.description", ctrlEnterAction);
	}

	@Test
	public void parsesBasicFields() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		EditFermentableDialog dialog = new EditFermentableDialog(null, new Fermentable("My Fermentable"), false);
		setText(dialog, "originField", "AU");
		setText(dialog, "supplierField", "MaltCo");
		setText(dialog, "yieldField", "80");
		setText(dialog, "colourField", "4");
		((JComboBox<?>)getField(dialog, "typeField")).setSelectedItem(Fermentable.Type.GRAIN);

		invokeOnOk(dialog);
		Fermentable result = dialog.getResult();
		assertNotNull(result);
		assertEquals("My Fermentable", result.getName());
		assertEquals("AU", result.getOrigin());
		assertEquals("MaltCo", result.getSupplier());
	}

	@Test
	public void yieldPercentageRoundTripWithoutEdit() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Fermentable source = new Fermentable("RoundTrip");
		source.setYield(new PercentageUnit(0.80));
		source.setMoisture(new PercentageUnit(0.04));

		EditFermentableDialog dialog = new EditFermentableDialog(null, source, false);
		invokeOnOk(dialog);
		Fermentable result = dialog.getResult();
		assertNotNull(result);
		assertEquals(0.80, result.getYield().get(), 0.0001);
		assertEquals(0.04, result.getMoisture().get(), 0.0001);
	}

	@Test
	public void colourSrmRoundTripWithoutEdit() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Fermentable source = new Fermentable("ColourRt");
		source.setColour(new ColourUnit(4, Quantity.Unit.SRM, false));

		EditFermentableDialog dialog = new EditFermentableDialog(null, source, false);
		invokeOnOk(dialog);
		Fermentable result = dialog.getResult();
		assertNotNull(result);
		assertEquals(4D, result.getColour().get(Quantity.Unit.SRM), 0.05);
	}

	private void setText(EditFermentableDialog dialog, String fieldName, String value) throws Exception
	{
		Object field = getField(dialog, fieldName);
		if (field instanceof SwingQuantityEditWidget<?> w)
		{
			w.setText(value);
		}
		else if (field instanceof JTextField jtf)
		{
			jtf.setText(value);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported field: " + fieldName);
		}
	}

	private Object getField(EditFermentableDialog dialog, String fieldName) throws Exception
	{
		Field field = EditFermentableDialog.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(dialog);
	}

	private void invokeOnOk(EditFermentableDialog dialog) throws Exception
	{
		Method onOk = EditFermentableDialog.class.getDeclaredMethod("onOk");
		onOk.setAccessible(true);
		onOk.invoke(dialog);
	}
}
