package mclachlan.brewday.ui.swing.dialogs;

import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;

public class EditWaterDialogTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void blankQuantitiesRemainNull() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Water source = new Water("My Water");
		source.setCalcium(new PpmUnit(10));
		source.setPh(new PhUnit(7.1));

		EditWaterDialog dialog = new EditWaterDialog(null, source, false);
		setText(dialog, "calciumField", "");
		setText(dialog, "bicarbonateField", "");
		setText(dialog, "sulfateField", "");
		setText(dialog, "chlorideField", "");
		setText(dialog, "sodiumField", "");
		setText(dialog, "magnesiumField", "");
		setText(dialog, "phField", "");

		invokeOnOk(dialog);
		Water result = dialog.getResult();
		assertNotNull(result);
		assertNull(result.getCalcium());
		assertNull(result.getBicarbonate());
		assertNull(result.getSulfate());
		assertNull(result.getChloride());
		assertNull(result.getSodium());
		assertNull(result.getMagnesium());
		assertNull(result.getPh());
	}

	@Test
	public void keyBindingsExistForEscapeAndCtrlEnter() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EditWaterDialog dialog = new EditWaterDialog(null, new Water("My Water"), false);

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
	public void fieldTooltipsIncludeUnits() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EditWaterDialog dialog = new EditWaterDialog(null, new Water("My Water"), false);

		JTextField calcium = (JTextField)getField(dialog, "calciumField");
		JTextField ph = (JTextField)getField(dialog, "phField");
		JTextArea description = (JTextArea)getField(dialog, "descriptionArea");
		assertNotNull(calcium.getToolTipText());
		assertNotNull(ph.getToolTipText());
		assertNotNull(description.getToolTipText());
		assertEquals(true, calcium.getToolTipText().toLowerCase().contains("ppm"));
		assertEquals(true, ph.getToolTipText().toLowerCase().contains("ph"));
		assertEquals(true, description.getToolTipText().toLowerCase().contains("text"));
	}

	@Test
	public void blankNameFocusesNameField() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		TestableEditWaterDialog dialog = new TestableEditWaterDialog(new Water(""), true);
		setText(dialog, "nameField", "");

		invokeOnOk(dialog);

		assertSame(getField(dialog, "nameField"), dialog.lastFocusedField);
		assertNotNull(dialog.lastErrorMessage);
	}

	@Test
	public void invalidPpmFocusesOffendingField() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		TestableEditWaterDialog dialog = new TestableEditWaterDialog(new Water("My Water"), false);
		setText(dialog, "calciumField", "not-a-number");

		invokeOnOk(dialog);

		assertSame(getField(dialog, "calciumField"), dialog.lastFocusedField);
		assertNotNull(dialog.lastErrorMessage);
		assertNull(dialog.getResult());
	}

	@Test
	public void descriptionAreaIsInScrollPane() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EditWaterDialog dialog = new EditWaterDialog(null, new Water("My Water"), false);
		JTextArea description = (JTextArea)getField(dialog, "descriptionArea");
		assertNotNull(SwingUtilities.getAncestorOfClass(JScrollPane.class, description));
	}

	private void setText(EditWaterDialog dialog, String fieldName, String value) throws Exception
	{
		JTextField field = (JTextField)getField(dialog, fieldName);
		field.setText(value);
	}

	private Object getField(EditWaterDialog dialog, String fieldName) throws Exception
	{
		Field field = EditWaterDialog.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(dialog);
	}

	private void invokeOnOk(EditWaterDialog dialog) throws Exception
	{
		Method onOk = EditWaterDialog.class.getDeclaredMethod("onOk");
		onOk.setAccessible(true);
		onOk.invoke(dialog);
	}

	private static class TestableEditWaterDialog extends EditWaterDialog
	{
		private JTextField lastFocusedField;
		private String lastErrorMessage;

		private TestableEditWaterDialog(Water water, boolean createMode)
		{
			super(null, water, createMode);
		}

		@Override
		protected void focusForValidation(JTextField field)
		{
			lastFocusedField = field;
			super.focusForValidation(field);
		}

		@Override
		protected void showValidationError(String message)
		{
			lastErrorMessage = message;
		}
	}
}
