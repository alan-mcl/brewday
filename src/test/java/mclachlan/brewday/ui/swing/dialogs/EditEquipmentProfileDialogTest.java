package mclachlan.brewday.ui.swing.dialogs;

import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.PowerUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EditEquipmentProfileDialogTest
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
		EquipmentProfile p = new EquipmentProfile("Eq");
		EditEquipmentProfileDialog dialog = new EditEquipmentProfileDialog(null, p, false);

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
	public void parsesNameField() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EquipmentProfile p = new EquipmentProfile("KeepName");
		EditEquipmentProfileDialog dialog = new EditEquipmentProfileDialog(null, p, false);
		((JTextField)getField(dialog, "nameField")).setText("KeepName");

		invokeOnOk(dialog);
		EquipmentProfile result = dialog.getResult();
		assertNotNull(result);
		assertEquals("KeepName", result.getName());
	}

	@Test
	public void numericRoundTripWithoutEdit() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		EquipmentProfile source = new EquipmentProfile("Rt");
		source.setMashTunVolume((VolumeUnit)Quantity.parseQuantity("50", Quantity.Unit.LITRES));
		source.setBoilElementPower((PowerUnit)Quantity.parseQuantity("3.5", Quantity.Unit.KILOWATT));
		source.setMashTunSpecificHeat(new ArbitraryPhysicalQuantity(300, Quantity.Unit.JOULE_PER_KG_CELSIUS));

		EditEquipmentProfileDialog dialog = new EditEquipmentProfileDialog(null, source, false);
		invokeOnOk(dialog);
		EquipmentProfile result = dialog.getResult();
		assertNotNull(result);
		assertEquals(50D, result.getMashTunVolume().get(Quantity.Unit.LITRES), 0.01);
		assertEquals(3.5D, result.getBoilElementPower().get(Quantity.Unit.KILOWATT), 0.01);
		assertEquals(300D, result.getMashTunSpecificHeat().get(), 0.01);
	}

	private Object getField(EditEquipmentProfileDialog dialog, String fieldName) throws Exception
	{
		Field field = EditEquipmentProfileDialog.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(dialog);
	}

	private void invokeOnOk(EditEquipmentProfileDialog dialog) throws Exception
	{
		Method onOk = EditEquipmentProfileDialog.class.getDeclaredMethod("onOk");
		onOk.setAccessible(true);
		onOk.invoke(dialog);
	}
}
