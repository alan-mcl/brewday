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
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.WaterParameters;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class EditWaterParametersDialogTest
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
		WaterParameters source = new WaterParameters("My Parameters");
		source.setMinCalcium(new PpmUnit(10));

		EditWaterParametersDialog dialog = new EditWaterParametersDialog(null, source, false);
		setText(dialog, "minCalciumField", "");
		setText(dialog, "maxCalciumField", "");
		setText(dialog, "minBicarbonateField", "");
		setText(dialog, "maxBicarbonateField", "");
		setText(dialog, "minSulfateField", "");
		setText(dialog, "maxSulfateField", "");

		invokeOnOk(dialog);
		WaterParameters result = dialog.getResult();
		assertNotNull(result);
		assertNull(result.getMinCalcium());
		assertNull(result.getMaxCalcium());
		assertNull(result.getMinBicarbonate());
		assertNull(result.getMaxBicarbonate());
		assertNull(result.getMinSulfate());
		assertNull(result.getMaxSulfate());
	}

	@Test
	public void keyBindingsExistForEscapeAndCtrlEnter() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EditWaterParametersDialog dialog = new EditWaterParametersDialog(null, new WaterParameters("My Parameters"), false);

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
		EditWaterParametersDialog dialog = new EditWaterParametersDialog(null, new WaterParameters("My Parameters"), false);

		JTextField minCalcium = (JTextField)getField(dialog, "minCalciumField");
		JTextField maxAlkalinity = (JTextField)getField(dialog, "maxAlkalinityField");
		JTextArea description = (JTextArea)getField(dialog, "descriptionArea");
		assertNotNull(minCalcium.getToolTipText());
		assertNotNull(maxAlkalinity.getToolTipText());
		assertNotNull(description.getToolTipText());
		assertEquals(true, minCalcium.getToolTipText().toLowerCase().contains("ppm"));
		assertEquals(true, maxAlkalinity.getToolTipText().toLowerCase().contains("ppm"));
		assertEquals(true, description.getToolTipText().toLowerCase().contains("text"));
	}

	@Test
	public void blankNameFocusesNameField() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		TestableEditWaterParametersDialog dialog = new TestableEditWaterParametersDialog(new WaterParameters(""), true);
		setText(dialog, "nameField", "");

		invokeOnOk(dialog);

		assertSame(getField(dialog, "nameField"), dialog.lastFocusedField);
		assertNotNull(dialog.lastErrorMessage);
	}

	@Test
	public void invalidPpmFocusesOffendingField() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		TestableEditWaterParametersDialog dialog = new TestableEditWaterParametersDialog(new WaterParameters("My Parameters"), false);
		setText(dialog, "minCalciumField", "not-a-number");

		invokeOnOk(dialog);

		assertSame(getField(dialog, "minCalciumField"), dialog.lastFocusedField);
		assertNotNull(dialog.lastErrorMessage);
		assertNull(dialog.getResult());
	}

	@Test
	public void editModeShowsExistingName() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		WaterParameters source = new WaterParameters("Named Profile");
		EditWaterParametersDialog dialog = new EditWaterParametersDialog(null, source, false);
		JTextField name = (JTextField)getField(dialog, "nameField");

		assertEquals("Named Profile", name.getText());
		assertEquals(false, name.isEditable());
	}

	@Test
	public void minAndMaxFieldsParseIndependently() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EditWaterParametersDialog dialog = new EditWaterParametersDialog(null, new WaterParameters("Range"), false);
		setText(dialog, "minCalciumField", "10");
		setText(dialog, "maxCalciumField", "25");

		invokeOnOk(dialog);
		WaterParameters result = dialog.getResult();
		assertNotNull(result);
		assertEquals(10D, result.getMinCalcium().get(), 0.0001);
		assertEquals(25D, result.getMaxCalcium().get(), 0.0001);
	}

	@Test
	public void descriptionAreaIsInScrollPane() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EditWaterParametersDialog dialog = new EditWaterParametersDialog(null, new WaterParameters("My Parameters"), false);
		JTextArea description = (JTextArea)getField(dialog, "descriptionArea");
		assertNotNull(SwingUtilities.getAncestorOfClass(JScrollPane.class, description));
	}

	private void setText(EditWaterParametersDialog dialog, String fieldName, String value) throws Exception
	{
		JTextField field = (JTextField)getField(dialog, fieldName);
		field.setText(value);
	}

	private Object getField(EditWaterParametersDialog dialog, String fieldName) throws Exception
	{
		Field field = EditWaterParametersDialog.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(dialog);
	}

	private void invokeOnOk(EditWaterParametersDialog dialog) throws Exception
	{
		Method onOk = EditWaterParametersDialog.class.getDeclaredMethod("onOk");
		onOk.setAccessible(true);
		onOk.invoke(dialog);
	}

	private static class TestableEditWaterParametersDialog extends EditWaterParametersDialog
	{
		private JTextField lastFocusedField;
		private String lastErrorMessage;

		private TestableEditWaterParametersDialog(WaterParameters waterParameters, boolean createMode)
		{
			super(null, waterParameters, createMode);
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
