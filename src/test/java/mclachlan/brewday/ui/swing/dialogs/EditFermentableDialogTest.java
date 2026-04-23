package mclachlan.brewday.ui.swing.dialogs;

import java.awt.GraphicsEnvironment;
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
import org.junit.Assume;
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
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
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
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
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

	private void setText(EditFermentableDialog dialog, String fieldName, String value) throws Exception
	{
		JTextField field = (JTextField)getField(dialog, fieldName);
		field.setText(value);
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
