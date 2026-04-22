package mclachlan.brewday.ui.swing.app;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class ActionHotkeySupport
{
	private ActionHotkeySupport()
	{
	}

	public static void setMnemonic(Action action, int keyCode)
	{
		action.putValue(Action.MNEMONIC_KEY, keyCode);
	}

	public static void setTooltip(Action action, String text)
	{
		action.putValue(Action.SHORT_DESCRIPTION, text);
	}

	public static void bind(JComponent component, KeyStroke keyStroke, String actionId, Action action)
	{
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, actionId);
		component.getActionMap().put(actionId, action);
	}

	public static void bindFocused(JComponent component, KeyStroke keyStroke, String actionId, Action action)
	{
		component.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, actionId);
		component.getActionMap().put(actionId, action);
	}

	public static int menuShortcutMask()
	{
		if (GraphicsEnvironment.isHeadless())
		{
			return InputEvent.CTRL_DOWN_MASK;
		}
		return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	}

	public static KeyStroke ctrlOrCmd(int keyCode)
	{
		return KeyStroke.getKeyStroke(keyCode, menuShortcutMask());
	}

	public static KeyStroke ctrlOrCmdShift(int keyCode)
	{
		return KeyStroke.getKeyStroke(keyCode, menuShortcutMask() | InputEvent.SHIFT_DOWN_MASK);
	}
}
